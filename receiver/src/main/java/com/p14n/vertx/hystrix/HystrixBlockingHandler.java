package com.p14n.vertx.hystrix;

import org.vertx.java.core.eventbus.Message;

/**
 * Created by Dean Pehrsson-Chapman
 * Date: 12/02/2014
 */
public interface HystrixBlockingHandler<Req,Res> {
  Res handleHystrix(Message<Req> event);
}
