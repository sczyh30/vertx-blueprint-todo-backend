package io.vertx.blueprint.todolist;

/**
 * Constant class.
 *
 * @author <a href="http://www.sczyh30.com">Eric Zhao</a>
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
