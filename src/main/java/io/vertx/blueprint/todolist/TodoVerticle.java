package io.vertx.blueprint.todolist;


import io.vertx.blueprint.todolist.entity.Todo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Vert.x todo Verticle
 */
public class TodoVerticle extends AbstractVerticle {

  private static String HOST = "127.0.0.1";
  private static int PORT = 8082;

  private RedisClient redis;

  /**
   * Init the redis client and save sample data
   */
  private void initData() {
    RedisOptions config;
    // this is for OpenShift Redis Cartridge
    String osPort = System.getenv("OPENSHIFT_REDIS_PORT");
    String osHost = System.getenv("OPENSHIFT_REDIS_HOST");
    if (osPort != null && osHost != null)
      config = new RedisOptions()
        .setHost(osHost).setPort(Integer.parseInt(osPort));
    else
      config = new RedisOptions().setHost(HOST);

    redis = RedisClient.create(vertx, config);

    redis.hset(Constants.REDIS_TODO_KEY, "98", Json.encodePrettily(
      new Todo(98, "Something to do...", false, 1, "todo/ex")), res -> {
      if (res.failed()) {
        System.err.println("[Error]Redis service is not running!");
        res.cause().printStackTrace();
      }
    });

  }

  @Override
  public void start(Future<Void> future) throws Exception {
    initData();

    Router router = Router.router(vertx);
    // CORS support
    Set<String> allowHeaders = new HashSet<>();
    allowHeaders.add("x-requested-with");
    allowHeaders.add("Access-Control-Allow-Origin");
    allowHeaders.add("origin");
    allowHeaders.add("Content-Type");
    allowHeaders.add("accept");
    Set<HttpMethod> allowMethods = new HashSet<>();
    allowMethods.add(HttpMethod.GET);
    allowMethods.add(HttpMethod.POST);
    allowMethods.add(HttpMethod.DELETE);
    allowMethods.add(HttpMethod.PATCH);

    router.route().handler(BodyHandler.create());
    router.route().handler(CorsHandler.create("*")
      .allowedHeaders(allowHeaders)
      .allowedMethods(allowMethods));

    // routes
    router.get(Constants.API_GET).handler(this::handleGetTodo);
    router.get(Constants.API_LIST_ALL).handler(this::handleGetAll);
    router.post(Constants.API_CREATE).handler(this::handleCreateTodo);
    router.patch(Constants.API_UPDATE).handler(this::handleUpdateTodo);
    router.delete(Constants.API_DELETE).handler(this::handleDeleteOne);
    router.delete(Constants.API_DELETE_ALL).handler(this::handleDeleteAll);

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(config().getInteger("http.port", PORT),
        System.getProperty("http.address", HOST), result -> {
          if (result.succeeded())
            future.complete();
          else
            future.fail(result.cause());
        });
  }

  private void handleCreateTodo(RoutingContext context) {
    final Todo todo = wrapObject(getTodoFromJson
      (context.getBodyAsString()), context);
    final String encoded = Json.encodePrettily(todo);
    redis.hset(Constants.REDIS_TODO_KEY, String.valueOf(todo.getId()),
      encoded, res -> {
        if (res.succeeded())
          context.response()
            .setStatusCode(201)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(encoded);
        else
          sendError(503, context.response());
      });
  }

  private void handleGetTodo(RoutingContext context) {
    String todoID = context.request().getParam("todoId");
    if (todoID == null)
      sendError(400, context.response());
    else {
      redis.hget(Constants.REDIS_TODO_KEY, todoID, x -> {
        if (x.succeeded()) {
          String result = x.result();
          if (result == null)
            sendError(404, context.response());
          else {
            context.response()
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(result);
          }
        } else
          sendError(503, context.response());
      });
    }
  }

  private void handleGetAll(RoutingContext context) {
    redis.hvals(Constants.REDIS_TODO_KEY, res -> {
      if (res.succeeded()) {
        String encoded = Json.encodePrettily(res.result().stream()
          .map(x -> getTodoFromJson((String) x))
          .collect(Collectors.toList()));
        context.response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(encoded);
      } else
        sendError(503, context.response());
    });
  }

  private void handleUpdateTodo(RoutingContext context) {
    String todoID = context.request().getParam("todoId");
    final Todo newTodo = getTodoFromJson(context.getBodyAsString());
    // handle error
    if (newTodo == null) {
      sendError(400, context.response());
      return;
    }

    redis.hget(Constants.REDIS_TODO_KEY, todoID, x -> {
      if (x.succeeded()) {
        String result = x.result();
        if (result == null)
          sendError(404, context.response());
        else {
          Todo oldTodo = getTodoFromJson(result);
          String response = Json.encodePrettily(oldTodo.merge(newTodo));
          redis.hset(Constants.REDIS_TODO_KEY, todoID, response, res -> {
            if (res.succeeded()) {
              context.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(response);
            }
          });
        }
      } else
        sendError(503, context.response());
    });
  }

  private void handleDeleteOne(RoutingContext context) {
    String todoID = context.request().getParam("todoId");
    redis.hdel(Constants.REDIS_TODO_KEY, todoID, res -> {
      if (res.succeeded())
        context.response().setStatusCode(204).end();
      else
        sendError(503, context.response());
    });
  }

  private void handleDeleteAll(RoutingContext context) {
    redis.del(Constants.REDIS_TODO_KEY, res -> {
      if (res.succeeded())
        context.response().setStatusCode(204).end();
      else
        sendError(503, context.response());
    });
  }

  private void sendError(int statusCode, HttpServerResponse response) {
    response.setStatusCode(statusCode).end();
  }

  /**
   * Decode Todo entity from JSON str
   *
   * @param jsonStr JSON str
   * @return Todo entity
   */
  private Todo getTodoFromJson(String jsonStr) {
    return Json.decodeValue(jsonStr, Todo.class);
  }

  /**
   * Wrap the Todo entity with appropriate id and url
   *
   * @param todo    a todo entity
   * @param context RoutingContext
   * @return the wrapped todo entity
   */
  private Todo wrapObject(Todo todo, RoutingContext context) {
    if (todo.getId() == 0)
      todo.setId(Math.abs(new Random().nextInt()));
    todo.setUrl(context.request().absoluteURI() + "/" + todo.getId());
    return todo;
  }

}
