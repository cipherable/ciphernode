package io.cipherable.server.models;

import java.util.Optional;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.regions.Region;

@Log4j2
@Value
public class RegionalEncryptionRealm {

  Arn role;
  Arn key;
  Region region;
  EncryptionRealm realm;

  public RegionalEncryptionRealm(EncryptionRealm realm, Region preferredRegion) {

    key = realm.getMasterKeys().stream()
        .filter(arn -> arn.region().equals(Optional.of(preferredRegion.toString())))
        .findFirst().orElse(realm.getMasterKeys().get(0));


    role = Arn.builder()
        .partition(realm.getAwsPartition())
        .service("iam")
        .region("")
        .accountId(realm.getAwsAccountId())
        .resource("role/" + realm.getAwsRole())
        .build();

    this.realm = realm;
    this.region = Region.of(key.region().get());
    log.info("region is {}", this.region);
  }

}
