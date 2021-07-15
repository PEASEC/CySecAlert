package de.tudarmstadt.peasec.program;

import de.tudarmstadt.peasec.entity.ProcessedTextEntity;
import de.tudarmstadt.peasec.entity.TweetLabelEntity;
import de.tudarmstadt.peasec.main;
import de.tudarmstadt.peasec.service.ClusteringService;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.util.Timer;
import de.tudarmstadt.peasec.util.TweetRepresentationParser;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class Clustering {

    //TODO: Documentation
    public static void cluster(Properties dataProperties, Map<String, String> relevanceMapping, double distanceThreshold, int alertTweetCountThreshold, boolean timed) {
        TweetRepresentationParser parser = new TweetRepresentationParser(dataProperties);

        // get labeled tweets
        List<TweetLabelEntity> labelEntities = MongoHelper
                .getInstance()
                .getCollection(
                        dataProperties.getProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME),
                        TweetLabelEntity.class
                )
                .find()
                .into(new ArrayList<>());

        // filter for relevant tweets
        List<ProcessedTextEntity> processedTextEntities = parser.getProcessedTextEntities(labelEntities.stream().filter(e -> {
            String s = e.getLabel();
            String foo = relevanceMapping.get(e.getLabel());
            return foo.equals("relevant");
        }).collect(Collectors.toList()));

        //perform clustering
        Properties properties = new Properties(dataProperties);
        properties.setProperty(ClusteringService.DISTANCE_THRESHOLD, Double.toString(distanceThreshold));
        properties.setProperty(ClusteringService.ALERT_TWEET_COUNT_THRESHOLD, Integer.toString(alertTweetCountThreshold));
        ClusteringService clusteringService = new ClusteringService(properties);
        Timer timer = new Timer();
        if (timed) timer.start();
        for(ProcessedTextEntity e : processedTextEntities) {
            clusteringService.add(e);
        }
        if(timed) timer.end();

        // Print results
        clusteringService.printRelevantClusters();
        if(timed) System.out.println("Runtime: " + timer.getTime() + "ns");
    }

    //TODO: Documentation
    public static void cluster(Properties dataProperties, Map<String, String> relevanceMapping, double distanceThreshold, int alertTweetCountThreshold) {
        cluster(dataProperties, relevanceMapping, distanceThreshold, alertTweetCountThreshold, false);
    }

    // TODO: Documentation
    public static void cluster(Properties dataProperties, Map<String, String> relevanceMapping, boolean timed) {
        cluster(dataProperties, relevanceMapping, 0.75, 3, timed);
    }

    // TODO: Documentation
    public static void cluster(Properties dataProperties, Map<String, String> relevanceMapping) {
        cluster(dataProperties, relevanceMapping, false);
    }

    // TODO: Documentation
    public static void cluster() {
        Properties parserProperties = new Properties();
        parserProperties.setProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME, main.CollectionName.MAY_2020_W1u2_TWEETS);
        parserProperties.setProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME, main.CollectionName.MAY_2020_W1u2_TRISTAN_LABELS);
        parserProperties.setProperty(CollectionNameProperties.PROCESSED_TEXT_ENTITY_COLLECTION_NAME, main.CollectionName.MAY_2020_W1u2_TOKEN);
        TweetRepresentationParser parser = new TweetRepresentationParser(parserProperties);

        List<TweetLabelEntity> labelEntities = MongoHelper.getInstance().getCollection(main.CollectionName.MAY_2020_W1u2_TRISTAN_LABELS, TweetLabelEntity.class).find().into(new ArrayList<>());

        List<ProcessedTextEntity> processedTextEntities = parser.getProcessedTextEntities(labelEntities.stream().filter(e -> {
            String s = e.getLabel();
            return s.equals("3") || s.equals("4");
        }).collect(Collectors.toList()));


        Properties properties = new Properties(parserProperties);
        properties.setProperty(ClusteringService.DISTANCE_THRESHOLD, "0.75");
        properties.setProperty(ClusteringService.ALERT_TWEET_COUNT_THRESHOLD, "3");
        ClusteringService clusteringService = new ClusteringService(properties);

        for(ProcessedTextEntity e : processedTextEntities) {
            clusteringService.add(e);
        }

        clusteringService.printRelevantClusters();
    }
}
