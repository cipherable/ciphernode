package io.cipherable.server;

import io.cipherable.config.ConfigRepository;
import io.cipherable.config.HttpConfigRepository;
import io.cipherable.server.aws.AwsKmsFactory;
import io.cipherable.server.aws.AwsSts;
import io.cipherable.server.aws.KmsCryptographicOperations;
import io.cipherable.server.crypto.SodiumCryptographyService;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import java.net.URL;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;


@Log4j2
public class Main {
  public static final String DEFAULT_HOST = "0.0.0.0";
  public static final int DEFAULT_PORT = 8190;

  public static void main(String[] args) throws Exception {
    String host = System.getenv("HOST");
    if (host == null) {
      host = DEFAULT_HOST;
    }
    final String finalHost = host;

    String portString = System.getenv("PORT");
    int port = DEFAULT_PORT;
    if (portString != null) {
      port = Integer.parseInt(portString);
    }
    final int finalPort = port;

    final Vertx vertx = Vertx.vertx();

    AwsCredentialsProvider awsCredentialsProvider = AwsCredentialsProviderChain.of(
        ContainerCredentialsProvider.builder().asyncCredentialUpdateEnabled(true).build(),
        EnvironmentVariableCredentialsProvider.create()
    );
    final AwsSts awsSts = new AwsSts(awsCredentialsProvider);
    final AwsKmsFactory awsKmsFactory = new AwsKmsFactory(awsSts);
    final SodiumCryptographyService sodium = new SodiumCryptographyService();
    final KmsCryptographicOperations kms = new KmsCryptographicOperations(awsKmsFactory);
    //final ConfigRepository configRepository = new LocalConfigRepository();
    String repoUrl = System.getenv("REALMS_REPOSITORY_URL");
    log.info("Realms repository URL is {}", repoUrl);
    final ConfigRepository configRepository = new HttpConfigRepository(vertx, new URL(repoUrl));
    AwsRegionProvider awsRegionProvider = new AwsRegionProviderChain(
        new InstanceProfileRegionProvider(),
        () -> Optional.ofNullable(System.getenv("AWS_REGION")).map(Region::of).orElse(null)
        );
    Region region = awsRegionProvider.getRegion();
    if (region == null) {
      throw new CipherableException("An AWS region is required but none was found.");
    }
    log.info("current region is {}", region);
    vertx.deployVerticle(
        () -> new HttpServer(finalHost, finalPort, configRepository, kms, sodium, region), new DeploymentOptions());
  }
}
