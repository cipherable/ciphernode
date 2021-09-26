package io.cipherable.server.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import software.amazon.awssdk.arns.Arn;

public class ArnDeserializer extends JsonDeserializer<Arn> {

  @Override
  public Arn deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    return Arn.fromString(jsonParser.getValueAsString());
  }
}
