# Vert.x Blueprint - Todo-Backend Tutorial

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
  compile 'io.vertx:vertx-jdbc-client:3.2.1'
  compile 'mysql:mysql-connector-java:6.0.2'
  compile 'io.vertx:vertx-redis-client:3.2.1'
  compile 'io.vertx:vertx-unit:3.2.1'

  testCompile group: 'junit', name: 'junit', version: '4.12'
}
```

You might not be familar with Gradle, that doesn't matter. Let's explain that:

- In the `jar` field, we configure it to generate **fat-jar** when compiles and point out the launcher class. A *fat-jar* is a convenient way to package a Vert.x application. It creates a jar containing both your application and all dependencies. Then, to launch it, you just need to execute `java -jar xxx.jar` without having to handle the `CLASSPATH`.
- We set both `targetCompatibility` and `sourceCompatibility` to **1.8**. This point is **important** as Vert.x requires Java 8.
- In `dependencies` field, we declares our dependencies. `vertx-core` and `vert-web` for REST API. `vertx-redis-client` and `vertx-jdbc-client` for data access(`mysql-connector-java` for MySQL driver, and you could replace by what you need). `vertx-unit` for test.

As we created `build.gradle`, let's start writing code~ Create the `src/main/java/io/vertx/blueprint/todolist/verticles/SingleApplicationVerticle.java ` file and write following content:

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


## REST API with Vert.x Web

## Decouple controller and service

## Asynchronous Pattern

## Test our service

## Packaging

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
