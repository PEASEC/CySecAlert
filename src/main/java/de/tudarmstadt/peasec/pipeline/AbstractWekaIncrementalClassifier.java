package de.tudarmstadt.peasec.pipeline;

import de.tudarmstadt.peasec.entity.IProcessedText;
import de.tudarmstadt.peasec.entity.ProcessedTextEntity;
import de.tudarmstadt.peasec.entity.help.LabeledProcessedTextWrapper;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.*;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.util.*;

public abstract class AbstractWekaIncrementalClassifier {
    public static final String LABELS = "WekaIncrementalClassifier.Labels";

    private ArrayList<Attribute> _cached_attributeList;

    private List<String> labels;

    private List<LabeledProcessedTextWrapper> trainedEntities;

    protected Classifier classifier;


    public AbstractWekaIncrementalClassifier() {
        this.labels = Arrays.asList("relevant", "irrelevant");
        this.trainedEntities = new ArrayList<>();
        this.classifier = this.buildFilteredClassifier(this.getEmptyClassifier());
    }

    public AbstractWekaIncrementalClassifier(Properties properties) {
        this();
        this.labels = Arrays.asList(properties.getProperty(LABELS, "relevant,irrelevant").split(","));
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<LabeledProcessedTextWrapper> getTrainedEntities() {
        return new ArrayList<>(trainedEntities);
    }

    public abstract Classifier getEmptyClassifier();

    public abstract <T extends IProcessedText> T getMostUncertain(List<T> entities);

    public double classifyInstance(ProcessedTextEntity entity) {
        Instances instances = this.getUnlabeledInstances(entity);
        double d = -1.0d;
        try {
            d = this.classifier.classifyInstance(instances.get(0));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return d;
    }

    public Evaluation evaluation() {
        return this.evaluate("");
    }

    public Evaluation evaluate(String optionString) {
        Evaluation evaluation = null;
        try {
            Instances instances = this.getLabeledInstances(this.trainedEntities);
            evaluation = new Evaluation(instances);
            evaluation.crossValidateModel(this.classifier, instances, 4, new Random(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return evaluation;
    }

    public void buildClassifier(List<LabeledProcessedTextWrapper> entities) {
        System.out.println("Building classifier...");

        //reset and write trainedEntities
        this.trainedEntities = new ArrayList<>();
        this.trainedEntities.addAll(entities);

        //init and build classifier
        this.classifier = this.buildFilteredClassifier(this.getEmptyClassifier());
        try {
            Instances instances = this.getLabeledInstances(entities);
            this.classifier.buildClassifier(instances);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Done.");
    }

    public void updateClassifier(LabeledProcessedTextWrapper entity) {
        this.trainedEntities.add(entity);
        try {
            this.classifier.buildClassifier(this.getLabeledInstances(this.trainedEntities));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateClassifier(List<LabeledProcessedTextWrapper> entities) {
        this.trainedEntities.addAll(entities);
        try {
            this.classifier.buildClassifier(this.getLabeledInstances(this.trainedEntities));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    protected Instances getUnlabeledInstances(ProcessedTextEntity entity) {
        Instances instances = new Instances("Data", this.getAttributeList(), 1);
        instances.setClassIndex(1);
        instances.add(this.getUnlabeledInstance(entity));
        return instances;
    }

    protected Instances getUnlabeledInstances(IProcessedText entity) {
        List<IProcessedText> wrapperList = new ArrayList<>(1);
        wrapperList.add(entity);
        return this.getUnlabeledInstances(wrapperList);
    }

    protected Instances getUnlabeledInstances(List<IProcessedText> entities) {
        Instances instances = new Instances("Data", this.getAttributeList(), entities.size());
        instances.setClassIndex(1);
        for(IProcessedText e : entities) {
            instances.add(this.getUnlabeledInstance(e));
        }
        return instances;
    }

    protected Instances getLabeledInstances(LabeledProcessedTextWrapper entity) {
        List<LabeledProcessedTextWrapper> wrapperList = new ArrayList<>(1);
        wrapperList.add(entity);
        return this.getLabeledInstances(wrapperList);
    }

    protected Instances getLabeledInstances(List<LabeledProcessedTextWrapper> entities) {
        Instances instances = new Instances("Data", this.getAttributeList(), entities.size());
        instances.setClassIndex(1);
        for(LabeledProcessedTextWrapper e : entities) {
            instances.add(this.getLabeledInstance(e));
        }
        return instances;
    }

    protected DenseInstance getUnlabeledInstance(IProcessedText textEntity) {
        DenseInstance instance = new DenseInstance(2);
        instance.setValue(this.getAttributeList().get(0), textEntity.getText());
        return instance;
    }

    protected DenseInstance getLabeledInstance(LabeledProcessedTextWrapper entitiy) {
        DenseInstance instance = new DenseInstance(2);
        instance.setValue(this.getAttributeList().get(0), entitiy.getText());
        instance.setValue(this.getAttributeList().get(1), entitiy.getLabel());
        return instance;
    }

    private ArrayList<Attribute> getAttributeList() {
        if(this._cached_attributeList == null) {
            FastVector fvNominalVal = new FastVector(this.labels.size());
            for (String label : this.labels)
                fvNominalVal.addElement(label);
            Attribute attribute1 = new Attribute("text", (FastVector) null);
            Attribute attribute2 = new Attribute("Label", fvNominalVal);
            // Create list of instances with one element
            ArrayList<Attribute> fvWekaAttributes = new ArrayList<>(2);
            fvWekaAttributes.add(attribute1);
            fvWekaAttributes.add(attribute2);
            this._cached_attributeList = fvWekaAttributes;
        }
        return this._cached_attributeList;
    }

    private Classifier buildFilteredClassifier(Classifier classifier) {
        StringToWordVector filter = new StringToWordVector();
        filter.setAttributeIndices("first");
        FilteredClassifier outClassifier = new FilteredClassifier();
        outClassifier.setFilter(filter);
        outClassifier.setClassifier(classifier);
        return outClassifier;
    }

    public void add(List<LabeledProcessedTextWrapper> l) {
        this.trainedEntities.addAll(l);
    }
}
