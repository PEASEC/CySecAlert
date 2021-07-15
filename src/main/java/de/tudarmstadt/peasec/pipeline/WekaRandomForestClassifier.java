package de.tudarmstadt.peasec.pipeline;

import de.tudarmstadt.peasec.entity.IProcessedText;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;

import java.util.*;

import static weka.core.Utils.splitOptions;

public class WekaRandomForestClassifier extends AbstractWekaIncrementalClassifier {
    @Override
    public Classifier getEmptyClassifier() {
        return new RandomForest();
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

    public <T extends IProcessedText> List<T> getMostUncertain(List<T> entities, int n) {
        Map<Double, List<T>> map = new HashMap<>();
        try {
            for(T entity : entities) {
                Instance instance = this.getUnlabeledInstances(entity).firstInstance();
                double[] probs = this.classifier.distributionForInstance(instance);
                double prob = getHighestDouble(probs);
                if(!map.containsKey(prob))
                    map.put(prob, new ArrayList<>());
                map.get(prob).add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<Double> aList = new ArrayList<>(map.keySet());
        aList.sort(Double::compareTo);
        List<T> out = new ArrayList<>();
        for(Double d : aList) {
            int count = map.get(d).size();
            if(count < 50 - out.size())
                out.addAll(map.get(d));
            else {
                out.addAll(new ArrayList<>(map.get(d).subList(0, 50 - out.size())));
                break;
            }
        }
        return out;
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
