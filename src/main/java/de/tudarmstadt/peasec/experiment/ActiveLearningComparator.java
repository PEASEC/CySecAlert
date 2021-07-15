package de.tudarmstadt.peasec.experiment;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import de.tudarmstadt.peasec.entity.ProcessedTextEntity;
import de.tudarmstadt.peasec.entity.TweetLabelEntity;
import de.tudarmstadt.peasec.entity.help.LabeledProcessedTextWrapper;
import de.tudarmstadt.peasec.depr.NaiveBayes;
import de.tudarmstadt.peasec.service.RandomForestService;
import de.tudarmstadt.peasec.service.NaiveBayesUncertaintySamplingService;
import de.tudarmstadt.peasec.service.wrapper.EvalResults;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.util.TweetRepresentationParser;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;
import de.tudarmstadt.peasec.util.config.UrlConfigurations;

import java.util.*;
import java.util.stream.Collectors;

public class ActiveLearningComparator {

    public static final String EVALUATION_STEPS = "ActiveLearningComparator.EvaluationSteps";
    public static final String START_WITH_N_RANDOM = "ActiveLearningComparator.StartWithNRandom";
    public static final String MINORITY_WEIGHT = "ActiveLearningComparator.MinorityWeight";

    private MongoCollection<TweetLabelEntity> labelCollection;
    private MongoCollection<ProcessedTextEntity> processedTextCollection;

    private RandomForestService rfService;

    private NaiveBayesUncertaintySamplingService samplingService;

    private TweetRepresentationParser parser;

    private List<Integer> steps;

    private int startWithNRandom, minorityWeight;

    private Map<Integer, List<EvalResults>> evalMap = new HashMap<>();

    private Properties properties;

    public ActiveLearningComparator(Properties properties) {
        this.labelCollection = MongoHelper.getInstance()
                .getCollection(properties.getProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME), TweetLabelEntity.class);
        this.processedTextCollection = MongoHelper.getInstance()
                .getCollection(properties.getProperty(CollectionNameProperties.PROCESSED_TEXT_ENTITY_COLLECTION_NAME), ProcessedTextEntity.class);
        this.parser = new TweetRepresentationParser(properties);
        this.steps = Arrays.stream(properties.getProperty(EVALUATION_STEPS).split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        this.steps.sort(Integer::compareTo);
        this.startWithNRandom = Integer.parseInt(properties.getProperty(START_WITH_N_RANDOM, "100"));
        this.minorityWeight = Integer.parseInt(properties.getProperty(MINORITY_WEIGHT, "1"));
        this.properties = properties;
    }

    private static LabeledProcessedTextWrapper mapLabel(LabeledProcessedTextWrapper entity) {
        String label = "irrelevant";
        String RELEVANT = "relevant";
        if(entity.getLabel().equals("3") || entity.getLabel().equals("4"))
            label = "relevant";
        entity.setLabel(label);
        return entity;
    }

    private static List<LabeledProcessedTextWrapper> mapLabel(List<LabeledProcessedTextWrapper> aList) {
        return aList.stream()
                .map(ActiveLearningComparator::mapLabel)
                .collect(Collectors.toList());
    }

    public void evaluateRandomSelection() {
        this.rfService = new RandomForestService(properties);
        this.samplingService = new NaiveBayesUncertaintySamplingService(properties);
        List<LabeledProcessedTextWrapper> entityList = new ArrayList<>();
        int stepPointer = 0;
        for(int i = 0; stepPointer < steps.size(); i++) {
            entityList.add(this.getRandomSelected());

            if(i == this.steps.get(stepPointer) - 1) {
                System.out.println("Random Selection : Starting evaluation after " + this.steps.get(stepPointer) + " labels...");
//                rfService.setTrainingData(entityList);
//                rfService.evaluate();
                this.evaluateWeightedRandomForest(this.steps.get(stepPointer), entityList, 10);
                stepPointer ++;
            }
        }
        this.rfService = null;
        this.samplingService = null;
    }

    private LabeledProcessedTextWrapper getRandomSelected() {
        TweetLabelEntity e = this.labelCollection.aggregate(Arrays.asList(Aggregates.sample(1))).first();
        LabeledProcessedTextWrapper out = parser.getLabeledProcessedTextWrapper(e);
        return mapLabel(out);
    }

    public void evaluateActiveSelection() {
        this.rfService = new RandomForestService(properties);
        this.samplingService = new NaiveBayesUncertaintySamplingService(properties);

        List<LabeledProcessedTextWrapper> entityList = new ArrayList<>();

        int stepPointer = 0;
        for(int i = 0; stepPointer < steps.size(); i++) {
            if(i < this.startWithNRandom) {
                LabeledProcessedTextWrapper tmp = this.getRandomSelected();
                entityList.add(tmp);
                this.samplingService.updateModel(this.parser.getProcessedTextEntities(tmp), tmp.getLabel());
            }
            else
                entityList.add(this.getActiveSelected());

            if(i == this.steps.get(stepPointer) - 1) {
                System.out.println("Active Selection : Starting evaluation after " + this.steps.get(stepPointer) + " labels...");
                this.evaluateWeightedRandomForest(this.steps.get(stepPointer), entityList, 5);
//                rfService.setTrainingData(entityList);
//                rfService.evaluate();
                stepPointer ++;
            }
        }
        this.rfService = null;
        this.samplingService = null;
    }

    private LabeledProcessedTextWrapper getActiveSelected() {
        ProcessedTextEntity e = this.samplingService.getMostUncertain();
        this.samplingService.updateModel(e, this.parser.getTweetLabelEntity(e).getLabel());
        return mapLabel(this.parser.getLabeledProcessedTextWrapper(e));
    }

    public void evaluateHybridSelection() {
        this.rfService = new RandomForestService(properties);
        this.samplingService = new NaiveBayesUncertaintySamplingService(properties);

        List<LabeledProcessedTextWrapper> entityList = new ArrayList<>();

        int stepPointer = 0;
        for(int i = 0; stepPointer < steps.size(); i++) {
            if(i % 2 == 0 && i > this.startWithNRandom) {
                entityList.add(this.getActiveSelected());
            }
            else {
                LabeledProcessedTextWrapper tmp = this.getRandomSelected();
                entityList.add(tmp);
                this.samplingService.updateModel(this.parser.getProcessedTextEntities(tmp), tmp.getLabel());
            }

            if(i == this.steps.get(stepPointer) - 1) {
                System.out.println("Active Selection : Starting evaluation after " + this.steps.get(stepPointer) + " labels...");
//                rfService.setTrainingData(entityList);
//                rfService.evaluate();
                this.evaluateWeightedRandomForest(this.steps.get(stepPointer), entityList, 5);
                stepPointer ++;
            }
        }

        this.rfService = null;
        this.samplingService = null;
    }

    private void evaluateWeightedRandomForest(Integer stage, List<LabeledProcessedTextWrapper> trainList, int minorityWeight) {
        // built test set
        List<Long> idList = trainList.stream().map(LabeledProcessedTextWrapper::getTweetId).collect(Collectors.toList());
        List<TweetLabelEntity> testLabels = this.labelCollection.aggregate(Arrays.asList(
                Aggregates.match(Filters.not(Filters.in("tweetId", idList))),
                Aggregates.sample(1000)
        )).into(new ArrayList<>());
        List<LabeledProcessedTextWrapper> testEntities = this.parser.getLabeledProcessedTextWrapper(testLabels);
        testEntities.forEach(e -> {
            String label = "irrelevant";
            if(e.getLabel().equals("3") || e.getLabel().equals("4"))
                label = "relevant";
            e.setLabel(label);
        });

        // print training set statistics
        long rel = trainList.stream().filter(e -> e.getLabel().equals("relevant")).count();
        long irrel = trainList.stream().filter(e -> e.getLabel().equals("irrelevant")).count();
        System.out.println("=== Training Set Statistics ===");
        System.out.println("Irrelevants: " + irrel);
        System.out.println("Relevants: " + rel);

        // simulate train set weighting
        List<LabeledProcessedTextWrapper> weightedTrainingList = new ArrayList<>(trainList);
        List<LabeledProcessedTextWrapper> relevantTweets = trainList.stream().filter(e -> e.getLabel().equals("relevant")).collect(Collectors.toList());
        for(int i = 0; i < minorityWeight-1; i++)
            weightedTrainingList.addAll(relevantTweets);

        this.rfService.setTrainingData(weightedTrainingList);
        this.rfService.setTestData(testEntities);
        this.rfService.train();
        EvalResults evalResult = this.rfService.calculateTestResults(0.0);
        evalResult.print();

        if(!this.evalMap.containsKey(stage))
            this.evalMap.put(stage, new ArrayList<>());
        this.evalMap.get(stage).add(evalResult);

    }

    public void endEvaluation() {
        System.out.println();
        System.out.println("=== END EVALUATION ===");
        for(int stage : this.steps) {
            List<EvalResults> resultList = evalMap.get(stage);
            System.out.println("--- " + stage + " steps ---");
            System.out.println("Avg Accuracy: " + resultList.stream().mapToDouble(EvalResults::getAccuracy).average().getAsDouble());
            System.out.println("Avg Precision: " + resultList.stream().mapToDouble(EvalResults::getPrecision).average().getAsDouble());
            System.out.println("Avg Recall: " + resultList.stream().mapToDouble(EvalResults::getRecall).average().getAsDouble());
            System.out.println("Avg F1: " + resultList.stream().mapToDouble(EvalResults::getF1).average().getAsDouble());
            System.out.println();
        }

        System.out.println("=== CSV ===");
        System.out.println("steps,accuracy,precision,recall,f1");
        for (int stage : this.steps) {
            List<EvalResults> resultList = evalMap.get(stage);
            double avg = resultList.stream().mapToDouble(EvalResults::getAccuracy).average().getAsDouble();
            double precision = resultList.stream().mapToDouble(EvalResults::getPrecision).average().getAsDouble();
            double recall = resultList.stream().mapToDouble(EvalResults::getRecall).average().getAsDouble();
            double f1 = resultList.stream().mapToDouble(EvalResults::getF1).average().getAsDouble();
            System.out.println(stage + "," + avg + "," + precision + "," + recall + "," + f1);
        }
    }

    public static void execute() {
        Properties properties = new Properties();
        properties.setProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME, "2019-nov-tweets");
        properties.setProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME, "2019-nov-tweets-labels");
        properties.setProperty(CollectionNameProperties.PROCESSED_TEXT_ENTITY_COLLECTION_NAME, "2019-nov-tweets-token");
        properties.setProperty(CollectionNameProperties.USER_ENTITY_COLLECTION_NAME, "user-base");
        properties.setProperty(ActiveLearningComparator.EVALUATION_STEPS, "200,400,600,800,1000,1200,1400,1600,1800,2000,2200,2400,2600,2800,3000,3200,3400,3600,3800,4000,4200,4400,4600,4800,5000,5200,5400,5600,5800,6000");
        properties.setProperty(UrlConfigurations.CLASSIFIER_TRAINING_DATA_URL, "D:/Dropbox/MasterThesis/experts/train.arff");
        properties.setProperty(UrlConfigurations.CLASSIFIER_TEST_DATA_URL, "D:/Dropbox/MasterThesis/experts/test.arff");
        properties.setProperty(CollectionNameProperties.NAIVE_BAYES_WORD_COUNT_COLLECTION_NAME, "bayes-word-count");
        properties.setProperty(CollectionNameProperties.NAIVE_BAYES_CLASS_PROBABILITY_COLLECTION_NAME, "bayes-class-prob");
        properties.setProperty(RandomForestService.LABELS, "relevant, irrelevant");
        properties.setProperty(NaiveBayes.LABELS, "relevant,irrelevant");
        properties.setProperty(NaiveBayesUncertaintySamplingService.SAMPLE_SIZE, "100");
        properties.setProperty(ActiveLearningComparator.START_WITH_N_RANDOM, "100");

        ActiveLearningComparator activeLearningComparator;

        int count = 5;
        List<String> minorityWeights = Arrays.asList("1");

        // RANDOM ------------------------------------------------------------------------------------

        for(String minorityWeight : minorityWeights) {
            properties.setProperty(MINORITY_WEIGHT, minorityWeight);
            activeLearningComparator = new ActiveLearningComparator(properties);
            for(int i = 0; i < count; i++) {
                activeLearningComparator.evaluateRandomSelection();
            }
            // System.out.println("Random Selection " + minorityWeight + "-Weight");
            activeLearningComparator.endEvaluation();
        }

        // NB ----------------------------------------------------------------------------------------------------------
        for(String minorityWeight : minorityWeights) {
            properties.setProperty(MINORITY_WEIGHT, minorityWeight);
            activeLearningComparator = new ActiveLearningComparator(properties);
            for(int i = 0; i < count; i++) {
                activeLearningComparator.evaluateActiveSelection();
            }
            // System.out.println("NB Selection " + minorityWeight + "-Weight");
            activeLearningComparator.endEvaluation();
        }

        // Hybrid ----------------------------------------------------------------------------------------------------------

        for(String minorityWeight : minorityWeights) {
            properties.setProperty(MINORITY_WEIGHT, minorityWeight);
            activeLearningComparator = new ActiveLearningComparator(properties);
            for(int i = 0; i < count; i++) {
                activeLearningComparator.evaluateHybridSelection();
            }
            // System.out.println("Hybrid Selection " + minorityWeight + "-Weight");
            activeLearningComparator.endEvaluation();
        }

//        for(String n : Arrays.asList("100", "200", "300", "400", "500")) {
//            System.out.println("===============================================");
//            System.out.println("===== Start with " + n + " Random===================");
//            System.out.println("===============================================");
//            properties.setProperty(ActiveLearningComparator.START_WITH_N_RANDOM, n);
//            ActiveLearningComparator activeLearningComparator = new ActiveLearningComparator(properties);
//            activeLearningComparator.evaluateActiveSelection();
//        }
    }

}
