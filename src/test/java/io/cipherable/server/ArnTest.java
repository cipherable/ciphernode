package io.cipherable.server;

import static io.cipherable.server.Utils.randomAwsAccount;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.arns.Arn;

public class ArnTest {
  @Test
  public void testArns() {
    Arn roleArn = Arn.builder()
        .partition("aws")
        .accountId(randomAwsAccount())
        .service("iam")
        .resource("role/123")
        .region("")
        .build();
    System.out.println(roleArn);
  }
}
