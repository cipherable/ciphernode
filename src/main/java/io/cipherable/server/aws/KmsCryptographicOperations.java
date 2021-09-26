package io.cipherable.server.aws;

import io.cipherable.server.models.DataKey;
import io.cipherable.server.models.RegionalEncryptionRealm;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class KmsCryptographicOperations {

  private final AwsKmsFactory awsKmsFactory;

  public KmsCryptographicOperations(AwsKmsFactory awsKmsFactory) {
    this.awsKmsFactory = awsKmsFactory;
  }


  public DataKey getDataKey(RegionalEncryptionRealm regionalEncryptionRealm)
      throws KmsOperationException {
    return awsKmsFactory.getClient(regionalEncryptionRealm).generateDataKey();
  }

  public byte[] decrypt(RegionalEncryptionRealm regionalEncryptionRealm, byte[] cipherTextBlob)
      throws Exception {
    return awsKmsFactory
        .getClient(regionalEncryptionRealm)
        .decrypt(regionalEncryptionRealm.getKey(), cipherTextBlob);
  }
}
