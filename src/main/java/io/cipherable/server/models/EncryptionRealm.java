package io.cipherable.server.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.cipherable.server.json.ArnDeserializer;
import io.cipherable.server.json.ArnSerializer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import lombok.Data;
import software.amazon.awssdk.arns.Arn;


@Data
public class EncryptionRealm {
  UUID id;
  String awsAccountId;
  String awsPartition;
  String awsRole;
  String friendlyName;
  String tenantAwsAccountId;
  String tenantUniqueId;
  String tenantAwsRole;
  Long maxDataKeyAgeSeconds;
  Boolean isActive;
  Instant createdAt;

  @JsonSerialize(contentUsing = ArnSerializer.class)
  @JsonDeserialize(contentUsing = ArnDeserializer.class)
  ArrayList<Arn> masterKeys = Lists.newArrayList();

  @JsonIgnore
  public boolean isTenantRealm() {
    return !Strings.isNullOrEmpty(tenantAwsAccountId);
  }
}
