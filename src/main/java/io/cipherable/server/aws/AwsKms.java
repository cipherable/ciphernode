package io.cipherable.server.aws;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.cipherable.server.models.DataKey;
import io.cipherable.server.models.RegionalEncryptionRealm;
import java.time.Duration;
import java.util.Base64;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

@Log4j2
public class AwsKms {
  private final KmsClient kmsClient;
  private final RegionalEncryptionRealm realm;
  private final LoadingCache<Arn, DataKey> dataKeyCache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(60)).build(this::generateDataKey);

  private final Cache<String, byte[]> decryptedDataKeyCache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(60)).build();

  public AwsKms(KmsClient kmsClient, RegionalEncryptionRealm realm) {
    this.kmsClient = kmsClient;
    this.realm = realm;
  }

  public void close() {
    log.info("closing kms for realm {}", realm.getRealm().getId());
    dataKeyCache.invalidateAll();
    decryptedDataKeyCache.invalidateAll();
    kmsClient.close();
  }

  private DataKey generateDataKey(Arn masterKey) throws KmsOperationException {
    GenerateDataKeyRequest generateDataKeyRequest =
        GenerateDataKeyRequest.builder()
            .keyId(masterKey.toString())
            .keySpec(DataKeySpec.AES_256)
            .build();
    try {
      GenerateDataKeyResponse resp = kmsClient.generateDataKey(generateDataKeyRequest);
      log.debug("generated a data key for {}", masterKey);
      return new DataKey(resp.plaintext().asByteArray(), resp.ciphertextBlob().asByteArray());
    } catch (Exception ex) {
      throw new KmsOperationException((ex));
    }
  }

  public DataKey generateDataKey() {
    return this.dataKeyCache.get(realm.getKey());
  }

  public byte[] decrypt(Arn keyArn, byte[] encryptedDataKey) throws Exception {
    String encryptedDataKeyStr = Base64.getEncoder().encodeToString(encryptedDataKey);
    byte[] decryptedDataKey = decryptedDataKeyCache.getIfPresent(encryptedDataKeyStr);
    if (decryptedDataKey != null) {
      log.debug("data key cache hit (encrypted {}) for master key {}", encryptedDataKeyStr, keyArn);
      return decryptedDataKey;
    }
    DecryptRequest decryptRequest =
        DecryptRequest.builder()
            .keyId(keyArn.toString())
            .ciphertextBlob(SdkBytes.fromByteArray(encryptedDataKey))
            .build();
    try {
      DecryptResponse resp = kmsClient.decrypt(decryptRequest);
      decryptedDataKey = resp.plaintext().asByteArray();
      decryptedDataKeyCache.put(encryptedDataKeyStr, decryptedDataKey);
      log.debug("caching data key (encrypted: {}) for master key {}",encryptedDataKeyStr, keyArn);
      return decryptedDataKey;
    } catch (Exception ex) {
      if (ex.getMessage().contains("not allowed to access")) {
        throw new KmsOperationException("You are not allowed to access " + keyArn);
      }
      throw new Exception(ex);
    }
  }
}
