package io.cipherable.server.models;

import static io.cipherable.server.Utils.randomCipherText;

import com.google.common.primitives.Ints;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

public class KeyedCipherTextTest {
  @Test
  public void testCipherText() {
    KeyedCipherText keyedCipherText = randomCipherText(Region.US_WEST_2, 100);

    Assertions.assertEquals(keyedCipherText, keyedCipherText);
    System.out.println(keyedCipherText);
  }

  @Test
  public void testCounterLength() {
    byte[] nonce = new byte[12];
    Assertions.assertEquals(nonce.length, 12);
    int counterVal = Integer.MAX_VALUE;
    byte[] counterByteArray = Ints.toByteArray(counterVal);
    byte[] resized = Arrays.copyOf(counterByteArray, 12);
    Assertions.assertEquals(12, resized.length);
    Assertions.assertEquals(counterVal, Ints.fromByteArray(resized));
    AtomicInteger x = new AtomicInteger(Integer.MAX_VALUE);
    x.get();
  }

}
