package de.tudarmstadt.peasec.pipeline;

import de.tudarmstadt.peasec.entity.IProcessedText;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Instance;

import java.util.List;

public class WekaKnnClassifier extends AbstractWekaIncrementalClassifier {
    @Override
    public Classifier getEmptyClassifier() {
        return new IBk(50);
    }

    @Override
    public <T extends IProcessedText> T getMostUncertain(List<T> entities) {
        T mostUncertainEntity = null;
        double mostUncertainProb = 1.0d;
        try {
            for(T entity : entities) {
                Instance instance = this.getUnlabeledInstances(entity).firstInstance();
                double[] probs = this.classifier.distributionForInstance(instance);
                double prob = getHighestDouble(probs);
                if(prob < mostUncertainProb) {
                    mostUncertainProb = prob;
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
