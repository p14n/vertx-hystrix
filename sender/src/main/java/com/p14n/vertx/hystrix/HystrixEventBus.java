package com.p14n.vertx.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixNonBlockingCommand;
import io.vertx.rxcore.java.eventbus.RxEventBus;
import io.vertx.rxcore.java.eventbus.RxMessage;
import rx.Observable;

/**
 * Created by Dean Pehrsson-Chapman
 * Date: 12/02/2014
 */
public class HystrixEventBus {
  RxEventBus bus;
  HystrixCommandGroupKey group;

  public HystrixEventBus(String hystrixGroup, RxEventBus bus) {
    this.bus = bus;
    this.group = HystrixCommandGroupKey.Factory.asKey(hystrixGroup);
  }

  public <S, R> Observable<RxMessage<R>> sendWithTimeout(final String address, final S msg, final long timeout) {
    HystrixNonBlockingCommand.Setter s = HystrixNonBlockingCommand.Setter.withGroupKey(group).andCommandKey
            (HystrixCommandKey.Factory.asKey("send-"+address));
    return new HystrixNonBlockingCommand<RxMessage<R>>(s) {
      @Override
      protected Observable<RxMessage<R>> run() throws Exception {
        return bus.sendWithTimeout(address, msg, timeout);
      }
    }.toObservable();
  }

}
