package io.vertx.blueprint.todolist;

import io.vertx.blueprint.todolist.service.JdbcTodoService;
import io.vertx.blueprint.todolist.service.RedisTodoService;
import io.vertx.blueprint.todolist.verticles.SingleApplicationVerticle;
import io.vertx.blueprint.todolist.verticles.TodoVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;

/**
 * Vert.x Blueprint Application - Todo Backend
 * Launcher Class
 *
 * @author Eric Zhao
 */
public class Application {

  public static Verticle RedisTodo() {
    RedisOptions config;
    // this is for OpenShift Redis Cartridge
    String osPort = System.getenv("OPENSHIFT_REDIS_PORT");
    String osHost = System.getenv("OPENSHIFT_REDIS_HOST");
    if (osPort != null && osHost != null)
      config = new RedisOptions()
        .setHost(osHost).setPort(Integer.parseInt(osPort));
    else
      config = new RedisOptions().setHost("127.0.0.1");

    return new TodoVerticle(new RedisTodoService(config));
  }

  public static Verticle JdbcTodo() {
    // in this example we use MySQL
    JsonObject config = new JsonObject()
      .put("url", "jdbc:mysql://localhost/vertx_blueprint?characterEncoding=UTF-8&useSSL=false")
      .put("driver_class", "com.mysql.cj.jdbc.Driver")
      .put("user", "vbpdb1")
      .put("password", "666666*")
      .put("max_pool_size", 30);
    return new TodoVerticle(new JdbcTodoService(config));
  }

  /*public static Verticle singleTodo() {
    RedisOptions config;
    // this is for OpenShift Redis Cartridge
    String osPort = System.getenv("OPENSHIFT_REDIS_PORT");
    String osHost = System.getenv("OPENSHIFT_REDIS_HOST");
    if (osPort != null && osHost != null)
      config = new RedisOptions()
        .setHost(osHost).setPort(Integer.parseInt(osPort));
    else
      config = new RedisOptions().setHost("127.0.0.1");

    return new SingleApplicationVerticle(config);
  }*/

  public static void runTodo(Verticle todoVerticle) {
    Vertx vertx = Vertx.vertx();

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

  public static void main(String[] args) {
    runTodo(RedisTodo());
  }
}
