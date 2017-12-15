package io.vertx.blueprint.todolist.service;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.blueprint.todolist.entity.Todo;

import java.util.List;

/**
 * Reactive service interface of todo backend.
 *
 * @author Eric Zhao
 */
public interface TodoService {

  Completable initData();

  Single<Todo> insert(Todo todo);

  Single<List<Todo>> getAll();

  Maybe<Todo> getCertain(String todoID);

  Maybe<Todo> update(String todoId, Todo newTodo);

  Completable delete(String todoId);

  Completable deleteAll();

}
