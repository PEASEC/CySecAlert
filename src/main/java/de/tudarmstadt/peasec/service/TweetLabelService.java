package de.tudarmstadt.peasec.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.tudarmstadt.peasec.entity.TweetEntity;
import de.tudarmstadt.peasec.entity.TweetLabelEntity;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.depr.NaiveBayes;
import de.tudarmstadt.peasec.pipeline.TextPreprocessor;
import de.tudarmstadt.peasec.pipeline.TweetLabeler;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.*;

public class TweetLabelService {

    private String tweetCollectionName;

    private TweetMongoService tweetMongoService;

    private MongoCollection<TweetLabelEntity> labelCollection;

    private TweetLabeler labeler;

    private NaiveBayes classifier;

    public TweetLabelService() {}

    public TweetLabelService(Properties properties) {
        this.setTweetMongoService(properties.getProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME));
        this.setLabelCollection(properties.getProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME));
        this.labeler = new TweetLabeler(properties);
        if(this.labelCollection.countDocuments() == 0)
            this.initLabelCollectionWithEmptyLabelEntities();
    }

    private void setTweetMongoService(String tweetMongoCollectionName) {
        this.tweetMongoService = new TweetMongoService(tweetMongoCollectionName);
    }

    private void setLabelCollection(String labelCollectionName) {
        this.labelCollection = MongoHelper.getInstance().getCollection(labelCollectionName, TweetLabelEntity.class);
    }

    public void createClassifier() {
        System.err.println("TweetLabelService.createClassifier() not implemented!");
//        this.classifier = new NaiveBayes(this.tweetCollectionName + "-bayes", Arrays.asList("+", "-"));
    }

    private void initLabelCollectionWithEmptyLabelEntities() {
        if(this.labelCollection.countDocuments() == 0) {
            List<TweetLabelEntity> labelList = this.tweetMongoService.getTweets()
                    .stream()
                    .map(e -> new TweetLabelEntity(e.getTweetId(), null))
                    .collect(Collectors.toList());
            this.labelCollection.insertMany(labelList);
        }
    }

    public void labelSample(int n) {
        tweetMongoService.getRandomTweets(n).forEach(t -> this.labelTweet(t));
    }

    /**
     *
     * @param n
     * @return true; if there where any unlabeled samples left
     */
    public boolean labelUnlabeledSample(int n) {
        List<TweetLabelEntity> labels = this.labelCollection.aggregate(Arrays.asList(
                match(Filters.not(Filters.exists("label"))),
                sample(n)
        )). into(new ArrayList<>(n));
        for(TweetLabelEntity l : labels) {
            TweetEntity entity = this.tweetMongoService.getTweetEntityByTweetId(l.getTweetId());
            this.labelTweet(entity);
        }
        return labels.size() > 0;
    }

    public void labelTweet(TweetEntity t) {
        String label = labeler.label(t);
        if(label.equals("drop"))
            this.deleteTweet(t);
        else
            this.saveLabel(t, label);
    }

    public void labelTweet(TweetEntity t, String additionalInfo) {
        String label = labeler.label(t, additionalInfo);
        if(label.equals("drop"))
            this.deleteTweet(t);
        else
            this.saveLabel(t, label);
    }

    public void labelTweet(long id) {
        TweetEntity t = this.tweetMongoService.getTweetEntityByTweetId(id);
        this.labelTweet(t);
    }

    public void labelTweet(long id, String additionalInfo) {
        TweetEntity t = this.tweetMongoService.getTweetEntityByTweetId(id);
        this.labelTweet(t, additionalInfo);
    }

    public void printStatistics() {
        long irrelevant = labelCollection.countDocuments(Filters.eq("label", "1"));
        long promotional = labelCollection.countDocuments(Filters.eq("label", "2"));
        long relevant = labelCollection.countDocuments(Filters.eq("label", "3"));
        long vuln = labelCollection.countDocuments(Filters.eq("label", "4"));
        long unlabeled = this.getUnlabeledCount();
        System.out.println("Irrelevant: " + irrelevant);
        System.out.println("Promotional: " + promotional);
        System.out.println("Relevant: " + relevant);
        System.out.println("Vulnerabilities: " + vuln);
        System.out.println("Unlabeled: " + unlabeled);
        System.out.println();
    }

    public long getUnlabeledCount() {
        return labelCollection.countDocuments(Filters.not(Filters.exists("label")));
    }

    public List<TweetLabelEntity> getUnlabeledEntities() {
        return labelCollection.find(Filters.not(Filters.exists("label"))).into(new ArrayList<>());
    }

    public String saveLabel(TweetEntity t, String label) {
        TweetLabelEntity old = this.labelCollection.findOneAndDelete(Filters.eq("tweetId", t.getTweetId()));
        this.labelCollection.insertOne(new TweetLabelEntity(t.getTweetId(), label));

        if(this.classifier != null) {
            TextPreprocessor preprocessor = new TextPreprocessor();
            String processed = preprocessor.process(t.getText());
            this.classifier.addLabeledEntry(Arrays.asList(processed.split(" ")), label);
        }

        return old.getLabel();
    }

    public NaiveBayes getNaiveBayes() {
        return this.classifier;
    }

    public void dropLabelCollection() {
        this.labelCollection.drop();
        this.initLabelCollectionWithEmptyLabelEntities();

        if(this.classifier != null)
            this.classifier.dropCollections();
    }

    private void deleteTweet(TweetEntity t) {
        this.tweetMongoService.deleteTweet(t);
        this.labelCollection.findOneAndDelete(Filters.eq("tweetId", t.getTweetId()));
    }

    public List<TweetLabelEntity> getLabelEntities() {
        return this.labelCollection.find().into(new ArrayList<>());
    }

}
