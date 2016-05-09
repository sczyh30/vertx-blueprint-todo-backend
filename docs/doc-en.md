# Vert.x Blueprint - Todo-Backend Tutorial

## Table of contents

- [Preface](#preface)
- [Introduction to Vert.x](introduction-to-vertx)
- [Our application - Todo service](our-application---todo-service)
- [Let's Start!](lets-start)
    - [Gradle build file](gradle-build-file)
    - [Todo entity](todo-entity)
    - [Verticle](verticle)
- [REST API with Vert.x Web](rest-api-with-vertx-web)

## Preface

In this tutorial, we are going to develop a RESTful web service - Todo Backend. This service provide a simple RESTful API in the form of a todo list, which we could add or complete todo stuff.

What you are going to learn:

- What is Vert.x and its basic design
- What is and how to use `Verticle`
- How to develop a REST API using Vert.x Web
- How to make use of *asynchronous development model*
- How to use persistence such as *Redis* and *MySQL* with the help of Vert.x data access components
- How to test your application

The code developed in this tutorial is available on [GitHub](https://github.com/sczyh30/vertx-blueprint-todo-backend/tree/master).

## Introduction to Vert.x

Welcome to the world of Vert.x! When you first heard about Vert.x, you must be wondering: what's this?

Let's move on the definition of Vert.x:

> Vert.x is a tool-kit for building reactive applications on the JVM.

(⊙o⊙)... Toolkit? Reactive? Seems so fuzzy... Let's explain these words in brief. **"Toolkit"** means that it's lightweight. It could be embedded in your current application without changing the structure.
Another significant word is **reactive**. Vert.x is made to build reactive systems. Reactive system has been defined in the [Reactive Manifesto](http://reactivemanifesto.org/). We could summarize it in 4 points:

- **Responsive** : The system responds in a timely manner if at all possible.
- **Resilient** : The system stays responsive in the face of failure (e.g. internal error).
- **Elastic** : The system stays responsive under varying workload.
- **Message Driven:** The components of the system communicate with each other using [asynchronous message-passing](http://www.reactivemanifesto.org/glossary#Asynchronous).

Vert.x is event driven and non-blocking. First, let's introduce **Event Loop** thread. Event loops are a group of threads that are responsible for dispatching and handling events in `Handler`. Every event is delivered and handled in an event loop. Notice, we **must never block the event loop** or our application will not be responsive as the process of handling events will be blocked. When building Vert.x applications, we should keep the asynchronous and non-blocking model in mind, rather than traditional blocking model. We will see the detail in the following sections.

## Our application - Todo service

Our application is a todo list REST service. It's very simple. The entire API consists of about 5 distinct operations (create a todo, view a todo, modify a todo, list all todos, delete all todos), which correspond to CRUD operations.

So we could design the following routes:

- Add a todo entity: `POST /todos`
- Get a certain todo entity: `GET /todos/:todoId`
- Get all todo entities: `GET /todos`
- Update a todo entity: `PATCH /todos/:todoId`
- Delete a certain todo entity: `DELETE /todos/:todoId`
- Delete all todo entities: `DELETE /todos`

The level of *REST* of the API is not the topic of this post, so you could also design as you like.

Now let's start!

## Let's Start!

Vert.x core provides a fairly low level set of functionality for handling HTTP, and for some applications that will be sufficient. That's the main reason behind [Vert.x Web](http://vertx.io/docs/vertx-web/java). Vert.x-Web builds on Vert.x core to provide a richer set of functionality for building real web applications, more easily.

### Gradle build file

First, let's create the project. In this tutorial we use Gradle as build tool, but you can use any other build tools you prefer, such as Maven and SBT. Basically, you need a directory with:

1. a `src/main/java` directory
2. a `src/test/java` directory
3. a `build.gradle` file

The directory tree will be like this:

```
.
├── build.gradle
├── settings.gradle
├── src
│   ├── main
│   │   └── java
│   └── test
│       └── java
```

Let's create the `build.gradle` file:

```groovy
apply plugin: 'java'

targetCompatibility = 1.8
sourceCompatibility = 1.8

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {

  compile "io.vertx:vertx-core:3.2.1"
  compile 'io.vertx:vertx-web:3.2.1'

  testCompile group: 'junit', name: 'junit', version: '4.12'
}
```

You might not be familar with Gradle, that doesn't matter. Let's explain that:

- We set both `targetCompatibility` and `sourceCompatibility` to **1.8**. This point is **important** as Vert.x requires Java 8.
- In `dependencies` field, we declares our dependencies. `vertx-core` and `vert-web` for REST API.

/*`vertx-redis-client` and `vertx-jdbc-client` for data access(`mysql-connector-java` for MySQL driver, and you could replace by what you need). `vertx-unit` for test.*/

As we created `build.gradle`, let's start writing code~

### Todo entity

First we need to create our data object - the `Todo` entity. Create the `src/main/java/io/vertx/blueprint/todolist/entity/Todo.java` file and write:

```Java
package io.vertx.blueprint.todolist.entity;


public class Todo {

  private int id;
  private String title;
  private Boolean completed;
  private Integer order;
  private String url;

  public Todo() {
  }

  public Todo(int id, String title, Boolean completed, Integer order, String url) {
    this.id = id;
    this.title = title;
    this.completed = completed;
    this.order = order;
    this.url = url;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Boolean isCompleted() {
    return getOrElse(completed, false);
  }

  public void setCompleted(Boolean completed) {
    this.completed = completed;
  }

  public Integer getOrder() {
    return getOrElse(order, 0);
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }


  private <T> T getOrElse(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  public Todo merge(Todo todo) {
    return new Todo(id,
      getOrElse(todo.title, title),
      getOrElse(todo.completed, completed),
      getOrElse(todo.order, order),
      url);
  }
}
```

Our `Todo` entity consists of id, title, order, url and a flag indicates if it is completed. This is a simple Java bean and could be marshaled to JSON format.

### Verticle

Then we start writing our verticle. Create the `src/main/java/io/vertx/blueprint/todolist/verticles/SingleApplicationVerticle.java ` file and write following content:

```java
package io.vertx.blueprint.todolist.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

public class SingleApplicationVerticle extends AbstractVerticle {

  private static final String HOST = "127.0.0.1";
  private static final int PORT = 8082;

  private final RedisClient redis;

  public SingleApplicationVerticle(RedisOptions redisOptions) {
    this.redis = RedisClient.create(Vertx.vertx(), redisOptions);
  }

  @Override
  public void start(Future<Void> future) throws Exception {
      // TODO with start...
  }
}
```

Our class `SingleApplicationVerticle` extends `AbstractVerticle` class. In Vert.x, a **verticle** is a component of the application. We can deploy *verticles* to run the components.

The `start` method will be called when verticle is deployed. And notice this `start` method takes a parameter typed `Future<Void>`, which means this is asynchronous start method. The `Future` indicates whether your actions have been done. After done, you can call `complete` on the `Future` (or `fail`) to notify that you are done (success or failure).

So next step is to create a http server and configure the routes to handle HTTP requests.

## REST API with Vert.x Web

### Create HTTP server with route

Let's change the `start` method with:

```java
@Override
public void start(Future<Void> future) throws Exception {

  Router router = Router.router(vertx); // <1>
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

  router.route().handler(CorsHandler.create("*") // <2>
    .allowedHeaders(allowHeaders)
    .allowedMethods(allowMethods));
  router.route().handler(BodyHandler.create()); // <3>


  // TODO:routes

  vertx.createHttpServer() // <4>
    .requestHandler(router::accept)
    .listen(PORT, HOST, result -> {
        if (result.succeeded())
          future.complete();
        else
          future.fail(result.cause());
      });
}
```

Wow! A long snippet, yeah? Don't worry, I'll explain that.

First we create a `Router` object (1). The router is responsible for dispatching HTTP requests to the certain right handler. We could define routes to handle different requests with different handlers. A `Handler` is responsible for handling requests and writing result. The handlers will be invoked when corresponding request arrives. These concepts are very common in web development.

Then we created two sets, `allowHeaders` and `allowMethods`. And then we add some HTTP headers and methods to the set, and then we attach the `CorsHandler` to the router (2). The `route()` method with no parameters means that it matches all requests. These two sets are about *CORS support*.
The most common initial gotcha when implementing Todo-Backend is getting CORS headers right. Both the Todo-Backend web client and the Todo-Backend specs themselves will be running javascript from a different domain than the one where our API implementation will live.
That means we need to enable [CORS support](http://enable-cors.org/server.html) by including a couple of custom HTTP headers and responding to the relevant OPTIONS HTTP requests. We don't fall into the detail of CORS. We just need to know how it can support CORS.

Next we attach the `BodyHandler` to the router (3). The `BodyHandler` allows you to retrieve request bodies and read data. For example, we can retrieve JSON data from the request body when implementing our REST service. We could enable it globally with `router.route().handler(BodyHandler.create())`.

Finally we create a HTTP server with `vertx.createHttpServer()` method. Note, this is the functionality of Vert.x Core. We then attach our *router handler* to the server, which is actually the role of Vert.x Web.
You may not familiar with the format like `router::accept`. It's a method reference and here it constructs a handler that handles requests by route. When requests arrive, Vert.x will call `accept` method.
The sever is bound to the 8082 port. And because the process might fail, we also pass a handler to `listen` method to check whether the server is established or failed.
As we mentioned above, we use `future.complete` to notify success and `future.fail` to notify failure.

So far, we have created the HTTP server. But you haven't see routes about our service yeah? It's time to declare them!

### Configure the routes

Now let's declare our todo service routes. As we mentioned above, we designed our route as follows:

- Add a todo entity: `POST /todos`
- Get a certain todo entity: `GET /todos/:todoId`
- Get all todo entities: `GET /todos`
- Update a todo entity: `PATCH /todos/:todoId`
- Delete a certain todo entity: `DELETE /todos/:todoId`
- Delete all todo entities: `DELETE /todos`

[NOTE Path Parameter | In the URL, We could define path parameters using placeholder `:` followed by the parameter name. When handling a matching request, Vert.x will automatically fetch the corresponding parameter. For example, `/todos/19` maps `todoId` to `19`.]

First we create a `Constants` class in the root package(`io.vertx.blueprint.todolist`) and store the path of routes:

```java
package io.vertx.blueprint.todolist;

public final class Constants {

  private Constants() {}

  /** API Route */
  public static final String API_GET = "/todos/:todoId";
  public static final String API_LIST_ALL = "/todos";
  public static final String API_CREATE = "/todos";
  public static final String API_UPDATE = "/todos/:todoId";
  public static final String API_DELETE = "/todos/:todoId";
  public static final String API_DELETE_ALL = "/todos";

}
```

Then in `start` method, replace `TODO` field with the following content:

```java
// routes
router.get(Constants.API_GET).handler(this::handleGetTodo);
router.get(Constants.API_LIST_ALL).handler(this::handleGetAll);
router.post(Constants.API_CREATE).handler(this::handleCreateTodo);
router.patch(Constants.API_UPDATE).handler(this::handleUpdateTodo);
router.delete(Constants.API_DELETE).handler(this::handleDeleteOne);
router.delete(Constants.API_DELETE_ALL).handler(this::handleDeleteAll);
```

The code is clear. We use corresponding method(e.g. `get`, `post`, `delete` ...) to bind the path to the route. And we call `handler` method to attach certain handler to the route. Note the type of handler is `Handler<RoutingContext>` and here we pass six method references to the `handler` method, each of which takes a `RoutingContext` parameter and returns void. We'll implement these six handler methods soon.

### Asynchronous Pattern

As we mentioned above, Vert.x is asynchronous and non-blocking. Every asynchronous method takes a `Handler` parameter as the callback and when the process is done, the handler will be called. There is also an equivalent pattern that returns a `Future` object:

```java
void doAsync(A a, B b, Handler<R> handler);
// these two are equivalent
Future<R> doAsync(A a, B b);
```

The `Future` object refers to the result of an action that may not start, or pending, or finish or fail. We could also attach a `Handler` on the `Future` object:

```java
Future<R> future = doAsync(A a, B b);
future.setHandler(r -> {
    if (r.failed()) {
        // do something on the failure
    } else {
        // do something on the result
    }
});
```

Most of Vert.x APIs are handler-based pattern. We will see both of the two patterns below.

### Todo logic implementation

Now It's time to implement our todo logic! Here we will use *Redis* as the backend persistence. [Redis](http://redis.io/) is an open source, in-memory data structure store, used as database, cache and message broker. It is often referred to as a data structure server since keys can contain strings, hashes, lists, sets and sorted sets. And fortunately, Vert.x provides us Vert.x-redis, a component that allows us to process data with Redis.

[NOTE How to install and run Redis? | Please follow the concrete instruction on [Redis Website](http://redis.io/download#installation) ]

#### Vert.x Redis

Vert.x-redis allows data to be saved, retrieved, searched for, and deleted in a Redis asynchronously. To use the Vert.x Redis client, we should add the following dependency to the *dependencies* section of `build.gradle`:

```gradle
compile 'io.vertx:vertx-redis-client:3.2.1'
```

We can access to Redis by `RedisClient` object. So we define a `RedisClient` object as a class object. Before we use `RedisClient`, we should connect to Redis and there is a config required. This config is provided in the form of `RedisOptions`. We will discuss the values of config later.

Add the following code to the `SingleApplicationVerticle` class:

```java
private final RedisClient redis;

public SingleApplicationVerticle(RedisOptions redisOptions) {
  this.redis = RedisClient.create(Vertx.vertx(), redisOptions);
}
```

Then, as soon as we create a verticle instance, the redis client will be created.

#### Store format

As Redis support various format of data, We store our todo objects in a *HashTable*.
Every data in the hash table has key and value. Here we use `id` as key and todo entity in **JSON** format as value.

Recall, the hash table should have a name(key), so we name it as *VERT_TODO*. Let's add the following constant to the `Constants` class:

```java
public static final String REDIS_TODO_KEY = "VERT_TODO";
```

Vert.x provides several methods to encode objects to JSON format (`JsonObject`) and decode JSON to a certain entity. Here we create an `Utils` class in `io.vertx.blueprint.todolist` root package and wrap a `getTodoFromJson` method:

```java
package io.vertx.blueprint.todolist;

import io.vertx.blueprint.todolist.entity.Todo;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;


public class Utils {

  private Utils() {}

  public static Todo getTodoFromJson(String jsonStr) {
    if (jsonStr == null)
      return null;
    else
      return Json.decodeValue(jsonStr, Todo.class);
  }

}
```

This method could resolve todo entity from JSON string. It's convenient as the todo entities we retrieve from Redis is in JSON string format.

#### Get/Get all

Let's implement the logic of getting todo objects. As we mentioned above, the hanlder method should take a `RoutingContext` as parameter and return void. Let's implement `handleGetTodo` method first:

```java
private void handleGetTodo(RoutingContext context) {
  String todoID = context.request().getParam("todoId"); // (1)
  if (todoID == null)
    sendError(400, context.response()); // (2)
  else {
    redis.hget(Constants.REDIS_TODO_KEY, todoID, x -> { // (3)
      if (x.succeeded()) {
        String result = x.result();
        if (result == null)
          sendError(404, context.response());
        else {
          context.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(result); // (4)
        }
      } else
        sendError(503, context.response());
    });
  }
}
```

First we retrieve the path parameter `todoId` using `getParam` (1). We should check whether the parameter exists and if not, the server should send a `400 Bad Request` error response to client (2); Here we wrap a method that could send error response:

```java
private void sendError(int statusCode, HttpServerResponse response) {
  response.setStatusCode(statusCode).end();
}
```

The `end` method is of vital importance. The response could be sent to client only if we call this method.

Back to the `handleGetTodo` method. If the parameter is okay, we could fetch the todo object by `todoId` from Redis. Here we use `hget` operation (3), which means get an entry by key from the hash table. Let's see the signature of `hget` method:

```java
RedisClient hget(String key, String field, Handler<AsyncResult<String>> handler);
```

The first parameter `key` is the name(key) of the hash table. The second parameter `field` is the key of the data. The third parameter is a handler that handles the result when action has been done. In the handler, first we should check if the action is successful. If not, the server should send a `503 Service Unavailable` error response to the client. If done, we could get the result. If the result is `null`, that indicates there is no todo object matches the `todoId` so we should return `404 Not Found` status. If the result is valid, we could write it to response by `end` method (4). Notice our REST API returns JSON data, so we set `content-type` header as JSON type.

The logic of `handleGetAll` is similar to `handleGetTodo`, but the implementation has some difference:

```java
private void handleGetAll(RoutingContext context) {
  redis.hvals(Constants.REDIS_TODO_KEY, res -> { // (1)
    if (res.succeeded()) {
      String encoded = Json.encodePrettily(res.result().stream() // (2)
        .map(x -> Utils.getTodoFromJson((String) x))
        .collect(Collectors.toList()));
      context.response()
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(encoded); // (3)
    } else
      sendError(503, context.response());
  });
}
```

Here we use `hvals` operation (1). `hvals` returns all values in the hash stored at key. In Vert.x-redis, it returns data as a `JsonArray` object. In the handler we first check whether the action is successful as before. If okay, we could write the result to response.Notice that we cannot directly write the returning `JsonArray` to the response as each value in `JsonArray` we retrieved from Redis was escaped so some characters are not correct. So we should first convert them into todo entity and then re-encode them to JSON.

Here we use an approach with functional style. Because the `JsonArray` class implements `Iterable<Object>` interface (behave like `List` yeah?), we could convert it to `Stream` using `stream` method. The `Stream` here is not the IO stream, but data flow. Then we call `Utils.getTodoFromJson` method on every item to convert every value(in string format) to `Todo` entity, using `map` operator. We don't explain `map` operator in detail but, it's really important in functional programming. After mapping, we collect the `Stream` in the form of `List<Todo>`. Now we could use `Json.encodePrettily` method to convert the list to JSON string. Finally we write the encoded result to response as before (3).

#### Create Todo

After having done two APIs above, you are more familar with Vert.x~ Now let's implement the logic of creating todo :

```java
private void handleCreateTodo(RoutingContext context) {
  final Todo todo = wrapObject(Utils.getTodoFromJson // (1)
    (context.getBodyAsString()), context);
  final String encoded = Json.encodePrettily(todo); // (2)
  redis.hset(Constants.REDIS_TODO_KEY, String.valueOf(todo.getId()), // (3)
    encoded, res -> {
      if (res.succeeded())
        context.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(encoded); // (4)
      else
        sendError(503, context.response());
    });
}
```

```java
private Todo wrapObject(Todo todo, RoutingContext context) {
  if (todo.getId() == 0)
    todo.setId(Math.abs(new Random().nextInt()));
  todo.setUrl(context.request().absoluteURI() + "/" + todo.getId());
  return todo;
}
```

#### Update

```java
private void handleUpdateTodo(RoutingContext context) {
  String todoID = context.request().getParam("todoId"); // (1)
  final Todo newTodo = Utils.getTodoFromJson(context.getBodyAsString()); // (2)
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
}
```

#### Remove/Remove all

```java
private void handleDeleteOne(RoutingContext context) {
  String todoID = context.request().getParam("todoId");
  redis.hdel(Constants.REDIS_TODO_KEY, todoID, res -> {
    if (res.succeeded())
      context.response().setStatusCode(204).end();
    else
      sendError(503, context.response());
  });
}
```

```java
private void handleDeleteAll(RoutingContext context) {
  redis.del(Constants.REDIS_TODO_KEY, res -> {
    if (res.succeeded())
      context.response().setStatusCode(204).end();
    else
      sendError(503, context.response());
  });
}
```

### Launcher Class

- In the `jar` field, we configure it to generate **fat-jar** when compiles and point out the launcher class. A *fat-jar* is a convenient way to package a Vert.x application. It creates a jar containing both your application and all dependencies. Then, to launch it, you just need to execute `java -jar xxx.jar` without having to handle the `CLASSPATH`.

### Run our service

Now it's time to run our REST service! Let's build and run the application:

```bash
gradle build
java -jar
```

## Decouple controller and service


### Asynchronous service using Future

```java
package io.vertx.blueprint.todolist.service;

import io.vertx.blueprint.todolist.entity.Todo;
import io.vertx.core.Future;

import java.util.List;
import java.util.Optional;


public interface TodoService {

  Future<Boolean> initData();

  Future<Boolean> insert(Todo todo);

  Future<List<Todo>> getAll();

  Future<Optional<Todo>> getCertain(String todoID);

  Future<Todo> update(String todoId, Todo newTodo);

  Future<Boolean> delete(String todoId);

  Future<Boolean> deleteAll();

}
```

### Refactor!



### Implement our service with Vert.x-Redis



### Implement our service with Vert.x-JDBC



### Run!



## Test our service



## Cheers!



## From other frameworks?

Well, you might have used other frameworks before, like Spring Boot. In this section, I will use analogy to introduce the concepts about Vert.x.

### From Spring Boot/Spring MVC

In Spring Boot, we usually configure the routes and handle the http requests in the Controller:

```java
@RestController
@ComponentScan
@EnableAutoConfiguration
public class TodoController {

  @Autowired
  private TodoService service;

  @RequestMapping(method = RequestMethod.GET, value = "/todos/{id}")
  public Todo getCertain(@PathVariable("id") int id) {
    return service.fetch(id);
  }
}
```

In Spring Boot, we configure the route using the annotation `@RequestMapping`. In Vert.x Web, things are a little different. We configure the route on the `Router` instance. And because Vert.x Web is asynchronous, we attach a `Handler` as the controller method callback to each route.

As for sending response, we use `end` method to write HTTP response in Vert.x Web. In Spring Boot, we send response to client simply by returning the result directly in the controller method.

### From Play Framework 2

If you are from Play Framework 2, you must be familiar with its asynchronous programming model. In Play Framework 2, we configure the route on the `routes` file, like the pattern `method path controller`:

```scala
GET     /todos/:todoId      controllers.TodoController.handleGetCertain(todoId: Int)
```

In Vert.x Web, we configure the route on the `Router` instance. So we can rewrite the above config:

```java
router.get("/todos/:todoId").handler(this::handleGetCertain);
```

`this::handleGetCertain` is the method reference (handler) that handles the request.

In Play Framework 2, we use asynchronous pattern with `Future`. Every handler returns an `Action`, which refers to type `Request[A] => Result`. We write our logic inside the `block: => Future[Result]` (or simply `block: R[A] => Result`) function closure like this:

```scala
def handleGetCertain(todoId: Int): Action[AnyContent] = Action.async {
    service.getCertain(todoId) map { // service return `Future[Option[Todo]]`
        case Some(res) =>
            Ok(Json.toJson(res))
        case None =>
            NotFound()
    }
}
```

And in Vert.x, we use asynchronous pattern with *callback*. We could rewrite as follows:

```java
private void handleCreateTodo(RoutingContext context) {
    String todoId = context.request().getParam("todoId"); // get path variable
    service.getCertain(todoId).setHandler(r -> { // service return `Future<Optional<Todo>>`
        if (r.succeeded) {
            Optional<Todo> res = r.result;
            if (res.isPresent()) {
                context.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(res)); // write `Result` to response with Ok(200)
            } else {
                sendError(404, context.response()); // NotFound(404)
            }
        } else {
            sendError(503, context.response());
        }
    });
}
```

In Vert.x, We write result to response directly by `end` method rather than encapsulate it with `Result`.

### Want to use other persistence frameworks?

You may want to use other persistence frameworks or libraries in Vert.x application, e.g. Mybatis ORM or Jedis. That's OK. You can use anything you want in Vert.x. But notice, ORM framework like Mybatis is synchronous and blocking, so the process could block the event loop. Thus, we need to use blocking handlers(`blockingHandler` method) to handle blocking requests:

```java
router.get("/todos/:todoId").blockingHandler(routingContext -> {
            String todoID = routingContext.request().getParam("todoId");
            Todo res = service.fetch(todoID);

            // do something and send response...

            routingContext.next();
        });
```

Vert.x will handle the blocking request in worker thread rather than event loop thread, so that it won't block the event loop.
