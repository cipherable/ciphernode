package io.cipherable.server.models;

import lombok.Value;

@Value
public class CipherText {
  byte[] nonce;
  byte[] encryptedBytes;
}
