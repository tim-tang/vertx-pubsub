/**
 * 
 */
package me.timtang.broadcaster;

import me.timtang.utils.VertxFeedConstant;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;


/**
 * Broadcast messages to persistor and followers.  
 * 
 * @author tim.tang
 *
 */
public class VertxFeedBroadcaster extends BusModBase {
    
    private String address;
    private String persistorAddress;
    
    /**
     *  (non-Javadoc)
     *  
     * @see org.vertx.java.busmods.BusModBase#start()
     */
    public void start() {
        super.start();
        
        this.address = getOptionalStringConfig("address", VertxFeedConstant.ADDRESS_VERTX_BROADCASTER);
        this.persistorAddress = getOptionalStringConfig("persistor_address", VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR);
        Handler<Message<JsonObject>> messageHandler = new Handler<Message<JsonObject>>(){
            @Override
            public void handle(final Message<JsonObject> message) {
                // do benchmark.
                // doBenchMark(message);
                // persist message into mongodb.
                doPersistMessage(message);
            }
        };
        eb.registerHandler(address, messageHandler);
    }
    
    /**
     * Mock data for benchmark.
     * 
     * @param message
     */
    @SuppressWarnings("unused")
    private void doBenchMark(Message<JsonObject> message){
        message.body =  new JsonObject().putString("action", "save").putString("collection", "messages").putObject("document", new JsonObject().putString("user", "tim").putString("text", "ddddd"));
    }
    
    /**
     * Persist message into mongdb.
     * 
     * @param message
     */
    private void doPersistMessage(final Message<JsonObject> message) {
        eb.send(persistorAddress, message.body, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                if (reply.body.getString("status").equals(VertxFeedConstant.STATUS_OK)) {
                    // broadcast to all followers.
                    JsonObject doc = getMandatoryObject("document", message);
                    broadcastToFollowers(reply, doc);
                }else{
                    logger.error("Failed to persist message: " + reply.body.getString("message"));
                    sendError(message, "Failed to excecute save message.");
                }
            }
        });
    }
    
    /**
     * Broadcast message to followers.
     * 
     * @param reply
     * @param doc
     */
    private void broadcastToFollowers(Message<JsonObject> reply, final JsonObject doc) {
        JsonObject findFollower = new JsonObject().putString("action", "find").putString("collection", VertxFeedConstant.COLLECTION_USERRELATIONS);
        JsonObject matcher = new JsonObject().putString("user", doc.getString("user"));
        findFollower.putObject("matcher", matcher);
        eb.send(persistorAddress, findFollower, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                if (reply.body.getString("status").equals(VertxFeedConstant.STATUS_OK)) {
                    JsonArray results = reply.body.getArray("results");
                    // publish to self.
                    eb.publish(doc.getString("user"), doc);
                    // publish to followers.
                    for(Object userRelation : results){
                        String follower = ((JsonObject) userRelation).getString("follower");
                        eb.publish(follower, doc);
                    }
                }else{
                    logger.error("Failed to execute broadcast to followers.");
                }
            }
        });
    }
}
