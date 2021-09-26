package io.cipherable.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.cipherable.server.models.EncryptionRealm;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HttpConfigRepository implements ConfigRepository {

  final WebClient webClient;
  final URL remoteSource;
  final Cache<UUID, EncryptionRealm> realms;

  public HttpConfigRepository(Vertx vertx, URL remoteSource) {
    this.webClient = WebClient.create(vertx);
    this.remoteSource = remoteSource;
    this.realms = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).build();
  }

  @Override
  public Future<EncryptionRealm> fetch(UUID uuid) {
    String path = "/" + uuid + ".json";
    EncryptionRealm cachedRealm = realms.getIfPresent(uuid);
    if (cachedRealm != null) {
      return Future.succeededFuture(cachedRealm);
    }
    log.info("fetching {}{}", remoteSource.toExternalForm(), path);
    return webClient
        .get(
            remoteSource.getPort() == -1 ? remoteSource.getDefaultPort() : remoteSource.getPort(),
            remoteSource.getHost(),
            path)
        .timeout(2500)
        .ssl(remoteSource.getProtocol().equals("https"))
        .send()
        .map(
            rep -> {
              JsonObject o = new JsonObject(rep.bodyAsString());
              EncryptionRealm realm = o.mapTo(EncryptionRealm.class);
              realms.put(uuid, realm);
              log.debug("realm {} is now cached", uuid);
              return realm;
            });
  }
}
