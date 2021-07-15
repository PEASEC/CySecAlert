package de.tudarmstadt.peasec.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import de.tudarmstadt.peasec.entity.TweetEntity;
import de.tudarmstadt.peasec.entity.UserEntity;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.util.Statistics;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;
import org.bson.Document;
import twitter4j.Status;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;

public class TweetMongoService {

    private MongoCollection<TweetEntity> collection;

    public TweetMongoService(String mongoCollectionName) { this.setCollection(mongoCollectionName); }

    public TweetMongoService(Properties properties) {
        String collectionName = properties.getProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME);
        this.collection = MongoHelper.getInstance().getCollection(collectionName, TweetEntity.class);
    }

    public void setCollection(String s) {
        this.collection = MongoHelper.getInstance().getCollection(s, TweetEntity.class);
    }

    public void saveStatusList(List<Status> statusList) {
        this.saveTweetList(statusList.stream().map(TweetEntity::new).collect(Collectors.toList()));
    }

    public void saveTweetList(List<TweetEntity> tweetList) {
        System.out.println("Saving...");
        this.collection.insertMany(tweetList);
        System.out.println("Statusses saved");
    }

    public void saveTweetEntity(TweetEntity t){
        if(this.getTweetEntityByTweetId(t.getTweetId()) != null) {
            Statistics.getInstance().addTo("WriteTweetFail-Duplicate");
            return;
        }
        this.collection.insertOne(t);
        Statistics.getInstance().addTo("WriteTweetSuccess");

    }

    public TweetEntity getTweetEntityByTweetId(long id) {
        return this.collection.find(eq("tweetId", id)).first();
    }

    public long getTweetCountByUser(UserEntity u) {
        long userId = u.getUserId();
        long count = this.collection.countDocuments(eq("userId", u.getUserId()));
        return count;
    }

    public TweetEntity getOldestTweetByUser(UserEntity u) {
        long userId = u.getUserId();
        TweetEntity out = null;

        List<TweetEntity> list = new ArrayList<>();
        this.collection.aggregate(Arrays.asList(
                match(Filters.eq("userId", userId)),
                sort(Sorts.ascending("tweetId")),
                limit(1)
        )).into(list);

        if(list.size() > 0) {
            out = list.get(0);
            System.out.println(u.getScreenName() + ": " + out.getCreatedAt().toString());
        }
        else
            System.out.println(u.getScreenName() + ": No Tweet!");
        return out;
    }

    public TweetEntity getLatestTweetByUser(UserEntity u) {
        long userId = u.getUserId();
        List<TweetEntity> list = new ArrayList<>();
        this.collection.aggregate(Arrays.asList(
                match(Filters.eq("userId", userId)),
                sort(Sorts.descending("tweetId")),
                limit(1)
        )).into(list);
        System.out.println(u.getScreenName() + ": " + list.get(0).getCreatedAt().toString());
        return list.get(0);
    }

    public long getTweetCountNewerThan(Date d) {
        return this.collection.countDocuments(Filters.gte("createdAt", d));
    }

    public List<TweetEntity> getTweetsNewerThan(Date d) {
        return this.collection.find(Filters.gte("createdAt", d)).into(new ArrayList<>());
    }

    public long getTweetCountOlderThan(Date d) {
        return this.collection.countDocuments(Filters.lte("createdAt", d));
    }

    public List<TweetEntity> getTweetsOlderThan(Date d) {
        return this.collection.find(Filters.lte("createdAt", d)).into(new ArrayList<>());
    }

    public long getTweetCountBetweenDates(Date lowerBoundary, Date upperBoundary) {
        return (int) this.collection.aggregate(Arrays.asList(
                match(Filters.gte("createdAt", lowerBoundary)),
                match(Filters.lte("createdAt", upperBoundary)),
                count()
        ), Document.class).first().get("count");
    }

    public List<TweetEntity> getTweetsBetweenDates(Date lowerBoundary, Date upperBoundary) {
        return this.collection.aggregate(Arrays.asList(
                match(Filters.gte("createdAt", lowerBoundary)),
                match(Filters.lte("createdAt", upperBoundary))
        )).into(new ArrayList<>());
    }

    public List<TweetEntity> getRandomTweets(int n) {
        return this.collection.aggregate(Arrays.asList(sample(n))).into(new ArrayList<>());
    }

    public long getTweetCount() {
        return this.collection.countDocuments();
    }

    public List<TweetEntity> getTweets() {
        return this.collection.find().into(new ArrayList<>());
    }

    public void dropCollection() {
        this.collection.drop();
    }

    public void deleteTweet(TweetEntity t) {
        this.collection.findOneAndDelete(Filters.eq("tweetId", t.getTweetId()));
    }
}
