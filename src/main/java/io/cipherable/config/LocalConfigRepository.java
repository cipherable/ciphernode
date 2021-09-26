package io.cipherable.config;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.cipherable.server.models.EncryptionRealm;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LocalConfigRepository implements ConfigRepository {

  @Override
  public Future<EncryptionRealm> fetch(UUID uuid) {
    Promise<EncryptionRealm> p = Promise.promise();
    try {
      JsonObject o = new JsonObject(Resources.toString(Resources.getResource("realms/" + uuid + ".json"), Charsets.UTF_8));
      p.complete(o.mapTo(EncryptionRealm.class));
      log.info(o);
    } catch (Exception ex) {
      log.error(ex);
      p.fail(ex);
    }

    return p.future();
  }
}
