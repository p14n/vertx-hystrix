package com.p14n.vertx.hystrix;

import com.netflix.hystrix.contrib.yammermetricspublisher.HystrixYammerMetricsPublisher;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.reporting.JmxReporter;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;
import rx.Observable;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Dean Pehrsson-Chapman
 * Date: 17/09/2013
 */
public class TestVert extends Verticle {
  @Override
  public void start() {

    HystrixEventBus bus = new HystrixEventBus("receiver", vertx.eventBus());
    HystrixMetricsSender sender = new HystrixMetricsSender(vertx.eventBus());

    final MetricsRegistry metrics = new MetricsRegistry();
    HystrixPlugins.getInstance().registerMetricsPublisher(new HystrixYammerMetricsPublisher(metrics));
    JmxReporter.startDefault(metrics);

    final CountDownLatch latch = new CountDownLatch(2);
    Handler<AsyncResult<Void>> resultHandler = new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> event) {
        latch.countDown();
        System.out.print("Registered handler: " + latch.getCount());
      }
    };

    System.out.print("Registering handlers");
    bus.registerHandler("blocker", new HystrixBlockingHandler<String, String>() {
      @Override
      public String handleHystrix(Message<String> event) {
        return event.body() + " back";
      }
    }, resultHandler);

    bus.registerHandler("non-blocker", new HystrixHandler<String, String>() {
      @Override
      public Observable<String> handleHystrix(Message<String> msg) {
        System.out.println("hystrix " + msg.body());
        return Observable.just(msg.body() + " back");
      }
    }, resultHandler);

    try {
      latch.await();
      System.out.print("Handlers all registered");
    } catch (InterruptedException e) {
      throw new RuntimeException("", e);
    }
    super.start();
  }


}
