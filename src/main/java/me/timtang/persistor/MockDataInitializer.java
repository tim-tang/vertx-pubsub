package me.timtang.persistor;

import java.util.HashMap;
import java.util.Map;

import me.timtang.utils.VertxFeedConstant;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.deploy.Verticle;


/**
 * Mock data initializer.
 * 
 * @author tim.tang
 * 
 */
public class MockDataInitializer extends Verticle {
    
	/**
	 *  (non-Javadoc)
	 *  
	 * @see org.vertx.java.deploy.Verticle#start()
	 */
	public void start() {
		vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildDrop(VertxFeedConstant.COLLECTION_USERS)));
	    vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildDrop(VertxFeedConstant.COLLECTION_USERRELATIONS)));
	    vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildDrop(VertxFeedConstant.COLLECTION_MESSAGES)));
		vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildUser("tim")));
		vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildUser("andy")));
		for(int i=0; i<1000; i++){
		    vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildUser("user"+i)));
		    vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildUserRelation("tim","user"+i)));
		}
		vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildUserRelation("tim","andy")));
		vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildUserRelation("andy","tim")));
		
		
    }
	
	private Map<String, Object> buildUserRelation(String user, String follower){
	    Map<String, Object> userRelation = new HashMap<>();
	    userRelation.put("action", "save");
	    userRelation.put("collection", "userrelations");
	    Map<String, Object> userRelationDocument = new HashMap<>();
	    userRelationDocument.put("user",  user);
	    userRelationDocument.put("follower", follower);
	    userRelation.put("document", userRelationDocument);
	    return userRelation;
	}

	
    /**
     * Persist user id into user relation collection.
     * 
     * @param user
     * @param follower
     */
    @SuppressWarnings("unused")
    private void saveUserRelation(String user, String follower) {
        //fetch users by name.
	    JsonObject findUser = new JsonObject().putString("action", "find").putString("collection", VertxFeedConstant.COLLECTION_USERS);
	    JsonArray queryArray = (new JsonArray()).addObject(new JsonObject().putString("username", user));
	    queryArray.addObject(new JsonObject().putString("username", follower));
	    JsonObject matcher = (new JsonObject()).putArray("$or", queryArray);
	    findUser.putObject("matcher", matcher);
	    vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR, findUser, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                if (reply.body.getString("status").equals(VertxFeedConstant.STATUS_OK)) {
                    JsonArray results = reply.body.getArray("results");
                    if(results != null && results.size() > 1){
                        String[] ids = new String[results.size()];
                        for(int i = 0; i < results.size(); i++){
                           ids[i] = ((JsonObject) results.get(i)).getString("_id");
                        }
                        vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildUserRelation(ids[0], ids[1])));
                        vertx.eventBus().send(VertxFeedConstant.ADDRESS_VERTX_MONGOPERSISTOR,new JsonObject(buildUserRelation(ids[1], ids[0])));
                    }
                }
            }
        });
    }

    /**
     * Build mock data for user.
     * 
     * @param user
     * @return map {@link Map<String, Object>}
     */
    private Map<String,Object> buildUser(String user) {
        Map<String, Object> saveOps = new HashMap<>();
		saveOps.put("action", "save");
		saveOps.put("collection", "users");
		Map<String, Object> userDocument = new HashMap<>();
		userDocument.put("username", user);
		userDocument.put("password", "123");
		saveOps.put("document", userDocument);
		return saveOps;
    }

    /**
     * Build mock data for drop collections.
     * 
     * @param collection
     * @return map {@link Map<String, Object>}
     */
    private Map<String,Object> buildDrop(String collection) {
        Map<String,Object> deleteOps = new HashMap<>();
		deleteOps.put("action", "delete");
		deleteOps.put("collection", collection);
		deleteOps.put("matcher", new HashMap<>());
        return deleteOps;
    }
} 
