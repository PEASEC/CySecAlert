package de.tudarmstadt.peasec.experiment;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.tudarmstadt.peasec.entity.ProcessedTextEntity;
import de.tudarmstadt.peasec.entity.TweetLabelEntity;
import de.tudarmstadt.peasec.entity.help.LabeledProcessedTextWrapper;
import de.tudarmstadt.peasec.main;
import de.tudarmstadt.peasec.pipeline.AbstractWekaIncrementalClassifier;
import de.tudarmstadt.peasec.pipeline.WekaKnnClassifier;
import de.tudarmstadt.peasec.pipeline.WekaNaiveBayesClassifier;
import de.tudarmstadt.peasec.pipeline.WekaRandomForestClassifier;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.util.PropertyHandler;
import de.tudarmstadt.peasec.util.Timer;
import de.tudarmstadt.peasec.util.TweetRepresentationParser;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;
import weka.classifiers.evaluation.Evaluation;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.sample;

public class ClassificationEvaluator {

    public static final String STEPS = "ClassificationEvaluator.Steps";

    MongoCollection<TweetLabelEntity> labelCollection;
    MongoCollection<ProcessedTextEntity> processedTextCollection;

    List<Long> virtuallyAlreadyLabeldList;

    TweetRepresentationParser parser;

    List<Integer> stepList;

    public ClassificationEvaluator(Properties properties) {
        this.labelCollection = MongoHelper.getInstance().getCollection(properties.getProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME), TweetLabelEntity.class);
        this.processedTextCollection = MongoHelper.getInstance().getCollection(properties.getProperty(CollectionNameProperties.PROCESSED_TEXT_ENTITY_COLLECTION_NAME), ProcessedTextEntity.class);

        this.parser = new TweetRepresentationParser(properties);

        this.stepList = Arrays.asList(properties.getProperty(STEPS).split(",")).stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public List<LabeledProcessedTextWrapper> selectRelevantLabels(List<LabeledProcessedTextWrapper> labelEntities) {
        List<LabeledProcessedTextWrapper> unlabeledEntities = new ArrayList<>(labelEntities);
        unlabeledEntities.forEach(l -> {
            String label = "irrelevant";
            if(l.getLabel().equals("4"))
                label = "relevant";
            l.setLabel(label);
        });
        return unlabeledEntities;
    }

    public Map<Integer, Evaluation> evaluateActiveNaiveBayes() {
        int nRandomSelected = 50;
        int sampleSize = 200;

        Map<Integer, Evaluation> map = new HashMap<>();

        //get entities
        List<TweetLabelEntity> labelEntities = this.labelCollection.find().into(new ArrayList<>());
        List<LabeledProcessedTextWrapper> unlabledEntities = this.parser.getLabeledProcessedTextWrapper(labelEntities);
        // parse labels
        unlabledEntities = this.selectRelevantLabels(unlabledEntities);
        //randomize
        Collections.shuffle(unlabledEntities);

        //create new classifier
        WekaNaiveBayesClassifier nb = new WekaNaiveBayesClassifier();

        List<LabeledProcessedTextWrapper> randomSubList = new ArrayList<>(unlabledEntities.subList(0, nRandomSelected));
        unlabledEntities.removeAll(randomSubList);
        nb.buildClassifier(randomSubList);

        // i is the number of the step
        LabeledProcessedTextWrapper entity;
        int stepListPointer = 0;
        int unlabeledEntityPointer = 0;
        for(int i = nRandomSelected; i < this.stepList.get(this.stepList.size()-1); i++) {

            //get Entity
            Collections.shuffle(unlabledEntities);
            List<LabeledProcessedTextWrapper> sample = new ArrayList<>(unlabledEntities.subList(0, sampleSize));
            entity = nb.getMostUncertain(sample);
            boolean b = unlabledEntities.remove(entity);

            //update classifier
            nb.updateClassifier(entity);

            if(i == this.stepList.get(stepListPointer) - 1) {
                System.out.println("Evaluation at step" + this.stepList.get(stepListPointer) + "...");
                Evaluation evaluation = nb.evaluation();
                map.put(this.stepList.get(stepListPointer), evaluation);
                stepListPointer++;
            }
        }
        return map;
    }

    public Map<Integer, Evaluation> evaluateActiveKnn() {
        int nRandomSelected = 50;
        int sampleSize = 200;

        Map<Integer, Evaluation> map = new HashMap<>();

        //get entities
        List<TweetLabelEntity> labelEntities = this.labelCollection.find().into(new ArrayList<>());
        List<LabeledProcessedTextWrapper> unlabledEntities = this.parser.getLabeledProcessedTextWrapper(labelEntities);
        // parse labels
        unlabledEntities = this.selectRelevantLabels(unlabledEntities);
        //randomize
        Collections.shuffle(unlabledEntities);

        //create new classifier
        WekaKnnClassifier nb = new WekaKnnClassifier();

        List<LabeledProcessedTextWrapper> randomSubList = new ArrayList<>(unlabledEntities.subList(0, nRandomSelected));
        unlabledEntities.removeAll(randomSubList);
        nb.buildClassifier(randomSubList);

        // i is the number of the step
        LabeledProcessedTextWrapper entity;
        int stepListPointer = 0;
        int unlabeledEntityPointer = 0;
        for(int i = nRandomSelected; i < this.stepList.get(this.stepList.size()-1); i++) {

            //get Entity
            Collections.shuffle(unlabledEntities);
            List<LabeledProcessedTextWrapper> sample = new ArrayList<>(unlabledEntities.subList(0, sampleSize));
            entity = nb.getMostUncertain(sample);
            boolean b = unlabledEntities.remove(entity);

            //update classifier
            nb.updateClassifier(entity);

            if(i == this.stepList.get(stepListPointer) - 1) {
                System.out.println("Evaluation at step" + this.stepList.get(stepListPointer) + "...");
                Evaluation evaluation = nb.evaluation();
                map.put(this.stepList.get(stepListPointer), evaluation);
                stepListPointer++;
            }
        }
        return map;
    }

    public Map<Integer, Evaluation> evaluateActiveRandomForest() {
        int nRandomSelected = 50;
        int sampleSize = 500;
        int stepSize = 50;

        Map<Integer, Evaluation> map = new HashMap<>();

        //get entities
        List<TweetLabelEntity> labelEntities = this.labelCollection.find().into(new ArrayList<>());
        List<LabeledProcessedTextWrapper> unlabledEntities = this.parser.getLabeledProcessedTextWrapper(labelEntities);
        // parse labels
        unlabledEntities = this.selectRelevantLabels(unlabledEntities);
        //randomize
        Collections.shuffle(unlabledEntities);

        WekaRandomForestClassifier rf = new WekaRandomForestClassifier();

        List<LabeledProcessedTextWrapper> randomSubList = new ArrayList<>(unlabledEntities.subList(0, nRandomSelected));
        unlabledEntities.removeAll(randomSubList);
        rf.buildClassifier(randomSubList);

        int stepListPointer = 0;
        for (int i = nRandomSelected; i < this.stepList.get(this.stepList.size()-1);) {
            //get Entity
            Collections.shuffle(unlabledEntities);
            List<LabeledProcessedTextWrapper> sample = new ArrayList<>(unlabledEntities.subList(0, sampleSize));
            List<LabeledProcessedTextWrapper> toLabel = rf.getMostUncertain(sample, stepSize);
            unlabledEntities.removeAll(toLabel);

            Timer timer = new Timer();
            timer.start();
            rf.updateClassifier(toLabel);
            i+= stepSize;
            System.out.println(i);
            timer.end();

            if(i == this.stepList.get(stepListPointer)) {
                System.out.println("Evaluation at step" + this.stepList.get(stepListPointer) + "...");
                Evaluation evaluation = rf.evaluation();
                map.put(this.stepList.get(stepListPointer), evaluation);
                stepListPointer++;
            }
        }
        return map;
    }

    public Map<Integer, Integer[][]> evaluateActiveRandomForestWithHoldoutSet(List<Long> idForTestSetList) {
        int nRandomSelected = 50;
        int sampleSize = 500;
        int stepSize = 50;

        Map<Integer, Integer[][]> map = new HashMap<>();

        //get entities
        List<TweetLabelEntity> testSet = this.labelCollection.find(Filters.in("tweetId", idForTestSetList)).into(new ArrayList<>());
        List<TweetLabelEntity> trainSet = this.labelCollection.find(Filters.not(Filters.in("tweetId", idForTestSetList))).into(new ArrayList<>());

        List<LabeledProcessedTextWrapper> unlabledEntities = this.parser.getLabeledProcessedTextWrapper(trainSet);
        // parse labels
        unlabledEntities = this.selectRelevantLabels(unlabledEntities);
        List<LabeledProcessedTextWrapper> testSetWrapped = this.selectRelevantLabels(this.parser.getLabeledProcessedTextWrapper(testSet));
        //randomize
        Collections.shuffle(unlabledEntities);

        WekaRandomForestClassifier rf = new WekaRandomForestClassifier();

        List<LabeledProcessedTextWrapper> randomSubList = new ArrayList<>(unlabledEntities.subList(0, nRandomSelected));
        unlabledEntities.removeAll(randomSubList);
        rf.buildClassifier(randomSubList);

        int stepListPointer = 0;
        for (int i = nRandomSelected; i < this.stepList.get(this.stepList.size()-1);) {
            //get Entity
            Collections.shuffle(unlabledEntities);
            List<LabeledProcessedTextWrapper> sample = new ArrayList<>(unlabledEntities.subList(0, sampleSize));
            List<LabeledProcessedTextWrapper> toLabel = rf.getMostUncertain(sample, stepSize);
            unlabledEntities.removeAll(toLabel);

            Timer timer = new Timer();
            timer.start();
            rf.updateClassifier(toLabel);
            i+= stepSize;
            System.out.println(i);
            timer.end();

            if(i == this.stepList.get(stepListPointer)) {
                System.out.println("Evaluation at step" + this.stepList.get(stepListPointer) + "...");
                AbstractWekaIncrementalClassifier classifier = new WekaRandomForestClassifier();
                classifier.buildClassifier(rf.getTrainedEntities());

                int tp = 0, tn = 0, fn = 0, fp = 0;
                for(LabeledProcessedTextWrapper inst : testSetWrapped) {
                    String real = inst.getLabel();
                    double d = rf.classifyInstance(parser.getProcessedTextEntities(inst));
                    String classifiedAs = "bla";
                    if(d == 0.0d)
                        classifiedAs = "relevant";
                    if(d == 1.0d)
                        classifiedAs = "irrelevant";

                    if(real.equals(classifiedAs))
                        if(real.equals("relevant")) tp++;
                        else tn++;
                    else
                    if(real.equals("relevant")) fn++;
                    else fp++;
                }
                Integer[][] confMat = new Integer[2][2];
                confMat[0][0] = tp;
                confMat[0][1] = fp;
                confMat[1][0] = fn;
                confMat[1][1] = tn;
                map.put(this.stepList.get(stepListPointer), confMat);

                stepListPointer++;
            }
        }
        return map;
    }

    public Map<Integer, Evaluation> evaluateRandomForestRandomSampled() {

        Map<Integer, Evaluation> map = new HashMap<>();

        //get entities
        List<TweetLabelEntity> labelEntities = this.labelCollection.find().into(new ArrayList<>());
        List<LabeledProcessedTextWrapper> unlabledEntities = this.parser.getLabeledProcessedTextWrapper(labelEntities);
        // parse labels
        unlabledEntities = this.selectRelevantLabels(unlabledEntities);
        //randomize
        Collections.shuffle(unlabledEntities);

        //create RandomForest
        WekaRandomForestClassifier rf = new WekaRandomForestClassifier();

        List<LabeledProcessedTextWrapper> entityList = new ArrayList<>();
        int lastUnlabeledEntityPointer = 0;
        for(int p = 0; p < this.stepList.size(); p++) {
            System.out.println("Evaluating at " + this.stepList.get(p) + " steps...");
            List<LabeledProcessedTextWrapper> list = new ArrayList<>(unlabledEntities.subList(lastUnlabeledEntityPointer, this.stepList.get(p)));
            lastUnlabeledEntityPointer = this.stepList.get(p);
            entityList.addAll(list);

            rf.buildClassifier(entityList);
            Evaluation evaluation = rf.evaluation();
            map.put(this.stepList.get(p), evaluation);
        }

        return map;
    }

    public Map<Integer, Integer[][]> evaluateRandomForestRandomSampledWithHoldoutSet(List<Long> idForTestSetList) {
        Map<Integer, Integer[][]> map = new HashMap<>();

        //get entities
        List<TweetLabelEntity> testSet = this.labelCollection.find(Filters.in("tweetId", idForTestSetList)).into(new ArrayList<>());
        List<TweetLabelEntity> trainSet = this.labelCollection.find(Filters.not(Filters.in("tweetId", idForTestSetList))).into(new ArrayList<>());

        List<LabeledProcessedTextWrapper> unlabledEntities = this.parser.getLabeledProcessedTextWrapper(trainSet);
        // parse labels
        unlabledEntities = this.selectRelevantLabels(unlabledEntities);
        List<LabeledProcessedTextWrapper> testSetWrapped = this.selectRelevantLabels(this.parser.getLabeledProcessedTextWrapper(testSet));
        //randomize
        Collections.shuffle(unlabledEntities);


        //create RandomForest
        WekaRandomForestClassifier rf = new WekaRandomForestClassifier();

        List<LabeledProcessedTextWrapper> entityList = new ArrayList<>();
        int lastUnlabeledEntityPointer = 0;
        for(int p = 0; p < this.stepList.size(); p++) {
            List<LabeledProcessedTextWrapper> list = new ArrayList<>(unlabledEntities.subList(lastUnlabeledEntityPointer, this.stepList.get(p)));
            lastUnlabeledEntityPointer = this.stepList.get(p);
            entityList.addAll(list);

            System.out.println("Evaluation at step" + this.stepList.get(p) + "...");
            AbstractWekaIncrementalClassifier classifier = new WekaRandomForestClassifier();
            rf.buildClassifier(entityList);

            int tp = 0, tn = 0, fn = 0, fp = 0;
            for(LabeledProcessedTextWrapper inst : testSetWrapped) {
                String real = inst.getLabel();
                double d = rf.classifyInstance(parser.getProcessedTextEntities(inst));
                String classifiedAs = "bla";
                if(d == 0.0d)
                    classifiedAs = "relevant";
                if(d == 1.0d)
                    classifiedAs = "irrelevant";

                if(real.equals(classifiedAs))
                    if(real.equals("relevant")) tp++;
                    else tn++;
                else
                if(real.equals("relevant")) fn++;
                else fp++;
            }
            Integer[][] confMat = new Integer[2][2];
            confMat[0][0] = tp;
            confMat[0][1] = fp;
            confMat[1][0] = fn;
            confMat[1][1] = tn;
            map.put(this.stepList.get(p), confMat);
        }

        return map;
    }

    public Map<Integer, Evaluation> evaluateRandomForestKnnSampled() {
        int nRandomSelected = 50;
        int sampleSize = 200;

        Map<Integer, Evaluation> map = new HashMap<>();

        //get entities
        List<TweetLabelEntity> labelEntities = this.labelCollection.find().into(new ArrayList<>());
        List<LabeledProcessedTextWrapper> unlabledEntities = this.parser.getLabeledProcessedTextWrapper(labelEntities);
        // parse labels
        unlabledEntities = this.selectRelevantLabels(unlabledEntities);
        //randomize
        Collections.shuffle(unlabledEntities);

        //create new classifier
        WekaKnnClassifier nb = new WekaKnnClassifier();

        List<LabeledProcessedTextWrapper> randomSubList = new ArrayList<>(unlabledEntities.subList(0, nRandomSelected));
        unlabledEntities.removeAll(randomSubList);
        nb.buildClassifier(randomSubList);

        // i is the number of the step
        LabeledProcessedTextWrapper entity;
        int stepListPointer = 0;
        int unlabeledEntityPointer = 0;
        for(int i = nRandomSelected; i < this.stepList.get(this.stepList.size()-1); i++) {

            //get Entity
            Collections.shuffle(unlabledEntities);
            List<LabeledProcessedTextWrapper> sample = new ArrayList<>(unlabledEntities.subList(0, sampleSize));
            entity = nb.getMostUncertain(sample);
            boolean b = unlabledEntities.remove(entity);

            //update classifier
            nb.updateClassifier(entity);

            if(i == this.stepList.get(stepListPointer) - 1) {
                System.out.println("Evaluation at step" + this.stepList.get(stepListPointer) + "...");
                AbstractWekaIncrementalClassifier rf = new WekaRandomForestClassifier();
                rf.add(nb.getTrainedEntities());
                Evaluation evaluation = rf.evaluation();
                map.put(this.stepList.get(stepListPointer), evaluation);
                stepListPointer++;
            }
        }
        return map;
    }

    public Map<Integer, Integer[][]> evaluateRandomForestKnnSampledWithHoldoutSet(List<Long> idForTestSetList) {
        int nRandomSelected = 50;
        int sampleSize = 200;

        Map<Integer, Integer[][]> map = new HashMap<>();

        //get entities
        List<TweetLabelEntity> testSet = this.labelCollection.find(Filters.in("tweetId", idForTestSetList)).into(new ArrayList<>());
        List<TweetLabelEntity> trainSet = this.labelCollection.find(Filters.not(Filters.in("tweetId", idForTestSetList))).into(new ArrayList<>());

        List<LabeledProcessedTextWrapper> unlabledEntities = this.parser.getLabeledProcessedTextWrapper(trainSet);
        // parse labels
        unlabledEntities = this.selectRelevantLabels(unlabledEntities);
        List<LabeledProcessedTextWrapper> testSetWrapped = this.selectRelevantLabels(this.parser.getLabeledProcessedTextWrapper(testSet));
        //randomize
        Collections.shuffle(unlabledEntities);

        //create new classifier
        WekaKnnClassifier nb = new WekaKnnClassifier();

        //random n samples to begin with
        List<LabeledProcessedTextWrapper> randomSubList = new ArrayList<>(unlabledEntities.subList(0, nRandomSelected));
        unlabledEntities.removeAll(randomSubList);
        nb.buildClassifier(randomSubList);

        // i is the number of the step
        LabeledProcessedTextWrapper entity;
        int stepListPointer = 0;
        int unlabeledEntityPointer = 0;
        for(int i = nRandomSelected; i < this.stepList.get(this.stepList.size()-1); i++) {

            //get Entity
            Collections.shuffle(unlabledEntities);
            List<LabeledProcessedTextWrapper> sample = new ArrayList<>(unlabledEntities.subList(0, sampleSize));
            entity = nb.getMostUncertain(sample);
            boolean b = unlabledEntities.remove(entity);

            //update classifier
            nb.updateClassifier(entity);

            if(i == this.stepList.get(stepListPointer) - 1) {
                System.out.println("Evaluation at step" + this.stepList.get(stepListPointer) + "...");
                AbstractWekaIncrementalClassifier rf = new WekaRandomForestClassifier();
                rf.buildClassifier(nb.getTrainedEntities());

                int tp = 0, tn = 0, fn = 0, fp = 0;
                for(LabeledProcessedTextWrapper inst : testSetWrapped) {
                    String real = inst.getLabel();
                    double d = rf.classifyInstance(parser.getProcessedTextEntities(inst));
                    String classifiedAs = "bla";
                    if(d == 0.0d)
                        classifiedAs = "relevant";
                    if(d == 1.0d)
                        classifiedAs = "irrelevant";

                    if(real.equals(classifiedAs))
                        if(real.equals("relevant")) tp++;
                        else tn++;
                    else
                    if(real.equals("relevant")) fn++;
                    else fp++;
                }
                Integer[][] confMat = new Integer[2][2];
                confMat[0][0] = tp;
                confMat[0][1] = fp;
                confMat[1][0] = fn;
                confMat[1][1] = tn;

                System.out.println(tp + "  " + fp);
                System.out.println(fn + "  " + tn);
                map.put(this.stepList.get(stepListPointer), confMat);
                stepListPointer++;
            }
        }
        return map;
    }

    public Map<Integer, Evaluation> evaluateRandomForestHybrid() {
        int nRandomSelected = 50;
        int sampleSize = 500;
        int stepSizeActive = 25;
        int stepSizeRand = 25;
        int stepSize = stepSizeActive + stepSizeRand;

        Map<Integer, Evaluation> map = new HashMap<>();

        //get entities
        List<TweetLabelEntity> labelEntities = this.labelCollection.find().into(new ArrayList<>());
        List<LabeledProcessedTextWrapper> unlabledEntities = this.parser.getLabeledProcessedTextWrapper(labelEntities);
        // parse labels
        unlabledEntities = this.selectRelevantLabels(unlabledEntities);
        //randomize
        Collections.shuffle(unlabledEntities);

        WekaRandomForestClassifier rf = new WekaRandomForestClassifier();

        List<LabeledProcessedTextWrapper> randomSubList = new ArrayList<>(unlabledEntities.subList(0, nRandomSelected));
        unlabledEntities.removeAll(randomSubList);
        rf.buildClassifier(randomSubList);

        int stepListPointer = 0;
        for (int i = nRandomSelected; i < this.stepList.get(this.stepList.size()-1);) {
            //get active part
            Collections.shuffle(unlabledEntities);
            List<LabeledProcessedTextWrapper> sample = new ArrayList<>(unlabledEntities.subList(0, sampleSize));
            List<LabeledProcessedTextWrapper> toLabel = rf.getMostUncertain(sample, stepSizeActive);
            unlabledEntities.removeAll(toLabel);

            //get random part
            Collections.shuffle(unlabledEntities);
            toLabel.addAll(unlabledEntities.subList(0, stepSizeRand));
            unlabledEntities.removeAll(unlabledEntities.subList(0, stepSizeRand));

            Timer timer = new Timer();
            rf.updateClassifier(toLabel);
            i+= stepSize;

            if(i == this.stepList.get(stepListPointer)) {
                System.out.println("Evaluation at step" + this.stepList.get(stepListPointer) + "...");
                Evaluation evaluation = rf.evaluation();
                map.put(this.stepList.get(stepListPointer), evaluation);
                stepListPointer++;
            }
        }
        return map;
    }

    public Map<Integer, Integer[][]> evaluateRandomForestHybridWithHoldoutSet(List<Long> idForTestSetList) {
        int nRandomSelected = 50;
        int sampleSize = 500;
        int stepSizeActive = 25;
        int stepSizeRand = 25;
        int stepSize = stepSizeActive + stepSizeRand;

        Map<Integer, Integer[][]> map = new HashMap<>();

        //get entities
        List<TweetLabelEntity> testSet = this.labelCollection.find(Filters.in("tweetId", idForTestSetList)).into(new ArrayList<>());
        List<TweetLabelEntity> trainSet = this.labelCollection.find(Filters.not(Filters.in("tweetId", idForTestSetList))).into(new ArrayList<>());

        List<LabeledProcessedTextWrapper> unlabledEntities = this.parser.getLabeledProcessedTextWrapper(trainSet);
        // parse labels
        unlabledEntities = this.selectRelevantLabels(unlabledEntities);
        List<LabeledProcessedTextWrapper> testSetWrapped = this.selectRelevantLabels(this.parser.getLabeledProcessedTextWrapper(testSet));
        //randomize
        Collections.shuffle(unlabledEntities);

        WekaRandomForestClassifier rf = new WekaRandomForestClassifier();

        List<LabeledProcessedTextWrapper> randomSubList = new ArrayList<>(unlabledEntities.subList(0, nRandomSelected));
        unlabledEntities.removeAll(randomSubList);
        rf.buildClassifier(randomSubList);

        int stepListPointer = 0;
        for (int i = nRandomSelected; i < this.stepList.get(this.stepList.size()-1);) {
            //get active part
            Collections.shuffle(unlabledEntities);
            List<LabeledProcessedTextWrapper> sample = new ArrayList<>(unlabledEntities.subList(0, sampleSize));
            List<LabeledProcessedTextWrapper> toLabel = rf.getMostUncertain(sample, stepSizeActive);
            unlabledEntities.removeAll(toLabel);

            //get random part
            Collections.shuffle(unlabledEntities);
            toLabel.addAll(unlabledEntities.subList(0, stepSizeRand));
            unlabledEntities.removeAll(unlabledEntities.subList(0, stepSizeRand));

            Timer timer = new Timer();
            rf.updateClassifier(toLabel);
            i+= stepSize;

            if(i == this.stepList.get(stepListPointer)) {
                System.out.println("Evaluation at step" + this.stepList.get(stepListPointer) + "...");
                AbstractWekaIncrementalClassifier cll = new WekaRandomForestClassifier();
                cll.buildClassifier(rf.getTrainedEntities());

                int tp = 0, tn = 0, fn = 0, fp = 0;
                for(LabeledProcessedTextWrapper inst : testSetWrapped) {
                    String real = inst.getLabel();
                    double d = rf.classifyInstance(parser.getProcessedTextEntities(inst));
                    String classifiedAs = "bla";
                    if(d == 0.0d)
                        classifiedAs = "relevant";
                    if(d == 1.0d)
                        classifiedAs = "irrelevant";

                    if(real.equals(classifiedAs))
                        if(real.equals("relevant")) tp++;
                        else tn++;
                    else
                    if(real.equals("relevant")) fn++;
                    else fp++;
                }
                Integer[][] confMat = new Integer[2][2];
                confMat[0][0] = tp;
                confMat[0][1] = fp;
                confMat[1][0] = fn;
                confMat[1][1] = tn;
                map.put(this.stepList.get(stepListPointer), confMat);
                stepListPointer++;
            }
        }
        return map;
    }

    public static void compareSamplingTechnique(Properties tweetProperties, Properties runProperties, int executionCount) {
        Properties properties = PropertyHandler.mergeProperties(tweetProperties, runProperties);

        boolean rfKnnSampling = true;
        boolean rfHybridSampling = true;
        boolean rfRandomSampling = true;
        boolean activeRandomForest = true;

        List<List<Long>> testIdListList = new ArrayList<>();

        for(int i = 0; i < executionCount; i++) {
            List<TweetLabelEntity> labelEntities = MongoHelper.getInstance()
                    .getCollection(properties.getProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME), TweetLabelEntity.class)
                    .aggregate(Arrays.asList(sample(1000)))
                    .into(new ArrayList<>());

            List<Long> testIdList = labelEntities.stream()
                    .map(TweetLabelEntity::getTweetId)
                    .collect(Collectors.toList());

            System.out.println("Positives: " + labelEntities.stream().filter(e -> {
                String s = e.getLabel();
                return s.equals("4");
            }).count());
            testIdListList.add(testIdList);
        }

        System.out.println();

        if(rfKnnSampling) {
            Function<Integer, Map<Integer, Integer[][]>> func = (Integer i) -> {
                List<List<Long>> tmp = new ArrayList<>(testIdListList);
                ClassificationEvaluator evaluator = new ClassificationEvaluator(properties);
                Map<Integer, Integer[][]> m = evaluator.evaluateRandomForestKnnSampledWithHoldoutSet(testIdListList.get(i));
                return m;
            };
            evaluateMultipleTimesWithHoldout(func, executionCount);
        }

        if(rfHybridSampling) {
            Function<Integer, Map<Integer, Integer[][]>> func = (Integer i) -> {
                List<List<Long>> tmp = new ArrayList<>(testIdListList);
                ClassificationEvaluator evaluator = new ClassificationEvaluator(properties);
                Map<Integer, Integer[][]> m = evaluator.evaluateRandomForestHybridWithHoldoutSet(testIdListList.get(i));
                return m;
            };
            evaluateMultipleTimesWithHoldout(func, executionCount);
        }

        if(rfRandomSampling) {
            Function<Integer, Map<Integer, Integer[][]>> func = (Integer i) -> {
                List<List<Long>> tmp = new ArrayList<>(testIdListList);
                ClassificationEvaluator evaluator = new ClassificationEvaluator(properties);
                Map<Integer, Integer[][]> m = evaluator.evaluateRandomForestRandomSampledWithHoldoutSet(testIdListList.get(i));
                return m;
            };
            evaluateMultipleTimesWithHoldout(func, executionCount);
        }

        if(activeRandomForest) {
            Function<Integer, Map<Integer, Integer[][]>> func = (Integer i) -> {
                List<List<Long>> tmp = new ArrayList<>(testIdListList);
                ClassificationEvaluator evaluator = new ClassificationEvaluator(properties);
                Map<Integer, Integer[][]> m = evaluator.evaluateActiveRandomForestWithHoldoutSet(testIdListList.get(i));
                return m;
            };
            evaluateMultipleTimesWithHoldout(func, executionCount);
        }
    }

    public static void compareActiveClassifier(Properties tweetProperties, Properties runProperties, int executionCount) {
        Properties properties = PropertyHandler.mergeProperties(tweetProperties, runProperties);
//        Properties properties =  new Properties();
//        properties.setProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME, main.CollectionName.DECEMBER_2019_TWEETS);
//        properties.setProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME, main.CollectionName.DECEMBER_2019_TRISTAN_LABELS);
//        properties.setProperty(CollectionNameProperties.PROCESSED_TEXT_ENTITY_COLLECTION_NAME, main.CollectionName.DECEMBER_2019_TOKEN);
//        properties.setProperty(CollectionNameProperties.USER_ENTITY_COLLECTION_NAME, "user-base");
//        properties.setProperty(STEPS, "100,200,300,400,500,600,700,800,900,1000");
//        properties.setProperty(AbstractWekaIncrementalClassifier.LABELS, "relevant,irrelevant");

        Map<Integer, List<Evaluation>> map;
        boolean rfKnnSampling = false;
        boolean rfHybridSampling = false;
        boolean rfRandomSampling = false;
        boolean activeRandomForest = true;
        boolean activeKnn = true;
        boolean activeBayes = true;

        if(rfKnnSampling) {
            System.out.println("Evaluation Active Random Forest kNN sampling");
            map = evaluateMultipleTimes(() -> {
                ClassificationEvaluator evaluator = new ClassificationEvaluator(properties);
                return evaluator.evaluateRandomForestKnnSampled();
            }, executionCount);
            printEvaluationAverageEvaluation(map);
            printEvalutationStatistics(map);
        }

        if(rfHybridSampling) {
            System.out.println("Evaluation Active Random Forest Hybrid sampling");
            map = evaluateMultipleTimes(() -> {
                ClassificationEvaluator evaluator = new ClassificationEvaluator(properties);
                return evaluator.evaluateRandomForestHybrid();
            }, executionCount);
            printEvaluationAverageEvaluation(map);
            printEvalutationStatistics(map);
        }

        if(rfRandomSampling) {
            System.out.println("Evaluation Active Random Forest Random sampling");
            map = evaluateMultipleTimes(() -> {
                ClassificationEvaluator evaluator = new ClassificationEvaluator(properties);
                return evaluator.evaluateRandomForestRandomSampled();
            }, executionCount);
            printEvaluationAverageEvaluation(map);
            printEvalutationStatistics(map);
        }

        if(activeRandomForest) {
            System.out.println("Evaluation Active Random Forest");
            map = evaluateMultipleTimes(() -> {
                ClassificationEvaluator evaluator = new ClassificationEvaluator(properties);
                return evaluator.evaluateActiveRandomForest();
            }, executionCount);
            printEvaluationAverageEvaluation(map);
            printEvalutationStatistics(map);
        }

        if(activeKnn) {
            System.out.println("Evaluation Active knn");
            map = evaluateMultipleTimes(() -> {
                ClassificationEvaluator evaluator = new ClassificationEvaluator(properties);
                return evaluator.evaluateActiveKnn();
            }, executionCount);
            printEvaluationAverageEvaluation(map);
            printEvalutationStatistics(map);
        }

        if(activeBayes) {
            System.out.println("Evaluation Active Bayes");
            map = evaluateMultipleTimes(() -> {
                ClassificationEvaluator evaluator = new ClassificationEvaluator(properties);
                return evaluator.evaluateActiveNaiveBayes();
            }, executionCount);
            printEvaluationAverageEvaluation(map);
            printEvalutationStatistics(map);
        }
    }

    private static void printEvaluationAverageEvaluation( Map<Integer, List<Evaluation>> map) {
        List<Map.Entry<Integer, List<Evaluation>>> list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparingInt(Map.Entry::getKey));
        list.forEach(e -> {
            System.out.println(e.getKey() + "," + e.getValue().stream().mapToDouble(entity -> entity.areaUnderROC(0)).average().getAsDouble());
        });
    }

    private static void printEvalutationStatistics( Map<Integer, List<Evaluation>> map) {
        List<Map.Entry<Integer, List<Evaluation>>> list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparingInt(Map.Entry::getKey));
        list.forEach(entity -> System.out.println(entity.getKey() + "," +
                entity.getValue().stream().mapToDouble(e -> e.precision(0)).average().getAsDouble() + "," +
                entity.getValue().stream().mapToDouble(e -> e.recall(0)).average().getAsDouble() + "," +
                entity.getValue().stream().mapToDouble(e -> e.fMeasure(0)).average().getAsDouble()
        ));
    }

    private static Map<Integer, List<Evaluation>> evaluateMultipleTimes(Supplier<Map<Integer, Evaluation>> supplier, int n) {
        Map<Integer, List<Evaluation>> map = new HashMap<>();
        for(int i = 0; i < n; i++) {
            for (Map.Entry<Integer, Evaluation> entry : supplier.get().entrySet()) {
                if (!map.containsKey(entry.getKey()))
                    map.put(entry.getKey(), new ArrayList<>());
                map.get(entry.getKey()).add(entry.getValue());
            }
        }
        return map;
    }

    private static void evaluateMultipleTimesWithHoldout(Function<Integer, Map<Integer, Integer[][]>> f, int n) {
        Map<Integer, List<Map<String, Double>>> map = new HashMap<>();
        for(int i = 0; i < n; i++) {
            for(Map.Entry<Integer, Integer[][]> e : f.apply(i).entrySet()) {
                int key = e.getKey();
                Map<String, Double> value = getStatsFromConfMatrix(e.getValue());
                if(!map.containsKey(key))
                    map.put(key, new ArrayList<>());
                map.get(key).add(value);
            }
        }
        printEvalualtionStatisticsWithHoldout(map);
    }

    private static Map<String,Double> getStatsFromConfMatrix(Integer[][] confMat) {
        int tp = confMat[0][0];
        int fp = confMat[0][1];
        int fn = confMat[1][0];
        int tn = confMat[1][1];
        Map<String, Double> map = new HashMap<>();
        double precision = (double) tp / (double)(tp + fp);
        double recall = (double) tp / (double) (tp + fn);
        map.put("precision", precision);
        map.put("recall", recall);
        map.put("f1", 2 * ((precision*recall)/(precision+recall)));
        return map;
    }

    public static void printEvalualtionStatisticsWithHoldout(Map<Integer, List<Map<String, Double>>> map) {
        System.out.println("steps,precision,recall,f1");
        List<Map.Entry<Integer, List<Map<String, Double>>>> aList = new ArrayList<>(map.entrySet());
        aList.sort(Comparator.comparingInt(Map.Entry::getKey));
        int count;
        double precisionAcc, recallAcc, f1Acc;
        for(Map.Entry<Integer, List<Map<String, Double>>> entry : aList) {
            count = 0;
            precisionAcc = 0.0d;
            recallAcc = 0.0d;
            f1Acc = 0.0d;
            int key = entry.getKey();
            for(Map<String, Double> stat : entry.getValue()) {
                precisionAcc += Double.isNaN(stat.get("precision")) ? 0.0d : stat.get("precision");
                recallAcc += stat.get("recall");
                f1Acc += Double.isNaN(stat.get("f1")) ? 0.0d : stat.get("f1");
                count++;
            }
            precisionAcc /= count;
            recallAcc /= count;
            f1Acc /= count;
            System.out.println(key + "," + precisionAcc + "," + recallAcc + "," + f1Acc);
        }
    }
}
