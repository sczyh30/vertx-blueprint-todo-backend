package io.vertx.blueprint.todolist;

import io.vertx.blueprint.todolist.entity.Todo;
import io.vertx.core.json.Json;

/**
 * Vert.x Blueprint Application - Todo Backend
 * Utils Class
 *
 * @author Eric Zhao
 */
public class Utils {

  private Utils() {}

  /**
   * Decode Todo entity from JSON str
   *
   * @param jsonStr JSON str
   * @return Todo entity
   */
  public static Todo getTodoFromJson(String jsonStr) {
    if (jsonStr == null)
      return null;
    else
      return Json.decodeValue(jsonStr, Todo.class);
  }

}
