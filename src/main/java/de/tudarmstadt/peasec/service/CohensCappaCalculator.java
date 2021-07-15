package de.tudarmstadt.peasec.service;

import de.tudarmstadt.peasec.entity.TweetEntity;
import de.tudarmstadt.peasec.entity.TweetLabelEntity;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CohensCappaCalculator {

    private List<String> labels = new ArrayList<>();

    public CohensCappaCalculator(List<String> labels) {
        this.setLabels(labels);
    }

    public void calculate(List<TweetLabelEntity> firstLabelEntities, List<TweetLabelEntity> secondLabelEntities) {
        Map<Long, TweetLabelEntity> firstIdMap, secondIdMap;
        firstIdMap = genereteIdMap(firstLabelEntities);
        secondIdMap = genereteIdMap(secondLabelEntities);

        int[][] confMat = this.getConfMat(firstIdMap, secondIdMap);
        this.printConfMat(confMat);

        this.printCappa(confMat);
    }

    public int[][] getConfMat(Map<Long, TweetLabelEntity> map1, Map<Long,TweetLabelEntity> map2) {
        int[][] confMat = new int[this.labels.size()][this.labels.size()];

        TweetLabelEntity e1, e2;
        int missedCount = 0;
        Set<Long> idSet = Stream.concat(this.getIdSet(map1).stream(), this.getIdSet(map2).stream()).collect(Collectors.toSet());
        for(long id : idSet) {
            e1 = map1.get(id);
            e2 = map2.get(id);

            // next if not in both
            if (e1 == null || e2 == null || this.labelToId(e1.getLabel()) == -1 || this.labelToId(e2.getLabel()) == -1) {
                missedCount++;
                continue;
            }

            confMat[this.labelToId(e1.getLabel())][this.labelToId(e2.getLabel())]++;
        }

        System.out.println("CohensCappaCalculator::getConfMat: " + missedCount + "entities were not in both sets!");
        return confMat;
    }

    public void printConfMat(int[][] confMat) {
        String head = "";
        for(int i = 0; i < this.labels.size(); i++) {
            head += this.labels.get(i) + "   ";
        }
        System.out.println(head);

        for(int i = 0; i < this.labels.size(); i++) {
            String s = "";
            for(int j = 0; j < this.labels.size(); j++) {
                s += confMat[j][i] + "   ";

            }
            System.out.println(s);
        }
    }

    public void printCappa(int[][] confMat) {
        double p0, pc;
        int sum = 0, diagSum = 0;
        Map<Integer, Long> aMap = new HashMap<>();
        Map<Integer, Long> bMap = new HashMap<>();
        for(int i = 0; i < this.labels.size(); i++) {
            aMap.put(i, 0l);
            bMap.put(i, 0l);
        }

        for(int i = 0; i < this.labels.size(); i++) {
            for(int j = 0; j < this.labels.size(); j++) {
                int count = confMat[j][i];
                sum += count;
                aMap.put(i, aMap.get(i)+count);
                bMap.put(j, bMap.get(j)+count);
                if(i == j)
                    diagSum += count;
            }
        }

        p0 = (double) diagSum / (double) sum;
        pc = 0;
        for(int i = 0; i < this.labels.size(); i++)
            pc += aMap.get(i) * bMap.get(i);
        pc /= (double) (sum * sum);
        double kappa = (p0 - pc) / (1 - pc);

        System.out.println("Kappa: " + kappa);
    }

    public int labelToId(String s) {
        return this.labels.indexOf(s);
    }

    public void setLabels(List<String> labels) {
        this.labels = new ArrayList<>(labels);
    }

    public List<String> getLabels(List<TweetLabelEntity> entities) {
        return this.labels;
    }

    public Map<Long, TweetLabelEntity> genereteIdMap(List<TweetLabelEntity> labelEntities) {
        Map<Long, TweetLabelEntity> map = new HashMap<>();
        labelEntities.forEach(e -> {
            map.put(e.getTweetId(), e);
        });
        return map;
    }

    public Set<Long> getIdSet(List<TweetLabelEntity> labelEntities) {
        return new HashSet<>(labelEntities.stream().map(TweetLabelEntity::getTweetId).collect(Collectors.toSet()));
    }

    public Set<Long> getIdSet(Map<Long, TweetLabelEntity> map) {
        return map.keySet();
    }
}
