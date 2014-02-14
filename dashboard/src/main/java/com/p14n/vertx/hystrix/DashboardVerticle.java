package com.p14n.vertx.hystrix;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.impl.MimeMapping;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

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
          file(request, "./index.html");
        } else {
          file(request, "." + request.path());
        }
      }
    }).listen(port);

  }

  private byte[] getFile(String path) {
    InputStream i = getClass().getClassLoader().getResourceAsStream(path);
    try {
      ByteArrayOutputStream o = new ByteArrayOutputStream();
      while (i.available() > 0) {
        o.write(i.read());
      }
      return o.toByteArray();
    } catch (IOException e) {
      return null;
    }
  }

  private void file(HttpServerRequest request, String filename) {
    byte[] file = getFile(filename);
    if (file != null) {
      HttpServerResponse response = request.response();
      response.putHeader(org.vertx.java.core.http.HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length));
      int li = filename.lastIndexOf('.');
      if (li != -1 && li != filename.length() - 1) {
        String ext = filename.substring(li + 1, filename.length());
        String contentType = MimeMapping.getMimeTypeForExtension(ext);
        if (contentType != null) {
          response.putHeader(org.vertx.java.core.http.HttpHeaders.CONTENT_TYPE, contentType);
        }
      }
      request.response().write(new Buffer(file));
      request.response().end();
      request.response().close();
    } else {
      sendError(request, 404, filename + " not found");
    }
  }

  private void sendError(HttpServerRequest req, int error, String message) {
    req.response().setStatusMessage(message);
    req.response().setStatusCode(error);
    req.response().end();
  }

  private void stream(HttpServerRequest request, String delay) {

    final HttpServerResponse response = request.response();

    response.headers().add("Content-Type", "text/event-stream;charset=UTF-8");
    response.headers().add("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
    response.headers().add("Pragma", "no-cache");

    response.setChunked(true);

    final AtomicLong timerId = new AtomicLong(-1);

    vertx.eventBus().publish("hystrix-dashboard-admin", delay);

    vertx.eventBus().registerHandler("hystrix-dashboard-data", new Handler<Message<String>>() {
      @Override
      public void handle(Message<String> event) {
        try {
          String json = event.body();
          response.write(json+"\n");
        } catch (Exception e){
          e.printStackTrace();
          vertx.eventBus().publish("hystrix-dashboard-admin", "0");
        }
      }
    });
  }
}
