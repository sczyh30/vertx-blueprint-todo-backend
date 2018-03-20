package io.vertx.blueprint.todolist.verticle;

import io.reactivex.Completable;
import io.vertx.blueprint.todolist.Constants;
import io.vertx.blueprint.todolist.common.RestfulApiVerticle;
import io.vertx.blueprint.todolist.entity.Todo;
import io.vertx.blueprint.todolist.service.RedisTodoService;
import io.vertx.blueprint.todolist.service.TodoService;
import io.vertx.core.Future;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.redis.RedisOptions;

import java.util.Objects;

/**
 * Reactive verticle of todo backend service.
 *
 * @author <a href="http://www.sczyh30.com">Eric Zhao</a>
 */
public class RxTodoVerticle extends RestfulApiVerticle {

  private static final Logger logger = LoggerFactory.getLogger(RxTodoVerticle.class);

  private static final String HOST = "0.0.0.0";
  private static final int PORT = 8082;

  private TodoService service;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Router router = Router.router(vertx);
    // Enable HTTP Body parse.
    router.route().handler(BodyHandler.create());
    // Enable CORS.
    enableCorsSupport(router);

    router.get(Constants.API_GET).handler(this::handleGetTodo);
    router.get(Constants.API_LIST_ALL).handler(this::handleGetAll);
    router.post(Constants.API_CREATE).handler(this::handleCreateTodo);
    router.patch(Constants.API_UPDATE).handler(this::handleUpdateTodo);
    router.delete(Constants.API_DELETE).handler(this::handleDeleteOne);
    router.delete(Constants.API_DELETE_ALL).handler(this::handleDeleteAll);

    String host = config().getString("http.address", HOST);
    int port = config().getInteger("http.port", PORT);

    initService().andThen(createHttpServer(router, host, port))
      .subscribe(startFuture::complete, startFuture::fail);
  }

  private void handleCreateTodo(RoutingContext context) {
    try {
      JsonObject rawEntity = context.getBodyAsJson();
      if (!Objects.isNull(rawEntity)) {
        final Todo todo = wrapObject(new Todo(rawEntity), context);
        // Call async service then send response back to client.
        sendResponse(context, service.insert(todo), Json::encodePrettily, this::created);
        return;
      }
      badRequest(context);
    } catch (DecodeException ex) {
      badRequest(context, ex);
    }
  }

  private void handleGetTodo(RoutingContext context) {
    String todoID = context.request().getParam("todoId");
    if (todoID == null) {
      badRequest(context);
      return;
    }
    sendResponse(context, service.getCertain(todoID), Json::encodePrettily);
  }

  private void handleGetAll(RoutingContext context) {
    sendResponse(context, service.getAll(), Json::encodePrettily);
  }

  private void handleUpdateTodo(RoutingContext context) {
    try {
      String todoID = context.request().getParam("todoId");
      final Todo newTodo = new Todo(context.getBodyAsString());
      // handle error
      if (todoID == null) {
        badRequest(context);
        return;
      }
      sendResponse(context, service.update(todoID, newTodo), Json::encodePrettily);
    } catch (DecodeException ex) {
      badRequest(context, ex);
    }
  }

  private void handleDeleteOne(RoutingContext context) {
    String todoID = context.request().getParam("todoId");
    sendResponse(context, service.delete(todoID), this::noContent);
  }

  private void handleDeleteAll(RoutingContext context) {
    sendResponse(context, service.deleteAll(), this::noContent);
  }

  private Completable initService() {
    RedisOptions config = new RedisOptions()
      .setHost(config().getString("redis.host", "127.0.0.1"))
      .setPort(config().getInteger("redis.port", 6379));
    service = new RedisTodoService(vertx, config);

    return service.initData();
  }

  /**
   * Wrap the todo entity with appropriate id and URL.
   *
   * @param todo    a todo entity
   * @param context RoutingContext
   * @return the wrapped todo entity
   */
  private Todo wrapObject(Todo todo, RoutingContext context) {
    int id = todo.getId();
    if (id > Todo.getIncId()) {
      Todo.setIncIdWith(id);
    } else if (id == 0)
      todo.setIncId();
    todo.setUrl(context.request().absoluteURI() + "/" + todo.getId());
    return todo;
  }
}
