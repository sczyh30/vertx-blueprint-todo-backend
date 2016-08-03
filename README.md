# Vert.x Blueprint - Todo Backend

[![Travis Build Status](https://travis-ci.org/sczyh30/vertx-blueprint-todo-backend.svg?branch=master)](https://travis-ci.org/sczyh30/vertx-blueprint-todo-backend)

Vert.x blueprint application - A todo-backend implementation using Vert.x
and various persistence(e.g. Redis or MySQL).

## Introduction

This repository is an introduction to basic Vert.x web RESTful service development.
Detailed tutorials (both in Chinese and English) are provided below.

## Detailed Document

- [English Version](http://sczyh30.github.io/vertx-blueprint-todo-backend/)
- [中文文档](http://sczyh30.github.io/vertx-blueprint-todo-backend/cn/)

## Want to improve this blueprint ?

Forks and PRs are definitely welcome !

## Build

To build the code:

    gradle build

Run service locally:

- with Redis: `java -jar build/libs/vertx-blueprint-todo-backend-fat.jar -conf config/config.json`
- with MySQL: `java -jar build/libs/vertx-blueprint-todo-backend-fat.jar -conf config/config_jdbc.json`

Run with Docker Compose:

    docker-compose up --build

## OpenShift

- [OpenShift Live Demo URL](http://verttodo-sczyh30.rhcloud.com/todos)
- [API Test](http://www.todobackend.com/specs/index.html?http://verttodo-sczyh30.rhcloud.com/todos)
- [Client Test](http://www.todobackend.com/client/index.html?http://verttodo-sczyh30.rhcloud.com/todos)


## API Test

Test result of the RESTful API (using [todo-backend-js-spec](https://github.com/TodoBackend/todo-backend-js-spec)):

![RESTful API Test Result](docs/img/vert-api-test.png)
