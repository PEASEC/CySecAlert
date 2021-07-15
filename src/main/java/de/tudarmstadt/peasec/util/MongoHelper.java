package de.tudarmstadt.peasec.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoHelper {

    private static final MongoHelper instance = new MongoHelper();

    //TODO: Enter the connection string and the name of mongo database here
    public static final String connectionString = "mongodb://localhost:27017";
    public static final String dbName = "twitterData";

    private MongoDatabase db;

    private MongoHelper() {
        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                pojoCodecRegistry);
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .build();

        MongoClient client = MongoClients.create(clientSettings);
        this.db = client.getDatabase(dbName);
    }

    public <T> MongoCollection<T> getCollection(String collectionName, Class<T> clazz) {
        MongoCollection<T> mongoCollection = this.db.getCollection(collectionName, clazz);
        return mongoCollection;
    }

    public static MongoHelper getInstance() {
        return instance;
    }

    public static MongoDatabase getDatabase() {return instance.db; }
}
