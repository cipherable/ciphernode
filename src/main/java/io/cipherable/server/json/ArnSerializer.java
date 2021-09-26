package io.cipherable.server.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import software.amazon.awssdk.arns.Arn;

public class ArnSerializer extends JsonSerializer<Arn> {

  @Override
  public void serialize(Arn arn, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    jsonGenerator.writeString(arn.toString());
  }
}
