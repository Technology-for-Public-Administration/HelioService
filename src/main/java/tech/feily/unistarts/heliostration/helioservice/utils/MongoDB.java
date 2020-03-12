package tech.feily.unistarts.heliostration.helioservice.utils;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public final class MongoDB {

    
    private MongoDB() {
        
    }
    
    private static class Holder {
        private static MongoClient mongoClient = new MongoClient("localhost", 27017);
        private static MongoDatabase mongoDatabase = mongoClient.getDatabase("helio"); 
    }
    
    public static MongoDatabase getInstance() {
        return Holder.mongoDatabase;
    }
    
}
