package de.tudarmstadt.peasec.service;

import de.tudarmstadt.peasec.entity.ProcessedTextEntity;
import de.tudarmstadt.peasec.entity.TweetEntity;
import de.tudarmstadt.peasec.entity.help.Cluster;
import de.tudarmstadt.peasec.util.TweetRepresentationParser;

import java.util.*;

public class InteractiveClusteringService {

    TweetRepresentationParser tweetRepresentationParser;

    Scanner scanner = new Scanner(System.in);

    Map<Double, Boolean> distMap = new HashMap<>();

    public InteractiveClusteringService(Properties properties) {
        this.tweetRepresentationParser = new TweetRepresentationParser(properties);
    }

    /**
     *
     * @param c
     * @param p
     * @param distance
     * @return true, if p should be part of c
     */
    public boolean determineOutcome(Cluster c, ProcessedTextEntity p, double distance) {
        this.printCluster(c);
        this.printProcessedTextEntity(p);

        String s = scanner.next();
        boolean out = s.equals("+");
        distMap.put(distance, out);

        printDistMap();

        return out;
    }

    public void printCluster(Cluster c) {
        System.out.println("=== CLUSTER ===");
        List<TweetEntity> tweetEntityList = this.tweetRepresentationParser.getTweetEntity(c.getProcessedTextEntityList());
        for(TweetEntity e : tweetEntityList) {
            System.out.println("------------");
            System.out.println(e.getText());
        }
        System.out.println("------------");
    }

    private void printProcessedTextEntity(ProcessedTextEntity p) {
        System.out.println("=== TWEET ===");
        TweetEntity tweetEntity = this.tweetRepresentationParser.getTweetEntity(p);
        System.out.println(tweetEntity.getText());
    }

    private void printDistMap() {
        System.out.println("=== DistMap ===");
        List<Map.Entry<Double, Boolean>> aList = new ArrayList<>(this.distMap.entrySet());
        aList.sort(Comparator.comparingDouble(Map.Entry::getKey));
        for(Map.Entry<Double, Boolean> entry : aList) {
            System.out.println("Dist: " + entry.getKey() + " --> " + entry.getValue());
        }
    }
}
