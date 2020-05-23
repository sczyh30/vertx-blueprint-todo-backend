package io.vertx.blueprint.todolist;

import io.vertx.blueprint.todolist.entity.Todo;

import io.vertx.blueprint.todolist.verticle.RxTodoVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for Todo API
 *
 * @author Eric Zhao
 */
@RunWith(VertxUnitRunner.class)
public class TodoApiTest {

  private final static int PORT = 8084;
  private Vertx vertx;

  private final Todo todoEx = new Todo(164, "Test case...", false, 22, "http://127.0.0.1:8082/todos/164");
  private final Todo todoUp = new Todo(164, "Test case...Update!", false, 26, "http://127.0.0.1:8082/todos/164");

  @Before
  public void before(TestContext context) {
    vertx = Vertx.vertx();
    final DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", PORT));
    // default config
    RxTodoVerticle todoVerticle = new RxTodoVerticle();

    vertx.deployVerticle(todoVerticle, options,
      context.asyncAssertSuccess());
  }

  @After
  public void after(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test(timeout = 3000L)
  public void testAdd(TestContext context) throws Exception {
    HttpClient client = vertx.createHttpClient();
    Async async = context.async();
    Todo todo = new Todo(164, "Test case...", false, 22, "/164");
    client.post(PORT, "localhost", "/todos", response -> {
      context.assertEquals(201, response.result().statusCode());
      client.close();
      async.complete();
    }).putHeader("content-type", "application/json").end(Json.encodePrettily(todo));
  }

  @Test(timeout = 3000L)
  public void testGet(TestContext context) throws Exception {
    HttpClient client = vertx.createHttpClient();
    Async async = context.async();
    client.getNow(PORT, "localhost", "/todos/164", response -> response.result().bodyHandler(body -> {
      context.assertEquals(new Todo(body.toString()), todoEx);
      client.close();
      async.complete();
    }));
  }

  @Test(timeout = 3000L)
  public void testUpdateAndDelete(TestContext context) throws Exception {
    HttpClient client = vertx.createHttpClient();
    Async async = context.async();
    Todo todo = new Todo(164, "Test case...Update!", false, 26, "/164h");
    client.request(HttpMethod.PATCH, PORT, "localhost", "/todos/164", response -> response.result().bodyHandler(body -> {
      context.assertEquals(new Todo(body.toString()), todoUp);
      client.request(HttpMethod.DELETE, PORT, "localhost", "/todos/164", rsp -> {
        context.assertEquals(204, rsp.result().statusCode());
        async.complete();
      }).end();
    })).putHeader("content-type", "application/json").end(Json.encodePrettily(todo));
  }

}