package de.tudarmstadt.peasec.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.tudarmstadt.peasec.entity.ProcessedTextEntity;
import de.tudarmstadt.peasec.entity.TweetEntity;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.pipeline.TextPreprocessor;
import de.tudarmstadt.peasec.util.Timer;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;

public class TweetTextPreprocessingService {

    public static final String TRIM_MOST_COMMON_WORD_FRACTION = "TweetTextPreprocessingService.TrimMostCommonWordFraction";
    public static final String TRIM_MOST_COMMON_WORD_COUNT = "TweetTextPreprocessingService.TrimMostCommonWordFraction";

    private MongoCollection<TweetEntity> tweetCollection;
    private MongoCollection<ProcessedTextEntity> processedTextCollection;

    private double trimFraction = -1;
    private int trimCount = -1;

    public TweetTextPreprocessingService(Properties properties) {
        this.trimFraction = Double.parseDouble(properties.getProperty(TRIM_MOST_COMMON_WORD_FRACTION, "-1"));
        this.trimCount = Integer.parseInt(properties.getProperty(TRIM_MOST_COMMON_WORD_COUNT, "-1"));
        this.setTweetCollection(properties.getProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME));
        this.setProcessedTextCollection(properties.getProperty(CollectionNameProperties.PROCESSED_TEXT_ENTITY_COLLECTION_NAME));
    }

    public void setTweetCollection(String tweetCollectionName) {
        this.tweetCollection = MongoHelper.getInstance()
                .getCollection(tweetCollectionName, TweetEntity.class);
    }

    public void setProcessedTextCollection(String processedTextCollectionName) {
        this.processedTextCollection = MongoHelper.getInstance()
                .getCollection(processedTextCollectionName, ProcessedTextEntity.class);
    }

    public void processAllTweetsAndSave() {
        this.processAllTweetsAndSave(false);
    }

    public void processAllTweetsAndSave(boolean timed) {
        List<TweetEntity> tweetList = this.getAllTweets();
        Timer timer = new Timer();
        if(timed) timer.start();
        List<ProcessedTextEntity> processedTextEntityList = processTweetList(tweetList);
        if(timed) {
            timer.end();
        }
        this.saveProcessedTextEntities(processedTextEntityList);
        if(timed) System.out.println("Preprocessing time: " + timer.getTime() + "ns");
    }

    public Map<String, Integer> getMostCommonWordMap(List<ProcessedTextEntity> list) {
        Map<String, Integer> map = new HashMap<>();
        for(ProcessedTextEntity e : list) {
            for(String s : e.getText().split(" ")) {
                if(!map.containsKey(s))
                    map.put(s, 1);
                else
                    map.put(s, map.get(s) + 1);
            }
        }
        return map;
    }

    private List<TweetEntity> getAllTweets() {
        return this.tweetCollection.find().into(new ArrayList<>());
    }

    public List<ProcessedTextEntity> processTweetList(List<TweetEntity> tweetList) {
        List<ProcessedTextEntity> outList = new ArrayList<>(tweetList.size());
        TextPreprocessor preprocessor = new TextPreprocessor();

        String text;
        ProcessedTextEntity entity;
        for(TweetEntity t : tweetList) {
             text = preprocessor.process(t.getText());
             entity = new ProcessedTextEntity();
             entity.setTweetId(t.getTweetId());
             entity.setText(text);
             outList.add(entity);
        }

        if(this.trimFraction > 0 || this.trimCount > 0)
            this.trimMostCommonTokens(outList);

        return outList;
    }

    public void trimMostCommonTokens(List<ProcessedTextEntity> textEntities) {
        Map<String, Integer> map = this.getMostCommonWordMap(textEntities);

        List<String> trimStrings = new ArrayList<>();
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
        entryList.sort(Comparator.comparingInt(Map.Entry::getValue));
        Collections.reverse(entryList);
        if(this.trimCount > 0) {
            trimStrings = entryList.subList(0, this.trimCount)
                    .stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        else if(this.trimFraction > 0) {
            trimStrings = entryList.subList(0, (int) (this.trimFraction*entryList.size()))
                    .stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        String s;
        List<String> copy = new ArrayList<>(trimStrings);
        for(ProcessedTextEntity e : textEntities) {
            s = e.getText();
            s = String.join(" ", Arrays.asList(s.split(" ")).stream().filter(token -> !copy.contains(token)).collect(Collectors.toList()));
            e.setText(s);
        }
    }

    public void saveProcessedTextEntities(List<ProcessedTextEntity> textEntityList) {
        List<Long> tweetIdList = textEntityList.stream().map(e -> e.getTweetId()).collect(Collectors.toList());
        this.processedTextCollection.deleteMany(Filters.in("tweetId", tweetIdList));
        this.processedTextCollection.insertMany(textEntityList);
    }

    public List<ProcessedTextEntity> getTextEntities() {
        return this.processedTextCollection.find().into(new ArrayList<>());
    }

    public List<ProcessedTextEntity> getTextEntities(Bson filter) {
        return this.processedTextCollection.find(filter).into(new ArrayList<>());
    }

    public long deleteTextEntities(Bson filter) {
        return this.processedTextCollection.deleteMany(filter).getDeletedCount();
    }

    public void dropProcessedTextEntities() {
        this.processedTextCollection.drop();
    }
}
