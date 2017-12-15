package io.vertx.blueprint.todolist.service;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.blueprint.todolist.entity.Todo;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JDBC implementation of {@link TodoService}.
 *
 * @author <a href="http://www.sczyh30.com">Eric Zhao</a>
 */
public class JdbcTodoService implements TodoService {

  private final Vertx vertx;
  private final JsonObject config;
  private final JDBCClient client;

  public JdbcTodoService(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
    this.client = JDBCClient.createShared(vertx, config);
  }

  @Override
  public Completable initData() {
    return client.rxGetConnection()
      .flatMapCompletable(connection -> connection.rxExecute(SQL_CREATE)
        .doOnTerminate(connection::close)
      );
  }

  @Override
  public Single<Todo> insert(Todo todo) {
    JsonArray params = new JsonArray().add(todo.getId())
      .add(todo.getTitle())
      .add(todo.isCompleted())
      .add(todo.getOrder())
      .add(todo.getUrl());
    return client.rxUpdateWithParams(SQL_INSERT, params)
      .map(e -> todo);
  }

  @Override
  public Single<List<Todo>> getAll() {
    return client.rxQuery(SQL_QUERY_ALL)
      .map(ar -> ar.getRows().stream()
        .map(Todo::new)
        .collect(Collectors.toList())
      );
  }

  @Override
  public Maybe<Todo> getCertain(String todoID) {
    return client.rxQueryWithParams(SQL_QUERY, new JsonArray().add(todoID))
      .map(ResultSet::getRows)
      .toObservable()
      .flatMapIterable(e -> e)
      .singleElement()
      .map(Todo::new);
  }

  @Override
  public Maybe<Todo> update(String todoId, Todo newTodo) {
    return getCertain(todoId)
      .flatMap(old -> {
        Todo fnTodo = old.merge(newTodo);
        int updateId = old.getId();
        JsonArray params = new JsonArray().add(updateId)
          .add(fnTodo.getTitle())
          .add(fnTodo.isCompleted())
          .add(fnTodo.getOrder())
          .add(fnTodo.getUrl())
          .add(updateId);
        return client.rxUpdateWithParams(SQL_UPDATE, params)
          .flatMapMaybe(v -> Maybe.just(fnTodo));
      });
  }

  @Override
  public Completable delete(String todoId) {
    return client.rxUpdateWithParams(SQL_DELETE, new JsonArray().add(todoId))
      .toCompletable();
  }

  @Override
  public Completable deleteAll() {
    return client.rxUpdate(SQL_DELETE_ALL).toCompletable();
  }

  private static final String SQL_CREATE = "CREATE TABLE IF NOT EXISTS `todo` (\n" +
    "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
    "  `title` varchar(255) DEFAULT NULL,\n" +
    "  `completed` tinyint(1) DEFAULT NULL,\n" +
    "  `order` int(11) DEFAULT NULL,\n" +
    "  `url` varchar(255) DEFAULT NULL,\n" +
    "  PRIMARY KEY (`id`) )";
  private static final String SQL_INSERT = "INSERT INTO `todo` " +
    "(`id`, `title`, `completed`, `order`, `url`) VALUES (?, ?, ?, ?, ?)";
  private static final String SQL_QUERY = "SELECT * FROM todo WHERE id = ?";
  private static final String SQL_QUERY_ALL = "SELECT * FROM todo";
  private static final String SQL_UPDATE = "UPDATE `todo`\n" +
    "SET\n" +
    "`id` = ?,\n" +
    "`title` = ?,\n" +
    "`completed` = ?,\n" +
    "`order` = ?,\n" +
    "`url` = ?\n" +
    "WHERE `id` = ?;";
  private static final String SQL_DELETE = "DELETE FROM `todo` WHERE `id` = ?";
  private static final String SQL_DELETE_ALL = "DELETE FROM `todo`";
}
