package io.cipherable.server;

import com.google.common.collect.Lists;
import io.cipherable.config.ConfigRepository;
import io.cipherable.server.aws.KmsCryptographicOperations;
import io.cipherable.server.aws.KmsOperationException;
import io.cipherable.server.crypto.CryptographyOperationException;
import io.cipherable.server.crypto.SodiumCryptographyService;
import io.cipherable.server.models.CipherText;
import io.cipherable.server.models.DataKey;
import io.cipherable.server.models.EncryptionRealm;
import io.cipherable.server.models.KeyedCipherText;
import io.cipherable.server.models.MultiRegionCipherText;
import io.cipherable.server.models.RegionalEncryptionRealm;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.regions.Region;

@Log4j2
public class HttpServer extends AbstractVerticle {
  private final ConfigRepository configRepository;
  private final KmsCryptographicOperations kms;
  private final SodiumCryptographyService sodium;
  private final int port;
  private final String host;
  private final Region region;

  public HttpServer(
      String host,
      int port,
      ConfigRepository configRepository,
      KmsCryptographicOperations kms,
      SodiumCryptographyService sodium,
      Region region) {
    this.host = host;
    this.port = port;
    this.configRepository = configRepository;
    this.kms = kms;
    this.sodium = sodium;
    this.region = region;
  }

  private void sendErrorResponse(RoutingContext ctx, int statusCode, String msg) {
    ctx.response()
        .setStatusCode(statusCode)
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .end(new JsonObject().put("error", msg).encodePrettily());
  }

  private void sendResponse(RoutingContext ctx, int statusCode) {
    ctx.response().setStatusCode(statusCode).end();
  }

  private void sendTextResponse(RoutingContext ctx, int statusCode, String text) {
    ctx.response()
        .setStatusCode(statusCode)
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/text")
        .end(text);
  }

  private void sendJsonResponse(RoutingContext ctx, int statusCode, JsonObject o) {
    ctx.response()
        .setStatusCode(statusCode)
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .end(o.encodePrettily());
  }

  public Handler<RoutingContext> requireJson() {
    return ctx -> {
      if (ctx.request().headers().contains(HttpHeaders.CONTENT_TYPE, "application/json", false)) {
        ctx.next();
      } else {
        sendErrorResponse(ctx, 415, "Unsupported Media Type. JSON required");
      }
    };
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router v1 = Router.router(vertx);

    v1.route(HttpMethod.POST, "/realm/:uuid/encrypt")
        .handler(BodyHandler.create())
        .handler(
            ctx -> {
              UUID uuid = UUID.fromString(ctx.pathParam("uuid"));
              configRepository
                  .fetch(uuid)
                  .compose(
                      realm -> encrypt(ctx, realm))
                  .onSuccess(m -> sendTextResponse(ctx, 200, m.getPackedString()))
                  .onFailure(t -> this.handleFailure(t, ctx));
            });

    v1.route(HttpMethod.POST, "/realm/:uuid/decrypt")
        .handler(BodyHandler.create())
        .handler(
            ctx -> {
              UUID uuid = UUID.fromString(ctx.pathParam("uuid"));
              configRepository
                  .fetch(uuid)
                  .compose(realm -> decrypt(ctx, realm))
                  .onSuccess(str -> sendTextResponse(ctx, 200, str))
                  .onFailure(t -> this.handleFailure(t, ctx));
            });

    Router root = Router.router(vertx);
    root.mountSubRouter("/v1/", v1);

    root.route(HttpMethod.GET, "/ping")
        .handler(
            ctx -> {
              ctx.response().end("pong");
            });
    root.route("/*")
        .handler(
            ctx -> {
              ctx.response().setStatusCode(404).end("Not Found");
            });

    vertx
        .createHttpServer()
        .requestHandler(root)
        .listen(port, host)
        .onSuccess(
            x -> {
              log.info("HTTP server started on {}:{}", host, port);
              startPromise.complete();
            })
        .onFailure(startPromise::fail);
  }

  public AtomicInteger counter = new AtomicInteger(0);
  public Future<MultiRegionCipherText> encrypt(RoutingContext ctx, EncryptionRealm realm) {
    if (realm.getMasterKeys().isEmpty()) {
      throw new RuntimeException("encryption requires master keys");
    } else {
      return context.<MultiRegionCipherText>executeBlocking(
          blocking -> {
            try {
              ArrayList<KeyedCipherText> keyedCipherTexts =
                  Lists.newArrayList();
              byte[] message =
                  ctx.getBodyAsString().getBytes(StandardCharsets.UTF_8);

              for (Arn key : realm.getMasterKeys()) {
                DataKey dk =
                    kms.getDataKey(
                        new RegionalEncryptionRealm(realm, region));
                CipherText cipherText =
                    sodium.encrypt(message, dk.getPlainBytes(), counter.getAndIncrement());
                keyedCipherTexts.add(
                    new KeyedCipherText(
                        key,
                        dk.getEncryptedBytes(),
                        cipherText.getNonce(),
                        cipherText.getEncryptedBytes()));
              }
              blocking.complete(new MultiRegionCipherText(keyedCipherTexts));
            } catch (KmsOperationException
                | CryptographyOperationException ex) {
              blocking.fail(ex);
            }
          });
    }
  }
  public Future<String> decrypt(RoutingContext ctx, EncryptionRealm realm) {
    if (realm.getMasterKeys().isEmpty()) {
      throw new RuntimeException("decryption requires master keys");
    } else {
      return context.<String>executeBlocking(
          blocking -> {
            try {
              Optional<MultiRegionCipherText> mrOpt =
                  MultiRegionCipherText.unpack(ctx.getBodyAsString());
              if (mrOpt.isEmpty()) {
                throw new RuntimeException("Invalid ciphertext");
              }
              MultiRegionCipherText mr = mrOpt.get();
              KeyedCipherText cipherText = mr.resolve(region);
              byte[] encryptedDataKey = cipherText.getEncryptedDatakey();
              byte[] plainTextDataKey =
                  kms.decrypt(
                      new RegionalEncryptionRealm(realm, region),
                      encryptedDataKey);

              byte[] plainText =
                  sodium.decrypt(
                      cipherText.getCipheredText(),
                      plainTextDataKey,
                      cipherText.getNonce());

              blocking.complete(new String(plainText));
            } catch (Exception ex) {
              blocking.fail(ex);
            }
          });
    }
  }
  public void handleFailure(Throwable t, RoutingContext ctx) {
    if (t.getStackTrace().length > 0) {
      log.error("exception", t);
    } else {
      log.error(t.getMessage());
    }
    String msg;
    if (t instanceof CipherableException) {
      msg = t.getMessage();
    } else {
      msg = "internal error";
    }
    sendErrorResponse(ctx, 500, msg);
  }
}
