package io.cipherable.server.aws;

import io.cipherable.server.models.RegionalEncryptionRealm;
import java.time.Duration;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

@Log4j2
public class AwsSts {
  private final AwsCredentialsProvider awsCredentialsProvider;
  public AwsSts(AwsCredentialsProvider awsCredentialsProvider) {
    this.awsCredentialsProvider = awsCredentialsProvider;
  }

  public StsAssumeRoleCredentialsProvider getCredentials(
      RegionalEncryptionRealm regionalEncryptionRealm) {
    if (regionalEncryptionRealm.getRealm().isTenantRealm()) {
      return assumeTenantRole(regionalEncryptionRealm);
    } else {
      return assumeRole(regionalEncryptionRealm);
    }
  }

  public StsAssumeRoleCredentialsProvider assumeRole(
      RegionalEncryptionRealm regionalEncryptionRealm) {
    AssumeRoleRequest assumeRoleRequest =
        AssumeRoleRequest.builder()
            .roleArn(regionalEncryptionRealm.getRole().toString())
            .roleSessionName("cipherable-user-session")
            .durationSeconds(900)
            .externalId(
                regionalEncryptionRealm.getRealm().getId().toString()) // should be customer UUID
            .build();
    log.debug("creating sts client to assume role in region {} for realm {}", regionalEncryptionRealm.getRegion(), regionalEncryptionRealm.getRealm().getId());

    return make(assumeRoleRequest, regionalEncryptionRealm.getRegion());
  }

  public StsAssumeRoleCredentialsProvider assumeTenantRole(
      RegionalEncryptionRealm regionalEncryptionRealm) {
    if (!regionalEncryptionRealm.getRealm().isTenantRealm()) {
      throw new IllegalArgumentException(
          "Cannot assume tenant role for realm " + regionalEncryptionRealm.getRealm().getId());
    }

    // assume customer role
    AssumeRoleRequest assumeRoleRequest =
        AssumeRoleRequest.builder()
            .roleArn(regionalEncryptionRealm.getRealm().getAwsRole().toString())
            .roleSessionName("cipherable-user-session")
            .durationSeconds(900)
            .externalId(
                regionalEncryptionRealm.getRealm().getId().toString()) // should be customer UUID
            .build();

    StsAssumeRoleCredentialsProvider provider =
        make(assumeRoleRequest, regionalEncryptionRealm.getRegion());

    // then assume tenant role
    AssumeRoleRequest assumeTenantRoleRequest =
        AssumeRoleRequest.builder()
            .roleArn(regionalEncryptionRealm.getRealm().getTenantAwsRole())
            .roleSessionName("cipherable-tenant-session")
            .durationSeconds(900)
            .externalId(
                regionalEncryptionRealm.getRealm().getTenantUniqueId()) // should be customer UUID
            .build();

    return make(assumeTenantRoleRequest, regionalEncryptionRealm.getRegion());
  }

  private StsAssumeRoleCredentialsProvider make(AssumeRoleRequest req, Region region) {
    StsClient stsClient = StsClient.builder().credentialsProvider(awsCredentialsProvider).region(region).build();

    return StsAssumeRoleCredentialsProvider.builder()
        .stsClient(stsClient)
        .asyncCredentialUpdateEnabled(true)
        .prefetchTime(Duration.ofMinutes(1))
        .refreshRequest(req)
        .build();
  }
}
