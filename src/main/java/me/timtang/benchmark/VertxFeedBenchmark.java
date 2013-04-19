package me.timtang.benchmark;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import me.timtang.utils.VertxFeedConstant;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;


/**
 * VertxFeed benchmark.
 * 
 * @author tim.tang
 */
public class VertxFeedBenchmark extends BusModBase{

    public void start(){
        super.start();
        
        // create a handler to populate mock data when the persistor is loaded
        Handler<String> mockDataHandler = new Handler<String>() {
            public void handle(String message) {
                container.deployVerticle("me.timtang.persistor.MockDataInitializer");
            }
        };
        // deploy mongodb persistor module and pass in the handler.
        container.deployModule("vertx.mongo-persistor-v1.2", null, 1, mockDataHandler);
        // deploy feed broadcaster verticle.
        container.deployVerticle("me.timtang.broadcaster.VertxFeedBroadcaster");
        
        // save message then broadcast to all followers.
        Handler<HttpServerRequest> broadcastHandler = new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest request) {
                final long start = System.currentTimeMillis();
                eb.send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR, new JsonObject(buildMessage("tim")), new Handler<Message<JsonObject>>(){
                    @Override
                    public void handle(Message<JsonObject> reply) {
                        long end =  System.currentTimeMillis();
                        if(reply != null && reply.body.getString("status").equals(VertxFeedConstant.STATUS_OK)){
                            request.response.putHeader("Content-Type", "application/json");
                            request.response.putHeader("Date", new Date(end));
                            request.response.putHeader("Connection", "close");
                            request.response.putHeader("X-Response-Time", end-start);
                            request.response.end(reply.toString());
                            request.response.close();
                        }
                    }
                });
            }
        };
        RouteMatcher rm = new RouteMatcher();
        
        
        rm.get("/post", broadcastHandler);
        vertx.createHttpServer().requestHandler(rm).listen(8080);
    }
    
    private Map<String, Object> buildMessage(String user){
        Map<String, Object> message = new HashMap<>();
        message.put("action", "save");
        message.put("collection", "messages");
        Map<String, Object> messageDocument = new HashMap<>();
        messageDocument.put("user", user);
        messageDocument.put("text", "This is my first twitter.......");
        message.put("document", messageDocument);
        return message;
    }
}
