package io.cipherable.server;

import io.cipherable.server.models.KeyedCipherText;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.regions.Region;

public class Utils {
  private final static Random random = new Random();

  public static byte[] randomBytes(int n) {
    byte[] arr = new byte[n];
    random.nextBytes(arr);
    return arr;
  }

  public static String randomAwsAccount() {
    return java.lang.String.valueOf(Math.round(random.nextFloat() * Math.pow(10,12)));
  }

  public static String bytesToString(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static String randomKeyId() {
    return UUID.randomUUID().toString();
  }

  public static byte[] randomNonce() {
    return randomBytes(12);
  }

  public static byte[] randomDataKey() {
    return randomBytes(256);
  }

  public static byte[] randomCipheredText() {
    return randomBytes(100 + random.nextInt(1000));
  }

  public static byte[] randomCipheredText(int size) {
    return randomBytes(size);
  }

  public static byte[] stringToBytes(String str) {
    return Base64.getDecoder().decode(str);
  }
  public static String readFileFromResources(String filename) {
    try {
    URL resource = Utils.class.getClassLoader().getResource(filename);
    byte[] bytes = Files.readAllBytes(Paths.get(resource.toURI()));
    return new String(bytes);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static KeyedCipherText randomCipherText(Region region, int size) {
    return new KeyedCipherText(
        Arn.builder().region(region.toString()).partition("aws").accountId(randomAwsAccount()).service("blah").resource("res").build(),
        randomBytes(16),
        randomBytes(12),
        randomBytes(32)
    );
  }



}
