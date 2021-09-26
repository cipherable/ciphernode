package io.cipherable.server.models;

import lombok.Value;

@Value
public class DataKey {
  byte[] plainBytes;
  byte[] encryptedBytes;
}
