package de.tudarmstadt.peasec.service;

import com.mongodb.client.MongoCollection;
import de.tudarmstadt.peasec.entity.UserEntity;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.util.Statistics;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;
import twitter4j.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class UserMongoService {

    private MongoCollection<UserEntity> collection;

    public UserMongoService(Properties properties) {
        String collectionName = properties.getProperty(CollectionNameProperties.USER_ENTITY_COLLECTION_NAME);
        this.collection = MongoHelper.getInstance().getCollection(collectionName, UserEntity.class);
    }

    public void saveUserEntityList(List<UserEntity> userList) {
        userList.forEach(this::saveUserEntity);
    }

    public void saveUserList(List<User> userList) {
        this.saveUserEntityList(userList.stream().map(UserEntity::new).collect(Collectors.toList()));
    }

    public void saveUserEntity(UserEntity e) {
        if(this.getUserEntityByUserId(e.getUserId()) != null) {
            Statistics.getInstance().addTo("WriteUserFail-Duplicate");
            return;
        }
        this.collection.insertOne(e);
        Statistics.getInstance().addTo("WriteUserSuccess");
    }

    public UserEntity getUserEntityByUserId(long id) {
        return this.collection.find(eq("userId", id)).first();
    }

    public UserEntity getUserByScreenName(String name) {
        //delete leading @
        String trimmedName = name;
        if(trimmedName.length() > 0 && trimmedName.charAt(0) == '@')
            trimmedName = trimmedName.substring(1);

        UserEntity e = this.collection.find(eq("screenName", trimmedName)).first();
        return e;
    }

    public void saveUser(User u) {
        this.saveUserEntity(new UserEntity(u));
    }

    public List<UserEntity> getUserEntityList() {
        return this.collection.find().into(new ArrayList<>());
    }

    public long countUsers() {
        return this.collection.countDocuments();
    }
}
