import com.p14n.vertx.hystrix.DashboardVerticle;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.testtools.TestVerticle;
import static org.vertx.testtools.VertxAssert.*;

/**
 * Created by Dean Pehrsson-Chapman
 * Date: 17/02/2014
 */
public class DashTestVerticle extends TestVerticle {

  @Test
  public void testGettingStream(){

    vertx.eventBus().registerHandler("hystrix-dashboard-admin",new Handler<Message>() {
      @Override
      public void handle(Message event) {
        vertx.eventBus().send("hystrix-dashboard-data","done");
      }
    });

    container.deployVerticle(DashboardVerticle.class.getName(),new Handler<AsyncResult<String>>() {
      @Override
      public void handle(AsyncResult<String> event) {

        vertx.createHttpClient().setPort(8080).getNow("/stream",new Handler<HttpClientResponse>() {
          @Override
          public void handle(HttpClientResponse resp) {

            assertEquals(200, resp.statusCode());

            resp.dataHandler(new Handler<Buffer>() {
              @Override
              public void handle(Buffer event) {
                assertEquals("data: done\n\n", event.toString());
                testComplete();
              }
            });
          }
        });
      }
    });
  }
}
