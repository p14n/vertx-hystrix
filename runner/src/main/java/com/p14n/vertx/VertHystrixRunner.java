package com.p14n.vertx;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PropertyConfigurator;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Dean Pehrsson-Chapman
 * Date: 13/09/2013
 */
public class VertHystrixRunner {

  private static void setupLogging() {
    Properties p = new Properties();
    p.setProperty("log4j.rootLogger", "TRACE,console");
    p.setProperty("log4j.appender.console", ConsoleAppender.class.getName());
    p.setProperty("log4j.appender.console.Target", "System.out");
    p.setProperty("log4j.appender.console.layout", "org.apache.log4j.PatternLayout");
    PropertyConfigurator.configure(p);
    System.setProperty(
            "org.vertx.logger-delegate-factory-class-name",
            "org.vertx.java.core.logging.impl.Log4jLogDelegateFactory"
    );
  }

  public static void main(String args[]) throws BrokenBarrierException, InterruptedException {

    setupLogging();
    PlatformManager pm = PlatformLocator.factory.createPlatformManager();
    deployAll(pm,"receiver","sender");
    Thread.currentThread().join();

  }

  public static PlatformManager deployAll(PlatformManager pm, String... names) {
    CountDownLatch latch = new CountDownLatch(names.length);
    int index = 1;
    for (String name : names) {
      File f = new File("../"+name);
      System.out.println(f.getAbsolutePath());
      if (f.exists()) {
        pm.deployModuleFromClasspath("hystrix~" + name + "~1",null, 1, getGradleClasspath("../"+name),
                createResultHandler(name, latch));
      } else {
        pm.deployModule(name, null, 1, createResultHandler(name, latch));
      }
    }
    try {
      System.out.println("Waiting for deployment to complete");
      latch.await();
      System.out.println("Deployment complete");
    } catch (InterruptedException e) {
      throw new RuntimeException("", e);
    }
    return pm;
  }

  private static AsyncResultHandler<String> createResultHandler(final String name, final CountDownLatch latch) {
    return new AsyncResultHandler<String>() {
      public void handle(AsyncResult<String> asyncResult) {
        if (asyncResult.succeeded()) {
          System.out.println(name + " deployment ID is " + asyncResult.result());
        } else {
          System.out.println(name + "deployment failed, ID is " + asyncResult.result());
          asyncResult.cause().printStackTrace();
        }
        try {
          latch.countDown();
        } catch (Exception e) {
        }
      }
    };
  }

  private static URL[] getGradleClasspath(String root) {
    try {
      File libDir = new File(root + "/build/output/lib/");
      File[] jars = libDir.listFiles();
      URL[] urls = null;
      System.out.println("Add jars");
      if (jars != null) {
        urls = new URL[jars.length + 2];
        int index = 2;
        for (File jar : jars) {
          System.out.println(" jar: "+jar.getAbsolutePath());
          urls[index++] = jar.toURI().toURL();
        }
      } else {
        urls = new URL[2];
      }
      urls[0] = new URL("file:" + root + "/build/classes/main/");
      urls[1] = new URL("file:" + root + "/build/resources/main/");
      return urls;
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
