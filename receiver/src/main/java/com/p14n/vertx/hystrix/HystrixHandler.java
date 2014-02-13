package com.p14n.vertx.hystrix;

import org.vertx.java.core.eventbus.Message;
import rx.Observable;

/**
 * Created by Dean Pehrsson-Chapman
 * Date: 12/02/2014
 */
public interface HystrixHandler<Req,Res> {
  Observable<Res> handleHystrix(Message<Req> msg);
}
