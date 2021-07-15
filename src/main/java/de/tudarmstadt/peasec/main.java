package de.tudarmstadt.peasec;

import de.tudarmstadt.peasec.experiment.ClassificationEvaluator;
import de.tudarmstadt.peasec.pipeline.AbstractWekaIncrementalClassifier;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;

import java.util.*;
import java.util.stream.Collectors;


public class main {

    public class CollectionName {
        public static final String DECEMBER_2019_TWEETS = "2019-dez-tweets";
        public static final String DECEMBER_2019_TRISTAN_LABELS = "2019-dez-tweets-labels";
        public static final String DECEMBER_2019_TOKEN = "2019-nov-tweets-token";

        public static final String MAY_2020_W1u2_TWEETS = "2020-may-W1u2-tweets";
        public static final String MAY_2020_W1u2_TRISTAN_LABELS = "2020-may-W1u2-tweets-labels";
        public static final String MAY_2020_W1u2_TOKEN = "2020-may-W1u2-tweets-token";
    }

    public static Map<String, String> getRelevanceMapping() {
        Map<String, String> map = new HashMap<>(4);
        map.put("1", "irrelevant");
        map.put("2", "irrelevant");
        map.put("3", "relevant");
        map.put("4", "relevant");
        return map;
    }

    public static Properties getFirstDatasetProperties() {
        Properties properties = new Properties();
        properties.setProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME, CollectionName.DECEMBER_2019_TWEETS);
        properties.setProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME, CollectionName.DECEMBER_2019_TRISTAN_LABELS);
        properties.setProperty(CollectionNameProperties.PROCESSED_TEXT_ENTITY_COLLECTION_NAME, CollectionName.DECEMBER_2019_TOKEN);
        properties.setProperty(CollectionNameProperties.USER_ENTITY_COLLECTION_NAME, "user-base");
        return properties;
    }

    public static Properties getSecondDatasetProperties() {
        Properties properties = new Properties();
        properties.setProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME, CollectionName.MAY_2020_W1u2_TWEETS);
        properties.setProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME, CollectionName.MAY_2020_W1u2_TRISTAN_LABELS);
        properties.setProperty(CollectionNameProperties.PROCESSED_TEXT_ENTITY_COLLECTION_NAME, CollectionName.MAY_2020_W1u2_TOKEN);
        properties.setProperty(CollectionNameProperties.USER_ENTITY_COLLECTION_NAME, "user-base");
        return properties;
    }

    public static Properties getExperimentProperties() {
        Properties properties = new Properties();
        properties.setProperty(ClassificationEvaluator.STEPS, "100,200,300,400,500,600,700,800,900,1000");
        properties.setProperty(AbstractWekaIncrementalClassifier.LABELS, "relevant,irrelevant");
        return properties;
    }

    public static void main(String args[]) {

        // crawl Tweets from user list
//        Crawling.crawl("users", "tweets");

        // label tweets
//        Labeling.labelTweets(getFirstDatasetProperties());

        // preprocess tweets (tokenization)
//        Preprocessing.preprocessTweets(getFirstDatasetProperties(), true);

        // train a classifier
        // TODO:

        // use a pretrained classifier
        // TODO:

        // Experiment from Fig. 3: Performance Comparison of Naive Bayes, kNN with k = 50 and Random Forest classifier with Uncertainty Sampling Based on their respective model
        ClassificationEvaluator.compareActiveClassifier(getFirstDatasetProperties(), getExperimentProperties(), 1);

        //Experiment from Fig. 2: Performance Comparison of RF Classifier trained with different Uncertainty Samplers: Random, RF, RF/Random(50/50), kNN
//        ClassificationEvaluator.compareSamplingTechnique(getFirstDatasetProperties(), getExperimentProperties(), 1);

        // run clustering
//        Clustering.cluster(getFirstDatasetProperties(), getRelevanceMapping());

        //run timed clustering
//        Clustering.cluster(getFirstDatasetProperties(), getRelevanceMapping(), true);

        // Run complete Classification and Clustering on a dataset
//        Classifying.completeEval(getSecondDatasetProperties(), 100, 400, getRelevanceMapping());
    }
}
