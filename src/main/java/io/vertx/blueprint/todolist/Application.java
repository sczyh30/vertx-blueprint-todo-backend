package io.vertx.blueprint.todolist;

import io.vertx.blueprint.todolist.verticles.SingleApplicationVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Vert.x Blueprint Application - Todo Backend
 * Launcher Class
 *
 * @author Eric Zhao
 */
public class Application {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    Verticle todoVerticle = new SingleApplicationVerticle();

    final int port = Integer.getInteger("http.port", 8082);
    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", port)
      );

    vertx.deployVerticle(todoVerticle, options, res -> {
      if (res.succeeded())
        System.out.println("Todo service is running at " + port + " port...");
      else
        res.cause().printStackTrace();
    });
  }
}
