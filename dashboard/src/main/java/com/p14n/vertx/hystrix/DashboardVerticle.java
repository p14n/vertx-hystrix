package com.p14n.vertx.hystrix;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Created by Dean Pehrsson-Chapman
 * Date: 14/02/2014
 */
public class DashboardVerticle extends Verticle {

  private <T> T cfg(String name, T defaultValue) {
    JsonObject config = getContainer().config();
    if (config != null && config.containsField(name))
      return config.getValue(name);
    return defaultValue;
  }

  @Override
  public void start() {

    int port = cfg("port", 8080);
    final String delay = cfg("delay", "5000");
    HttpServer server = vertx.createHttpServer();

    server.requestHandler(new Handler<HttpServerRequest>() {

      @Override
      public void handle(final HttpServerRequest request) {

        if ("/stream".equals(request.path())) {
          stream(request, delay);
        } else if ("/".equals(request.path())) {
          request.response().sendFile("./index.html");
        } else {
          request.response().sendFile("." + request.path());
        }
      }
    }).listen(port);

  }

  private void stream(HttpServerRequest request, String delay) {

    final HttpServerResponse response = request.response();

    response.setChunked(true);

    response.headers().add("Content-Type", "text/event-stream;charset=UTF-8");
    response.headers().add("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
    response.headers().add("Pragma", "no-cache");

    vertx.eventBus().publish("hystrix-dashboard-admin", delay);

    vertx.eventBus().registerHandler("hystrix-dashboard-data", new Handler<Message<String>>() {
      @Override
      public void handle(Message<String> event) {
        try {

          String json = event.body();
          response.write("data: " + json + "\n\n");

        } catch (Exception e) {
          e.printStackTrace();
          vertx.eventBus().publish("hystrix-dashboard-admin", "0");
        }
      }
    });
  }
}
