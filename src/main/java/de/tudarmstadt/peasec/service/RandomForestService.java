package de.tudarmstadt.peasec.service;

import de.tudarmstadt.peasec.entity.help.LabeledProcessedTextWrapper;
import de.tudarmstadt.peasec.service.wrapper.EvalResults;
import de.tudarmstadt.peasec.util.Timer;
import de.tudarmstadt.peasec.util.config.UrlConfigurations;
import edu.stanford.nlp.util.StringUtils;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.*;
import weka.core.converters.ArffLoader;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static weka.core.Utils.splitOptions;

public class RandomForestService {

    public static final String RANDOM_FOREST_RELATION_NAME = "RandomForest.RelationName";
    public static final String LABELS = "RandomForest.Labels";

    Classifier classifier;

    String testUrl, trainUrl;

    String modelUrl;

    String relationName;

    List<String> labels;

    private ArrayList<Attribute> attributeList;

    public RandomForestService() {}

    public RandomForestService(Properties properties) {
        this.testUrl = properties.getProperty(UrlConfigurations.CLASSIFIER_TEST_DATA_URL, "");
        this.trainUrl = properties.getProperty(UrlConfigurations.CLASSIFIER_TRAINING_DATA_URL);
        this.modelUrl = properties.getProperty(UrlConfigurations.CLASSIFIER_MODEL_URL, "");
        this.relationName = properties.getProperty(RANDOM_FOREST_RELATION_NAME, "DefaultRelation");
        this.labels = Arrays.asList(properties.getProperty(LABELS, "").split(","));
    }

    //----- Getter & Setter --------------------------------------------------------------------------------------------

    public String getTestUrl() {
        return testUrl;
    }

    public void setTestUrl(String testUrl) {
        this.testUrl = testUrl;
    }

    public String getTrainUrl() {
        return trainUrl;
    }

    public void setTrainUrl(String trainUrl) {
        this.trainUrl = trainUrl;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    //----- Create & Read Arff-Files ------------------------------------------------------------------------------------------

    public void setTrainAndTestData(List<LabeledProcessedTextWrapper> instanceList, double trainFraction) {
        int limit = (int) (instanceList.size() * trainFraction);
        Collections.shuffle(instanceList);
        this.setTestData(instanceList.subList(0, limit));
        this.setTrainingData(instanceList.subList(limit, instanceList.size()));
    }

    public void setTrainingData(List<LabeledProcessedTextWrapper> instanceList) {
        Path path = Paths.get(this.trainUrl);
        this.createArffFile(instanceList, path);
    }

    public void setTestData(List<LabeledProcessedTextWrapper> instanceList) {
        Path path = Paths.get(this.testUrl);
        this.createArffFile(instanceList, path);

    }

    private void createArffFile(List<LabeledProcessedTextWrapper> instanceList, Path path) {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("@RELATION " + this.relationName);
            writer.newLine();
            writer.newLine();

            writer.write("@ATTRIBUTE text STRING");
            writer.newLine();
            writer.write("@ATTRIBUTE Label {" + StringUtils.join(this.labels, ",") + "}");
            writer.newLine();
            writer.newLine();

            writer.write("@DATA");
            writer.newLine();

            String text, label;
            long tweetId;
            for(LabeledProcessedTextWrapper instance : instanceList) {
                text = instance.getText();
                label = instance.getLabel();
                tweetId = instance.getTweetId();
                writer.write("\'" + text + "\'," + label + "\'," + tweetId);
                writer.newLine();
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private Instances getInstancesFromArffFile(String url) {
        Instances out = null;
        try (Reader r = new BufferedReader(new FileReader(url))) {
            ArffLoader.ArffReader arffReader = new ArffLoader.ArffReader(r);
            out = arffReader.getData();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return out;
    }

    //----- Training & Evaluation --------------------------------------------------------------------------------------

    private void initClassifier(Classifier classifier) {
        StringToWordVector filter = new StringToWordVector();
        filter.setAttributeIndices("first");
        FilteredClassifier outClassifier = new FilteredClassifier();
        outClassifier.setFilter(filter);
        outClassifier.setClassifier(classifier);
        this.classifier =  outClassifier;
    }

    public void evaluate() {
        // Load Instances from Arff-Files
        Instances trainInstances = this.getInstancesFromArffFile(this.trainUrl);
        trainInstances.setClassIndex(1);

        RandomForest rf = new RandomForest();
        try {
            String[] options = splitOptions("-num-slots 0");
            rf.setOptions(options);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        this.initClassifier(new RandomForest());

        try {
            Timer timer = new Timer();
            timer.start();
            Evaluation evaluation = new Evaluation(trainInstances);
            evaluation.crossValidateModel(this.classifier, trainInstances, 4, new Random(1));
            timer.end();
            System.out.println(evaluation.toSummaryString());
            System.out.println(evaluation.toMatrixString("=== Confusion Matrix ===\n"));
            System.out.println("AUC: " + evaluation.areaUnderROC(0));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void train() {
        // Load Instances from Arff-Files
        Instances trainInstances = this.getInstancesFromArffFile(this.trainUrl);
        trainInstances.setClassIndex(1);

        // build classifier
        this.initClassifier(new RandomForest());
        try {
            this.classifier.buildClassifier(trainInstances);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public EvalResults calculateTestResults(double positive) {
        Instances testInstances = this.getInstancesFromArffFile(this.testUrl);

        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;
        int positives = 0;
        int negatives = 0;
        try {
            Timer timer = null;
            for(int i = 0; i < testInstances.size(); i++) {
                if(i % 100 == 0) {
                    if (timer != null)
                        timer.end();
                    timer = new Timer();
                    timer.start();
                }
                double pred = this.classify(testInstances.instance(i).stringValue(0));
                double real = testInstances.instance(i).value(1);

                if(real == positive)
                    positives++;
                else
                    negatives++;

                if(pred == positive && real == positive)
                    tp++;
                else if(pred == positive && real != positive)
                    fp++;
                else if(pred != positive && real == positive)
                    fn++;
                else if(pred != positive && real != positive)
                    tn++;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        EvalResults evalResults = new EvalResults();
        evalResults.setTp(tp);
        evalResults.setFp(fp);
        evalResults.setTn(tn);
        evalResults.setFn(fn);

        return evalResults;
    }

    public double classify(String s) {
        ArrayList<Attribute> attributeList = this.getAttributeList();

        Instances instances = new Instances("Test relation", attributeList, 1);
        instances.setClassIndex(1);
        DenseInstance instance = new DenseInstance(2);
        instance.setValue(attributeList.get(0), s);
        instances.add(instance);

        double pred = -1;
        try {
            pred = this.classifier.classifyInstance(instances.instance(0));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return pred;
    }

    private ArrayList<Attribute> getAttributeList() {
        if(this.attributeList == null) {
            FastVector fvNominalVal = new FastVector(this.labels.size());
            for (String label : this.labels)
                fvNominalVal.addElement(label);
            Attribute attribute1 = new Attribute("text", (FastVector) null);
            Attribute attribute2 = new Attribute("Label", fvNominalVal);
            // Create list of instances with one element
            ArrayList<Attribute> fvWekaAttributes = new ArrayList<>(2);
            fvWekaAttributes.add(attribute1);
            fvWekaAttributes.add(attribute2);
            this.attributeList = fvWekaAttributes;
        }
        return this.attributeList;
    }

}
