package de.tudarmstadt.peasec.service;

import de.tudarmstadt.peasec.entity.ProcessedTextEntity;
import de.tudarmstadt.peasec.entity.TweetEntity;
import de.tudarmstadt.peasec.entity.help.Cluster;
import de.tudarmstadt.peasec.entity.help.TokenVector;
import de.tudarmstadt.peasec.util.TweetRepresentationParser;

import java.util.*;

public class ClusteringService {

    public static final String DISTANCE_THRESHOLD = "ClusteringService.DistanceThreshold";
    public static final String INTERACTIVE_MODE = "ClusteringService.InteractiveMode";
    public static final String ALERT_TWEET_COUNT_THRESHOLD = "ClusteringService.AlertThreshold";

    // TODO: maybe change to map to save which clusters have already been used in 'alerts'
    private List<Cluster> clusters;

    private double distanceThreshold;
    private int alertThreshold;

    Map<String, Integer> dfMap = new HashMap<>();

    private InteractiveClusteringService interactiveClusteringService = null;
    private TweetRepresentationParser tweetRepresentationParser;

    public ClusteringService() {
        this.clusters = new ArrayList<>();
    }

    public ClusteringService(Properties properties) {
        this();
        this.tweetRepresentationParser = new TweetRepresentationParser(properties);
        this.distanceThreshold = Double.parseDouble(properties.getProperty(DISTANCE_THRESHOLD, "-1.0d"));
        this.alertThreshold = Integer.parseInt(properties.getProperty(ALERT_TWEET_COUNT_THRESHOLD, "5"));
        if(properties.containsKey(INTERACTIVE_MODE))
            this.interactiveClusteringService = new InteractiveClusteringService(properties);
    }

    private boolean isInteractiveMode() {
        return this.interactiveClusteringService != null;
    }

    public Optional<Cluster> add(ProcessedTextEntity e) {
        Cluster candidateCluster;

        Optional<Cluster> c = getAppropriateCluster(e);
        candidateCluster = c.orElse(this.addCluster());
        this.addToCluster(e, candidateCluster);

        return this.checkForAlert(candidateCluster) ? Optional.of(candidateCluster) : Optional.empty();
    }

    private void addToCluster(ProcessedTextEntity e, Cluster c) {
        for(String token : e.getText().split(" ")) {
            if(!this.dfMap.containsKey(token))
                this.dfMap.put(token, 1);
            this.dfMap.put(token, this.dfMap.get(token) + 1);
        }
        c.add(e);
    }

    private Optional<Cluster> getAppropriateCluster(ProcessedTextEntity e) {
        Optional<Cluster> out;
        TokenVector tv = new TokenVector(e.getText());
        Optional<Cluster> candOpt = this.findNearestCluster(tv);

        if(!candOpt.isEmpty()) {
            Cluster cand = candOpt.get();
            double distance = TokenVector.cosineDistance(cand.getCenter(), tv);
            if (!this.isInteractiveMode() || distance == 1) {
                out = distance < this.distanceThreshold
                        ? Optional.of(cand)
                        : Optional.empty();
            } else {
                out = this.interactiveClusteringService.determineOutcome(cand, e, distance)
                        ? Optional.of(cand)
                        : Optional.empty();
            }
        }
        else
            out = Optional.empty();
        return out;
    }

    private Optional<Cluster> findNearestCluster(TokenVector tv) {
        double minDist = Double.MAX_VALUE;
        Cluster currentMinDistCluster = null;
        double dist;
        for(Cluster c : this.clusters) {
            dist = TokenVector.cosineDistance(c.getCenter(), tv);
            if(dist < minDist) {
                minDist = dist;
                currentMinDistCluster = c;
            }
        }
        return currentMinDistCluster != null ? Optional.of(currentMinDistCluster) : Optional.empty();
    }

    private Cluster addCluster() {
        Cluster c = new Cluster();
        c.setDfMap(this.dfMap);
        this.clusters.add(c);
        return c;
    }

    private boolean checkForAlert(Cluster c) {
        //TODO: implement
        return false;
    }

    public void printClusters() {
        this.printClusters(Integer.MAX_VALUE);
    }

    public void printRelevantClusters() {
        this.printClusters(this.alertThreshold);
    }

    public void printClusters(int minSize) {
        System.out.println("=== Clustering Parameter ===");
        System.out.println("Threshold: " + this.distanceThreshold);
        System.out.println("Minimal Count per Cluster: " + minSize);
        this.clusters.stream()
                .filter(e -> e.getEntityCount() >= minSize)
                .forEach(this::printCluster);
    }

    public void printCluster(Cluster c) {
        System.out.println("=== CLUSTER ===");
        System.out.println("Size: " + c.getEntityCount());
        List<TweetEntity> tweetEntityList = this.tweetRepresentationParser.getTweetEntity(c.getProcessedTextEntityList());
        for(TweetEntity e : tweetEntityList) {
            System.out.println("------------");
            System.out.println(e.getText());
        }
        System.out.println("------------");
        System.out.println();
        System.out.println();
        System.out.println();
    }
}
