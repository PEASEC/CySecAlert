package de.tudarmstadt.peasec.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import de.tudarmstadt.peasec.entity.ProcessedTextEntity;
import de.tudarmstadt.peasec.entity.TweetLabelEntity;
import de.tudarmstadt.peasec.depr.NaiveBayes;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.util.TweetRepresentationParser;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class NaiveBayesUncertaintySamplingService {

    public static final String SAMPLE_SIZE = "UncertaintySamplingService.SampleSize";

    NaiveBayes nb;
    List<Long> labeledTweets;
    MongoCollection<TweetLabelEntity> labelCollection;
    TweetRepresentationParser parser;
    int sampleSize;

    public NaiveBayesUncertaintySamplingService(Properties properties) {
        this.nb = new NaiveBayes(properties);
        this.labeledTweets = new ArrayList<>();
        this.labelCollection = MongoHelper.getInstance()
                .getCollection(properties.getProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME), TweetLabelEntity.class);
        this.parser = new TweetRepresentationParser(properties);
        this.sampleSize = Integer.parseInt(properties.getProperty(SAMPLE_SIZE));
    }

    public ProcessedTextEntity getMostUncertain() {
        return this.getMostUncertain(this.parser.getProcessedTextEntityById(this.getUnlabeledTweets()));
    }

    public ProcessedTextEntity getMostUncertain(List<ProcessedTextEntity> textEntityList) {
        return this.nb.getMostUncertain(textEntityList);
//        ConcurrentHashMap<Double, ProcessedTextEntity> concurrentHashMap = new ConcurrentHashMap<>();
//        textEntityList.parallelStream().forEach((entity) -> {
//            double certainty = this.nb.getCertainty(entity);
//            concurrentHashMap.put(certainty, entity);
//        });
//        List<Double> distances = new ArrayList<>(concurrentHashMap.keySet());
//        Collections.sort(distances);
//        return concurrentHashMap.get(distances.get(distances.size()-1));
    }

    public void updateModel(ProcessedTextEntity entity, String label) {
        this.nb.addLabeledEntry(entity, label);
        this.labeledTweets.add(entity.getTweetId());
    }

    public List<Long> getUnlabeledTweets() {
        return this.getUnlabeledTweets(this.sampleSize);
    }

    public List<Long> getUnlabeledTweets(int n) {
        return this.labelCollection.aggregate(Arrays.asList(
                Aggregates.match(Filters.not(Filters.in("tweetId", labeledTweets))),
                Aggregates.sample(n)
        )).into(new ArrayList<>())
                .stream()
                .map(TweetLabelEntity::getTweetId)
                .collect(Collectors.toList());
    }

}
