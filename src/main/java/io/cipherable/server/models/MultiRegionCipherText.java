package io.cipherable.server.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.StringJoiner;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.regions.Region;

@Value
@Log4j2
public class MultiRegionCipherText {
  ArrayList<KeyedCipherText> keyedCipherTexts;

  public static Optional<MultiRegionCipherText> unpack(final String payload) {
    if (Strings.isNullOrEmpty(payload)) {
      return Optional.empty();
    }

    String base64Payload = payload;
    if (payload.contains("_")) {
      // strip region prefix if present
      String[] tokens = payload.split("_");
      if (tokens.length != 2) {
        log.error("payload contains region prefix marker but its length is not 2");
        return Optional.empty();
      }
      base64Payload = tokens[1];
    }
    return tryUnpack(Base64.getDecoder().decode(base64Payload.trim()));
  }

  private static Optional<MultiRegionCipherText> tryUnpack(byte[] bytes) {
    try (MessageUnpacker packer = MessagePack.newDefaultUnpacker(bytes)) {

      // version
      int version = packer.unpackInt();
      if (version != 1) {
        log.error("not version 1, got {}", version);
        return Optional.empty();
      }

      ArrayList<KeyedCipherText> keyedCipherTexts = Lists.newArrayList();
      int n = packer.unpackArrayHeader();
      for (int i = 0; i < n; i++) {
        String arn = packer.unpackString();

        int dataKeyLength = packer.unpackBinaryHeader();
        byte[] dataKey = new byte[dataKeyLength];
        packer.readPayload(dataKey);

        int nonceLength = packer.unpackBinaryHeader();
        byte[] nonce = new byte[nonceLength];
        packer.readPayload(nonce);

        int cipheredTextLength = packer.unpackBinaryHeader();
        byte[] cipheredText = new byte[cipheredTextLength];
        packer.readPayload(cipheredText);
        KeyedCipherText keyedCipherText =
            new KeyedCipherText(Arn.fromString(arn), dataKey, nonce, cipheredText);
        keyedCipherTexts.add(keyedCipherText);
      }
      return Optional.of(new MultiRegionCipherText(keyedCipherTexts));
    } catch (IOException ex) {
      log.error(ex);
      return Optional.empty();
    }
  }

  public KeyedCipherText resolve(Region region) {
   KeyedCipherText keyedCipherText = keyedCipherTexts.stream()
        .filter(
            cipherText -> {
              Optional<String> keyRegion = cipherText.getKey().region();
              return keyRegion.isPresent() && keyRegion.get().equals(region.toString());
            })
        .findFirst().orElse(keyedCipherTexts.get(0));
    log.debug("current region: {}, resolved ciphertext region: {}", region, keyedCipherText.getKey().region());
    return keyedCipherText;
  }

  @JsonIgnore
  public String getPackedString() {
    return Base64.getEncoder().encodeToString(pack());
  }

  private String withRegionPrefix(String ciphertextBase64) {
    StringJoiner sb = new StringJoiner(":");
    keyedCipherTexts.forEach(c -> sb.add(c.getKey().region().get().toString()));
    return sb + "_" + ciphertextBase64;
  }

  public byte[] pack() {
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {

      // version
      packer.packInt(1);
      packer.packArrayHeader(keyedCipherTexts.size());

      for (KeyedCipherText c : keyedCipherTexts) {
        // key arn
        packer.packString(c.getKey().toString());

        packer.packBinaryHeader(c.getEncryptedDatakey().length);
        packer.writePayload(c.getEncryptedDatakey());

        packer.packBinaryHeader(c.getNonce().length);
        packer.writePayload(c.getNonce());

        packer.packBinaryHeader(c.getCipheredText().length);
        packer.writePayload(c.getCipheredText());
      }

      //log.info("wrote {} bytes", packer.getTotalWrittenBytes());
      return packer.toByteArray();
    } catch (IOException ex) {
      log.error("error", ex);
      return new byte[0];
    }
  }
}
