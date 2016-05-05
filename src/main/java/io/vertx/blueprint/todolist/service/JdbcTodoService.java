package io.vertx.blueprint.todolist.service;

import io.vertx.blueprint.todolist.entity.Todo;

import io.vertx.core.Future;

import java.util.List;

/**
 * Vert.x Blueprint Application - Todo Backend
 * Todo Service JDBC Implementation
 *
 * @author Eric Zhao
 */
public class JdbcTodoService implements TodoService {

  @Override
  public Future<Boolean> insert(Todo todo) {
    return null;
  }

  @Override
  public Future<List<Todo>> getAll() {
    return null;
  }

  @Override
  public Future<Todo> getCertain(String todoID) {
    return null;
  }

  @Override
  public Future<Todo> update(String todoId, Todo newTodo) {
    return null;
  }

  @Override
  public Future<Boolean> delete(String todoId) {
    return null;
  }

  @Override
  public Future<Boolean> deleteAll() {
    return null;
  }
}
