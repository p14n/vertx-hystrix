package com.p14n.vertx.hystrix;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;

/**
 * Created by Dean Pehrsson-Chapman
 * Date: 14/02/2014
 */
public class HystrixMetricsSender {

  volatile HystrixMetricsPoller poller = null;

  public HystrixMetricsSender(final EventBus bus) {
    bus.registerHandler("hystrix-dashboard-admin",new Handler<Message<String>>() {
      @Override
      public void handle(Message<String> event) {
        stopPoller();
        try {
          int delay = Integer.parseInt(event.body());
          if(delay>0){
            poller = new HystrixMetricsPoller(new HystrixMetricsPoller.MetricsAsJsonPollerListener() {
              @Override
              public void handleJsonMetric(String json) {
                bus.send("hystrix-dashboard-data",json);
              }
            },delay);
            poller.start();
          }
        } catch (NumberFormatException e){}
      }
    });
  }

  private void stopPoller() {
    if(poller!=null){
      if(poller.isRunning()){
        poller.shutdown();
      }
      poller = null;
    }
  }
}
