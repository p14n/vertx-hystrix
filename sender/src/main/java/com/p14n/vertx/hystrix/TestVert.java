package com.p14n.vertx.hystrix;

import com.netflix.hystrix.contrib.yammermetricspublisher.HystrixYammerMetricsPublisher;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.reporting.JmxReporter;
import io.vertx.rxcore.java.eventbus.RxEventBus;
import io.vertx.rxcore.java.eventbus.RxMessage;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;
import rx.util.functions.Action1;

/**
 * Created by Dean Pehrsson-Chapman
 * Date: 17/09/2013
 */
public class TestVert extends Verticle {

  @Override
  public void start() {

    final HystrixEventBus bus = new HystrixEventBus("sender", new RxEventBus(vertx.eventBus()));
    final MetricsRegistry metrics = new MetricsRegistry();
    HystrixPlugins.getInstance().registerMetricsPublisher(new HystrixYammerMetricsPublisher(metrics));
    JmxReporter.startDefault(metrics);

    //vertx.setPeriodic(100, createSender());

    vertx.setPeriodic(100, createHystrixSender(bus));
    super.start();
  }

  private Handler<Long> createSender() {
    final EventBus bus = vertx.eventBus();

    return new Handler<Long>() {
      @Override
      public void handle(Long event) {
        System.out.println("Sending");
        bus.sendWithTimeout("blocker", "Hello blocker", 100l, new Handler<AsyncResult<Message<String>>>() {
          @Override
          public void handle(AsyncResult<Message<String>> event) {
            System.out.println("Received from blocker " + event.result().body());
          }
        });
        bus.sendWithTimeout("non-blocker", "Hello non-blocker", 100l, new Handler<AsyncResult<Message<String>>>() {
          @Override
          public void handle(AsyncResult<Message<String>> event) {
            System.out.println("Received from non-blocker " + event.result().body());          }
        });
      }
    };
  }

  private Handler<Long> createHystrixSender(final HystrixEventBus bus) {
    return new Handler<Long>() {
      @Override
      public void handle(Long event) {
        System.out.println("Sending");
        bus.sendWithTimeout("blocker", "Hello blocker", 100).subscribe(new Action1<RxMessage<Object>>() {
          @Override
          public void call(RxMessage<Object> objectRxMessage) {
            System.out.println("Received from blocker " + objectRxMessage.body());
          }
        });
        bus.sendWithTimeout("non-blocker", "Hello non-blocker", 100).subscribe(new Action1<RxMessage<Object>>() {
          @Override
          public void call(RxMessage<Object> objectRxMessage) {
            System.out.println("Received from non-blocker " + objectRxMessage.body());
          }
        });
      }
    };
  }


}
