FROM java:8-jre

RUN wget http://download.redis.io/releases/redis-3.0.7.tar.gz \
        && tar xzf redis-3.0.7.tar.gz \
        && rm redis-3.0.7.tar.gz \
        && cd redis-3.0.7 \
        && apt-get update -y \
        && apt-get install make gcc -y \
        && make \
        && make install

ENV VERTICLE_FILE build/libs/vertx-blueprint-todo-backend-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8082

COPY $VERTICLE_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["redis-server & java -jar vertx-blueprint-todo-backend-fat.jar"]