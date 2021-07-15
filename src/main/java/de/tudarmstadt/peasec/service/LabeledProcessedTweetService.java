package de.tudarmstadt.peasec.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.tudarmstadt.peasec.entity.*;
import de.tudarmstadt.peasec.entity.help.LabeledProcessedTextWrapper;
import de.tudarmstadt.peasec.util.MongoHelper;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;

import java.util.*;
import java.util.stream.Collectors;

public class LabeledProcessedTweetService {

    public boolean DROP_EMPTY_TEXT_ENTITIES = true;

    // tokenList's as strings in this collection
    MongoCollection<ProcessedTextEntity> processedTextCollection;

    // tweet label collection
    MongoCollection<TweetLabelEntity> labelCollection;

    // tweet collection
    MongoCollection<TweetEntity> tweetCollection;

    public LabeledProcessedTweetService() {

    }

    public LabeledProcessedTweetService(Properties properties) {
        this.setProcessedTextCollection(properties.getProperty(CollectionNameProperties.PROCESSED_TEXT_ENTITY_COLLECTION_NAME));
        this.setLabelCollection(properties.getProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME));
        this.setTweetCollection(properties.getProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME));
    }

    private void setProcessedTextCollection(String processedTextCollectionName) {
        this.processedTextCollection = MongoHelper.getInstance()
                .getCollection(processedTextCollectionName, ProcessedTextEntity.class);
    }

    private void setLabelCollection(String labelCollectionName) {
        this.labelCollection = MongoHelper.getInstance()
                .getCollection(labelCollectionName, TweetLabelEntity.class);
    }

    private void setTweetCollection(String tweetCollectionName) {
        this.tweetCollection = MongoHelper.getInstance()
                .getCollection(tweetCollectionName, TweetEntity.class);
    }

    public List<LabeledProcessedTextWrapper> buildLabeledProcessedTextWrapperListFromLabels() {
        List<TweetLabelEntity> labelList = this.labelCollection.find().into(new ArrayList<>());
        List<Long> tweetIdList = labelList.stream().map(e -> e.getTweetId()).collect(Collectors.toList());
        List<ProcessedTextEntity> textList = this.processedTextCollection
                .find(Filters.in("tweetId", tweetIdList))
                .into(new ArrayList<>());

        Map<Long, LabeledProcessedTextWrapper> map =  new HashMap<>();
        for(ProcessedTextEntity e : textList) {
            if(DROP_EMPTY_TEXT_ENTITIES && e.getText().length() == 0)
                continue;
            LabeledProcessedTextWrapper w = new LabeledProcessedTextWrapper();
            w.setTweetId(e.getTweetId());
            w.setText(e.getText());
            map.put(e.getTweetId(), w);
        }

        for(TweetLabelEntity e : labelList) {
            if(map.containsKey(e.getTweetId())) {
                LabeledProcessedTextWrapper w = map.get(e.getTweetId());
                w.setLabel(e.getLabel());
                map.put(e.getTweetId(), w);
            }
        }

        return new ArrayList<>(map.values());
    }

    public List<LabeledProcessedTextWrapper> buildLabeledProcessedTextWrapperList(List<Long> tweetIdList) {
        List<TweetLabelEntity> labelList = this.labelCollection
                .find(Filters.in("tweetId", tweetIdList))
                .into(new ArrayList<>());
        List<ProcessedTextEntity> processedTextList = this.processedTextCollection
                .find(Filters.in("tweetId", tweetIdList))
                .into(new ArrayList<>());

        Map<Long, LabeledProcessedTextWrapper> map = new HashMap<>();
        for(TweetLabelEntity labelEntity : labelList) {
            LabeledProcessedTextWrapper w = new LabeledProcessedTextWrapper();
            w.setTweetId(labelEntity.getTweetId());
            w.setLabel(labelEntity.getLabel());
            map.put(labelEntity.getTweetId(), w);
        }

        for(ProcessedTextEntity processedTextEntity : processedTextList) {
            LabeledProcessedTextWrapper w = map.get(processedTextEntity.getTweetId());
            w.setText(processedTextEntity.getText());
        }

        return new ArrayList<>(map.values());
    }
}
