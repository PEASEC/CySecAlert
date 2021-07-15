package de.tudarmstadt.peasec.service;

import com.mongodb.client.MongoDatabase;
import de.tudarmstadt.peasec.util.MongoHelper;

import java.util.ArrayList;

public class DatabaseService {

    private static final boolean isCollectionDropEnabled = true;

    private MongoDatabase db = MongoHelper.getInstance().getDatabase();

    public void printCollectionList() {
        this.db.listCollectionNames().into(new ArrayList<>()).forEach(System.out::println);
        System.out.println();
    }

    public void dropCollection(String collectionName) {
        if(!isCollectionDropEnabled) {
            System.err.println("Collection Drop is disabled.");
            return;
        }
        this.db.getCollection(collectionName).drop();

    }

}
