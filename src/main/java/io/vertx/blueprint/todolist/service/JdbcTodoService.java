package io.vertx.blueprint.todolist.service;

import io.vertx.blueprint.todolist.Utils;
import io.vertx.blueprint.todolist.entity.Todo;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Vert.x Blueprint Application - Todo Backend
 * Todo Service JDBC Implementation
 *
 * @author Eric Zhao
 */
public class JdbcTodoService implements TodoService {

  private final Vertx vertx;
  private final JsonObject config;
  private final JDBCClient client;

  private static final String SQL_CREATE = "CREATE TABLE IF NOT EXISTS `todo` (\n" +
    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
    "  `title` varchar(255) DEFAULT NULL,\n" +
    "  `completed` tinyint(1) DEFAULT NULL,\n" +
    "  `order` int(11) DEFAULT NULL,\n" +
    "  `url` varchar(255) DEFAULT NULL,\n" +
    "  PRIMARY KEY (`id`) )";
  private static final String SQL_INSERT = "INSERT INTO `todo` " +
    "(`title`, `completed`, `order`, `url`) VALUES (?, ?, ?, ?)";
  private static final String SQL_QUERY = "SELECT * FROM todo WHERE id = ?";
  private static final String SQL_QUERY_ALL = "SELECT * FROM todo";
  private static final String SQL_UPDATE = "";
  private static final String SQL_DELETE = "DELETE FROM `todo` WHERE `id` = %s";
  private static final String SQL_DELETE_ALL = "DELETE FROM `todo`";

  public JdbcTodoService(JsonObject config) {
    this(Vertx.vertx(), config);
  }

  public JdbcTodoService(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
    this.client = JDBCClient.createShared(vertx, config);
  }

  @Override
  public Future<Boolean> initData() {
    Future<Boolean> result = Future.future();
    client.getConnection(conn -> {
      if (conn.succeeded()) {
        final SQLConnection connection = conn.result();
        connection.execute(SQL_CREATE, create -> {
          if (create.succeeded())
            result.complete(true);
          else
            result.fail(create.cause());
        });
      } else {
        result.fail(conn.cause());
      }
    });
    return result;
  }

  @Override
  public Future<Boolean> insert(Todo todo) {
    Future<Boolean> result = Future.future();
    client.getConnection(conn -> {
      if (conn.succeeded()) {
        final SQLConnection connection = conn.result();
        connection.updateWithParams(SQL_INSERT, new JsonArray().add(todo.getTitle())
        .add(todo.isCompleted())
        .add(todo.getOrder())
        .add(todo.getUrl()), r -> {
          if (r.failed()) {
            result.fail(r.cause());
          } else {
            result.complete(true);
          }
        });
        connection.close();
      } else {
        result.fail(conn.cause());
      }
    });
    return result;
  }

  @Override
  public Future<List<Todo>> getAll() {
    Future<List<Todo>> result = Future.future();
    client.getConnection(conn -> {
      if (conn.succeeded()) {
        final SQLConnection connection = conn.result();
        connection.query(SQL_QUERY_ALL, r -> {
          if (r.failed()) {
            result.fail(r.cause());
          } else {
            List<Todo> todos = r.result().getRows().stream()
              .map(x -> Utils.getTodoFromJson((String) x.encode()))
              .collect(Collectors.toList());
            result.complete(todos);
          }
          connection.close();
        });
      } else {
        result.fail(conn.cause());
      }
    });
    return result;
  }

  @Override
  public Future<Todo> getCertain(String todoID) {
    Future<Todo> result = Future.future();
    client.getConnection(conn -> {
      if (conn.succeeded()) {
        final SQLConnection connection = conn.result();
        connection.queryWithParams(SQL_QUERY, new JsonArray().add(todoID), r -> {
          if (r.failed()) {
            result.fail(r.cause());
          } else {
            List<JsonObject> list = r.result().getRows();
            if (list == null || list.isEmpty()) {
              result.complete(null);
            } else {
              result.complete(Utils.getTodoFromJson(list.get(0).encode()));
            }
          }
          connection.close();
        });
      } else {
        result.fail(conn.cause());
      }
    });
    return result;
  }

  @Override
  public Future<Todo> update(String todoId, Todo newTodo) {
    Future<Todo> result = Future.future();
    client.getConnection(conn -> {
      if (conn.succeeded()) {
        final SQLConnection connection = conn.result();
        // TODO: implement this
        connection.close();
      } else {
        result.fail(conn.cause());
      }
    });
    return result;
  }

  private Future<Boolean> deleteProcess(String sql) {
    Future<Boolean> result = Future.future();
    client.getConnection(conn -> {
      if (conn.succeeded()) {
        final SQLConnection connection = conn.result();
        connection.execute(sql, r -> {
          if (r.failed()) {
            result.complete(false);
          } else {
            result.complete(true);
          }
          connection.close();
        });
      } else {
        result.fail(conn.cause());
      }
    });
    return result;
  }

  @Override
  public Future<Boolean> delete(String todoId) {
    return deleteProcess(String.format(SQL_DELETE, todoId));
  }

  @Override
  public Future<Boolean> deleteAll() {
    return deleteProcess(SQL_DELETE_ALL);
  }
}
