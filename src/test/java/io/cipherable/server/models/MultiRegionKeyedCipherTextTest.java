package io.cipherable.server.models;

import static io.cipherable.server.Utils.randomCipherText;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.regions.Region;

public class MultiRegionKeyedCipherTextTest {

  @ParameterizedTest
  @ValueSource(ints = {10, 50, 100, 1000}) // six numbers
  public void test(int inputSize) {
    ArrayList<KeyedCipherText> keyedCipherTexts =
        Lists.newArrayList(
            randomCipherText(Region.US_WEST_2, inputSize),
            randomCipherText(Region.US_EAST_2, inputSize),
            randomCipherText(Region.EU_WEST_1, inputSize)
        );
    MultiRegionCipherText m = new MultiRegionCipherText(keyedCipherTexts);

    String packedString = m.getPackedString();
    double packedStringLength = packedString.length();
    System.out.println("plain text payload size is " + inputSize);
    double overhead = packedStringLength / inputSize;
    System.out.println("packed string is " +packedStringLength + ". overhead " + overhead);


    MultiRegionCipherText unpacked = MultiRegionCipherText.unpack(packedString).get();
    Assertions.assertEquals(m, unpacked);
  }
}
