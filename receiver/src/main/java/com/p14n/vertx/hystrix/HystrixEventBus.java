package com.p14n.vertx.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixNonBlockingCommand;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import rx.Observable;
import rx.util.functions.Action1;

/**
 * Created by Dean Pehrsson-Chapman
 * Date: 12/02/2014
 */
public class HystrixEventBus {

  EventBus bus;
  HystrixCommandGroupKey group;

  public HystrixEventBus(String hystrixGroup, EventBus bus) {
    this.bus = bus;
    this.group = HystrixCommandGroupKey.Factory.asKey(hystrixGroup);
  }

  public <Request, Response> HystrixEventBus registerHandler(String address, HystrixHandler<Request,
          Response> handler,
                                                             Handler<AsyncResult<Void>> resultHandler) {
    HystrixNonBlockingCommand.Setter s = HystrixNonBlockingCommand.Setter.withGroupKey(group).andCommandKey
            (HystrixCommandKey.Factory.asKey(address));
    bus.registerHandler(address, createCommandHandler(s, handler), resultHandler);
    return this;
  }

  public <Request, Response> HystrixEventBus registerHandler(String address, HystrixBlockingHandler<Request,
          Response> handler,
                                                             Handler<AsyncResult<Void>> resultHandler) {
    HystrixCommand.Setter s = HystrixCommand.Setter.withGroupKey(group).andCommandKey
            (HystrixCommandKey.Factory.asKey(address));
    Handler<? extends Message> commandHandler = createCommandHandler(s, handler);
    bus.registerHandler(address, commandHandler, resultHandler);
    return this;
  }

  private <Request, Response> Handler<? extends Message> createCommandHandler(final HystrixNonBlockingCommand.Setter s,
                                                                              final HystrixHandler<Request,
                                                                                      Response> handler) {
    return new Handler<Message<Request>>() {
      @Override
      public void handle(final Message<Request> message) {

        Observable<Response> o = new HystrixNonBlockingCommand<Response>(s) {
          @Override
          protected Observable<Response> run() throws Exception {
            return handler.handleHystrix(message);
          }
        }.toObservable();

        o.subscribe(new Action1<Response>() {
          @Override
          public void call(Response res) {
            message.reply(res);
          }
        });
      }
    };
  }

  private <Request, Response> Handler<? extends Message> createCommandHandler(final HystrixCommand.Setter s,
                                                                              final HystrixBlockingHandler<Request,
                                                                                      Response> handler) {

    return new Handler<Message<Request>>() {
      @Override
      public void handle(final Message<Request> message) {

        Observable<Response> o = new HystrixCommand<Response>(s) {
          @Override
          protected Response run() throws Exception {
            return handler.handleHystrix(message);
          }
        }.toObservable();

        o.subscribe(new Action1<Response>() {
          @Override
          public void call(Response res) {
            message.reply(res);
          }
        });
      }
    };
  }

}
