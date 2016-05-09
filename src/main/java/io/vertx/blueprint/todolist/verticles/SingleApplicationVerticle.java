package io.vertx.blueprint.todolist.verticles;


import io.vertx.blueprint.todolist.Constants;
import io.vertx.blueprint.todolist.Utils;
import io.vertx.blueprint.todolist.entity.Todo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
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
 * Vert.x Blueprint Application - Todo Backend
 * Single Application Verticle (Redis)
 * Service and controller logic in one class
 *
 * @author Eric Zhao
 */
public class SingleApplicationVerticle extends AbstractVerticle {

  private static final String HOST = "127.0.0.1";
  private static final int PORT = 8082;

  private final RedisClient redis;

  public SingleApplicationVerticle(RedisOptions redisOptions) {
    this.redis = RedisClient.create(Vertx.vertx(), redisOptions);
  }

  /**
   * Init sample data
   */
  private void initData() {
    redis.hset(Constants.REDIS_TODO_KEY, "24", Json.encodePrettily(
      new Todo(24, "Something to do...", false, 1, "todo/ex")), res -> {
      if (res.failed()) {
        System.err.println("[Error] Redis service is not running!");
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
    try {
      final Todo todo = wrapObject(Utils.getTodoFromJson
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
    } catch (DecodeException e) {
      sendError(400, context.response());
    }
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
          .map(x -> Utils.getTodoFromJson((String) x))
          .collect(Collectors.toList()));
        context.response()
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(encoded);
      } else
        sendError(503, context.response());
    });
  }

  private void handleUpdateTodo(RoutingContext context) {
    try {
      String todoID = context.request().getParam("todoId");
      final Todo newTodo = Utils.getTodoFromJson(context.getBodyAsString());
      // handle error
      if (todoID == null || newTodo == null) {
        sendError(400, context.response());
        return;
      }

      redis.hget(Constants.REDIS_TODO_KEY, todoID, x -> {
        if (x.succeeded()) {
          String result = x.result();
          if (result == null)
            sendError(404, context.response());
          else {
            Todo oldTodo = Utils.getTodoFromJson(result);
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
    } catch (DecodeException e) {
      sendError(400, context.response());
    }
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
