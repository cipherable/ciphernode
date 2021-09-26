package io.cipherable.server.aws;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.cipherable.server.models.RegionalEncryptionRealm;
import java.time.Duration;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.kms.KmsClient;

@Log4j2
public class AwsKmsFactory {

  private final AwsSts awsSts;
  private final LoadingCache<RegionalEncryptionRealm, AwsKms> clients;

  public AwsKmsFactory(AwsSts awsSts) {
    this.awsSts = awsSts;
    this.clients =
        Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofHours(1))
            .expireAfterAccess(Duration.ofMinutes(1))
            // .refreshAfterWrite(Duration.ofSeconds(1))
            .removalListener(
                (RegionalEncryptionRealm realm, AwsKms kms, RemovalCause cause) -> {
                  if (kms != null) {
                    log.debug("closing kms client for realm {} in region {}", realm.getRealm().getId(), realm.getRegion());
                    kms.close();
                  }
                })
            .build(this::createClient);
  }

  public AwsKms getClient(RegionalEncryptionRealm realm) {
    return this.clients.get(realm);
  }

  private AwsKms createClient(RegionalEncryptionRealm realm) {
    log.debug("creating kms client for realm {} in region {}", realm.getRealm().getId(), realm.getRegion());
    return new AwsKms(
        KmsClient.builder()
            .overrideConfiguration(
                b ->
                    b.apiCallTimeout(Duration.ofMillis(1000))
                        .apiCallAttemptTimeout(Duration.ofMillis(1000)))
            .credentialsProvider(awsSts.getCredentials(realm))
            .region(realm.getRegion())
            .build(),
        realm);
  }
}
