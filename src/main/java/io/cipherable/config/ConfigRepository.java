package io.cipherable.config;

import io.cipherable.server.models.EncryptionRealm;
import io.vertx.core.Future;
import java.util.UUID;

public interface ConfigRepository {
  Future<EncryptionRealm> fetch(UUID uuid);
}
