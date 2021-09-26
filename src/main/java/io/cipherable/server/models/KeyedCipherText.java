package io.cipherable.server.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Value;
import software.amazon.awssdk.arns.Arn;

@Value
public class KeyedCipherText {
  @JsonIgnore Arn key;
  byte[] encryptedDatakey;
  byte[] nonce;
  byte[] cipheredText;
}
