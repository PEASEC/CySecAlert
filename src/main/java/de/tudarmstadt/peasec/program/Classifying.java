package de.tudarmstadt.peasec.program;

import de.tudarmstadt.peasec.entity.ProcessedTextEntity;
import de.tudarmstadt.peasec.entity.TweetEntity;
import de.tudarmstadt.peasec.entity.help.LabeledProcessedTextWrapper;
import de.tudarmstadt.peasec.pipeline.WekaKnnClassifier;
import de.tudarmstadt.peasec.pipeline.WekaRandomForestClassifier;
import de.tudarmstadt.peasec.service.ClusteringService;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.util.TweetRepresentationParser;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;

import java.util.*;

public class Classifying {

    //TODO: Documentation
    public static void applyRelevanceMappingToEntities(List<LabeledProcessedTextWrapper> wrappedTweets, Map<String, String> relevanceMapping) {
        if(relevanceMapping != null) {
            wrappedTweets.forEach(e -> {
                e.setLabel(relevanceMapping.get(e.getLabel()));
            });
        }
    }

    /**
     * Execute Classification and Clustering with the CySecAlert default configuration on a labeled dataset, where
     * label "irrelevant" represents irrelevant and label "relevant" represents relevant tweets.
     *
     * @param datasetProperties @see overloaded function below
     * @param tweetCount @see overloaded function below
     * @param samples @see overloaded function below
     */
    public static void completeEval(Properties datasetProperties, int tweetCount, int samples) {
        completeEval(datasetProperties, tweetCount, samples, null);
    }

    /**
     * Execute Classification and Clustering with the CySecAlert default configuration on a labeled dataset with an
     * arbitrary mapping from label to relevance
     *
     * @param datasetProperties Property Object with the following properties set
     *                          - CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME: Collection that stores
     *                          objects of type de.tudarmstadt.peasec.entity.TweetEntity representing the tweets
     *                          - CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME: Collection storing
     *                          objects of type de.tudarmstadt.peasec.entity.TweetLabelEntity representing the relevant
     *                          labels for the tweets in the dataset
     *                          - CollectionName.MAY_2020_W1u2_TOKEN: Collection storing objects of type
     *                          de.tudarmstadt.peasec.entity.ProcessedTextEntity representing tokenized text of tweets
     *
     *                          For an example: @see getFristDatasetProperties()
     * @param tweetCount        number of tweets that are certained by uncertainty of a kNN classifier and than used to
     *                          train the random forest classifier for relevance classification
     * @param sampleSize        number of tweets that are compared regarding their uncertainty per iteration (reduces run-time)
     * @param relevanceMapping      a mapping from the labels, that occure in the label dataset to "irrelevant" for irrelevant and
     *                          to "relevant" for relevant tweets
     */
    public static void completeEval(Properties datasetProperties, int tweetCount, int sampleSize, Map<String, String> relevanceMapping) {
        TweetRepresentationParser parser = new TweetRepresentationParser(datasetProperties);

        // get tweet list
        List<TweetEntity> tweets = MongoHelper.getInstance()
                .getCollection(datasetProperties.getProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME), TweetEntity.class)
                .find()
                .into(new ArrayList<>());

        //map relevance to binary
        List<LabeledProcessedTextWrapper> unusedEntities = new ArrayList<>(parser.getLabeledProcessedTextWrapper(tweets));
        applyRelevanceMappingToEntities(unusedEntities, relevanceMapping);

        List<LabeledProcessedTextWrapper> randomSubList = new ArrayList<>(unusedEntities.subList(0, 50));
        unusedEntities.removeAll(randomSubList);
        WekaKnnClassifier knnClassifier = new WekaKnnClassifier();
        knnClassifier.buildClassifier(randomSubList);

        LabeledProcessedTextWrapper entity;
        for (int i = 0; i < tweetCount; i++) {

            if(i % 100 == 0)
                System.out.println("Uncertainty sampled " + i + " tweets.");
            //get Entity
            Collections.shuffle(unusedEntities);
            List<LabeledProcessedTextWrapper> sample = new ArrayList<>(unusedEntities.subList(0, sampleSize));
            entity = knnClassifier.getMostUncertain(sample);
            boolean b = unusedEntities.remove(entity);

            //update classifier
            knnClassifier.updateClassifier(entity);
        }

        // train relevance classifier
        WekaRandomForestClassifier rf = new WekaRandomForestClassifier();
        rf.buildClassifier(knnClassifier.getTrainedEntities());

        // classify tweet relevance for whole dataset
        List<ProcessedTextEntity> relevantList = new ArrayList<>();
        for(ProcessedTextEntity e : parser.getProcessedTextEntities(tweets)) {
            double d = rf.classifyInstance(e);
            if(d == 0.0d) {
                relevantList.add(e);
            }
        }

        // perform tweet clustering for relevant tweets
        datasetProperties.setProperty(ClusteringService.DISTANCE_THRESHOLD, "0.75");
        datasetProperties.setProperty(ClusteringService.ALERT_TWEET_COUNT_THRESHOLD, "5");
        ClusteringService clusteringService = new ClusteringService(datasetProperties);
        for(ProcessedTextEntity e: relevantList)
            clusteringService.add(e);
        clusteringService.printRelevantClusters();
    }
}
