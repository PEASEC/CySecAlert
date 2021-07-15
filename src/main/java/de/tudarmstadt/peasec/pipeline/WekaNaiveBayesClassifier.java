package de.tudarmstadt.peasec.pipeline;

import de.tudarmstadt.peasec.entity.IProcessedText;
import de.tudarmstadt.peasec.entity.help.LabeledProcessedTextWrapper;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instance;
import weka.filters.Filter;

import java.util.List;
import java.util.Properties;

public class WekaNaiveBayesClassifier extends AbstractWekaIncrementalClassifier {

    public WekaNaiveBayesClassifier() {
        super();
    }

    public WekaNaiveBayesClassifier(Properties properties) {
        super(properties);
    }

    @Override
    public Classifier getEmptyClassifier() {
        return new NaiveBayes();
    }

    @Override
    public <T extends IProcessedText> T getMostUncertain(List<T> entities) {
        T mostUncertainEntity = null;
        double leastUncertainProb = 1.0d;
        try {
            for(T entity : entities) {
                Instance instance = this.getUnlabeledInstances(entity).firstInstance();
                double[] probs = this.classifier.distributionForInstance(instance);
                double prob = getHighestDouble(probs);
                if(prob < leastUncertainProb) {
                    leastUncertainProb = prob;
                    mostUncertainEntity = entity;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mostUncertainEntity;
    }

    private static double getHighestDouble(double[] arr) {
        double max = Double.MIN_VALUE;
        for(double d : arr) {
            if(d > max)
                max = d;
        }
        return max;
    }
}
