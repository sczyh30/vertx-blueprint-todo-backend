package io.vertx.blueprint.todolist;

/**
 * Vert.x Blueprint Application - Todo Backend
 * Constants class
 */
public final class Constants {

  private Constants() {}

  /** API Route */
  public static final String API_GET = "/todos/:todoId";
  public static final String API_LIST_ALL = "/todos";
  public static final String API_CREATE = "/todos";
  public static final String API_UPDATE = "/todos/:todoId";
  public static final String API_DELETE = "/todos/:todoId";
  public static final String API_DELETE_ALL = "/todos";

  /** Persistence key */
  public static final String REDIS_TODO_KEY = "VERT_TODO";

}
