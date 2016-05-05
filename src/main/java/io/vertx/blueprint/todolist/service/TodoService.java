package io.vertx.blueprint.todolist.service;

import io.vertx.blueprint.todolist.entity.Todo;
import io.vertx.core.Future;

import java.util.List;

/**
 * Vert.x Blueprint Application - Todo Backend
 * Todo Service Interface
 *
 * @author Eric Zhao
 */
public interface TodoService {

  Future<Boolean> insert(Todo todo);

  Future<List<Todo>> getAll();

  Future<Todo> getCertain(String todoID);

  Future<Todo> update(String todoId, Todo newTodo);

  Future<Boolean> delete(String todoId);

  Future<Boolean> deleteAll();

}
