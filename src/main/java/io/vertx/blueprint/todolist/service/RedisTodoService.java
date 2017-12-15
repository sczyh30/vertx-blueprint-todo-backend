package io.vertx.blueprint.todolist.service;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.blueprint.todolist.Constants;
import io.vertx.blueprint.todolist.entity.Todo;

import io.vertx.core.json.Json;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Redis implementation of {@link TodoService}.
 *
 * @author <a href="http://www.sczyh30.com">Eric Zhao</a>
 */
public class RedisTodoService implements TodoService {

  private final Vertx vertx;
  private final RedisOptions config;
  private final RedisClient redis;

  public RedisTodoService(Vertx vertx, RedisOptions config) {
    this.vertx = vertx;
    this.config = config;
    this.redis = RedisClient.create(vertx, config);
  }

  @Override
  public Completable initData() {
    Todo sample = new Todo(Math.abs(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE)),
      "Something to do...", false, 1, "todo/ex");
    return insert(sample).toCompletable();
  }

  @Override
  public Single<Todo> insert(Todo todo) {
    final String encoded = Json.encodePrettily(todo);
    return redis.rxHset(Constants.REDIS_TODO_KEY, String.valueOf(todo.getId()), encoded)
      .map(e -> todo);
  }

  @Override
  public Single<List<Todo>> getAll() {
    return redis.rxHvals(Constants.REDIS_TODO_KEY)
      .map(e -> e.stream()
        .map(x -> new Todo((String) x))
        .collect(Collectors.toList())
      );
  }

  @Override
  public Maybe<Todo> getCertain(String todoID) {
    if (Objects.isNull(todoID)) {
      return Maybe.empty();
    }
    return redis.rxHget(Constants.REDIS_TODO_KEY, todoID)
      .toMaybe()
      .map(Todo::new);
  }

  @Override
  public Maybe<Todo> update(String todoId, Todo newTodo) {
    return getCertain(todoId)
      .map(old -> old.merge(newTodo))
      .flatMap(e -> insert(e)
        .flatMapMaybe(r -> Maybe.just(e))
      );
  }

  @Override
  public Completable delete(String todoId) {
    return redis.rxHdel(Constants.REDIS_TODO_KEY, todoId).toCompletable();
  }

  @Override
  public Completable deleteAll() {
    return redis.rxDel(Constants.REDIS_TODO_KEY).toCompletable();
  }
}
