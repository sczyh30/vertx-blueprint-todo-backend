FROM java:8-jre

ENV VERTICLE_FILE build/libs/vertx-blueprint-todo-backend-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8082

COPY $VERTICLE_FILE $VERTICLE_HOME/
# COPY config.json $VERTICLE_HOME/

WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["java -jar vertx-blueprint-todo-backend-fat.jar"]