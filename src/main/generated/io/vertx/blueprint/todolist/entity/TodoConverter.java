/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.blueprint.todolist.entity;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.todolist.entity.Todo}.
 * <p>
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.todolist.entity.Todo} original class using Vert.x codegen.
 */
public class TodoConverter {

  public static void fromJson(JsonObject json, Todo obj) {
    if (json.getValue("completed") instanceof Boolean) {
      obj.setCompleted((Boolean) json.getValue("completed"));
    }
    if (json.getValue("id") instanceof Number) {
      obj.setId(((Number) json.getValue("id")).intValue());
    }
    if (json.getValue("order") instanceof Number) {
      obj.setOrder(((Number) json.getValue("order")).intValue());
    }
    if (json.getValue("title") instanceof String) {
      obj.setTitle((String) json.getValue("title"));
    }
    if (json.getValue("url") instanceof String) {
      obj.setUrl((String) json.getValue("url"));
    }
  }

  public static void toJson(Todo obj, JsonObject json) {
    if (obj.isCompleted() != null) {
      json.put("completed", obj.isCompleted());
    }
    json.put("id", obj.getId());
    if (obj.getOrder() != null) {
      json.put("order", obj.getOrder());
    }
    if (obj.getTitle() != null) {
      json.put("title", obj.getTitle());
    }
    if (obj.getUrl() != null) {
      json.put("url", obj.getUrl());
    }
  }
}