package de.tudarmstadt.peasec.util;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.tudarmstadt.peasec.entity.*;
import de.tudarmstadt.peasec.entity.help.LabeledProcessedTextWrapper;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;

import java.util.*;
import java.util.stream.Collectors;

public class TweetRepresentationParser {

    private MongoCollection<TweetEntity> tweetEntityCollection;
    private MongoCollection<ProcessedTextEntity> processedTextEntityCollection;
    private MongoCollection<TweetLabelEntity> tweetLabelEntityCollection;

    public TweetRepresentationParser() {

    }

    public TweetRepresentationParser(Properties prop) {
        this();
        this.setTweetEntityCollection(prop.getProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME));
        this.setProcessedTextEntityCollection(prop.getProperty(CollectionNameProperties.PROCESSED_TEXT_ENTITY_COLLECTION_NAME));
        this.setTweetLabelEntityCollection(prop.getProperty(CollectionNameProperties.TWEET_LABEL_ENTITY_COLLECTION_NAME));
    }

    public void setTweetEntityCollection(String tweetEntityCollectionName) {
        this.tweetEntityCollection = MongoHelper.getInstance()
                .getCollection(tweetEntityCollectionName, TweetEntity.class);
    }

    public void setProcessedTextEntityCollection(String processedTextEntityCollectionName) {
        this.processedTextEntityCollection = MongoHelper.getInstance()
                .getCollection(processedTextEntityCollectionName, ProcessedTextEntity.class);
    }

    public void setTweetLabelEntityCollection(String tweetLabelEntityCollectionName) {
        this.tweetLabelEntityCollection = MongoHelper.getInstance()
                .getCollection(tweetLabelEntityCollectionName, TweetLabelEntity.class);
    }

    // getTweetEntity

    public TweetEntity getTweetEntityById(long l) {
        return this.tweetEntityCollection.find(Filters.eq("tweetId", l)).first();
    }

    public List<TweetEntity> getTweetEntityById(List<Long> longList) {
        return this.tweetEntityCollection.find(Filters.in("tweetId", longList)).into(new ArrayList<>());
    }

    public TweetEntity getTweetEntity(IAdressableById a) {
        return this.getTweetEntityById(a.getTweetId());
    }

    public List<TweetEntity> getTweetEntity(List<? extends IAdressableById> list) {
        List<Long> idList = list.stream().map(e -> e.getTweetId()).collect(Collectors.toList());
        return this.getTweetEntityById(idList);
    }

    // getTweetLabelEntity

    public TweetLabelEntity getTweetLabelEntityById(long l) {
        return this.tweetLabelEntityCollection.find(Filters.eq("tweetId", l)).first();
    }

    public List<TweetLabelEntity> getTweetLabelEntityById(List<Long> idList) {
        return this.tweetLabelEntityCollection.find(Filters.in("tweetId", idList)).into(new ArrayList<>());
    }

    public TweetLabelEntity getTweetLabelEntity(IAdressableById a) {
        return this.getTweetLabelEntityById(a.getTweetId());
    }

    public List<TweetLabelEntity> getTweetLabelEntity(List<? extends IAdressableById> aList) {
        List<Long> idList = aList.stream().map(a -> a.getTweetId()).collect(Collectors.toList());
        return this.getTweetLabelEntityById(idList);
    }

    // getPrecessedTextEntity

    public ProcessedTextEntity getProcessedTextEntityById(long l) {
        return this.processedTextEntityCollection.find(Filters.eq("tweetId", l)).first();
    }

    public List<ProcessedTextEntity> getProcessedTextEntityById(List<Long> idList) {
        return this.processedTextEntityCollection.find(Filters.in("tweetId", idList)).into(new ArrayList<>());
    }

    public ProcessedTextEntity getProcessedTextEntities(IAdressableById a) {
        return this.getProcessedTextEntityById(a.getTweetId());
    }

    public List<ProcessedTextEntity> getProcessedTextEntities(List<? extends IAdressableById> aList) {
        List<Long> idList = aList.stream().map(a -> a.getTweetId()).collect(Collectors.toList());
        return this.getProcessedTextEntityById(idList);
    }

    // getLabeledProcessedTextWrapper
    public LabeledProcessedTextWrapper getLabeledProcessedTextWrapperById(long l) {
        LabeledProcessedTextWrapper w = new LabeledProcessedTextWrapper();
        TweetLabelEntity label = this.getTweetLabelEntityById(l);
        ProcessedTextEntity processedText = this.getProcessedTextEntityById(l);
        w.setTweetId(l);
        w.setLabel(label.getLabel());
        w.setText(processedText.getText());
        return w;
    }

    public List<LabeledProcessedTextWrapper> getLabeledProcessedTextWrapperById(List<Long> idList) {
        List<TweetLabelEntity> labelList = this.getTweetLabelEntityById(idList);
        List<ProcessedTextEntity> processedTextList = this.getProcessedTextEntityById(idList);
        return this.buildLabeledProcessedTextWrapperObjects(labelList, processedTextList);
    }

    public LabeledProcessedTextWrapper getLabeledProcessedTextWrapper(IAdressableById a) {
        return this.getLabeledProcessedTextWrapperById(a.getTweetId());
    }

    public List<LabeledProcessedTextWrapper> getLabeledProcessedTextWrapper(List<? extends IAdressableById> aList) {
        List<Long> idList = aList.stream().map(e -> e.getTweetId()).collect(Collectors.toList());
        return this.getLabeledProcessedTextWrapperById(idList);
    }

    private List<LabeledProcessedTextWrapper> buildLabeledProcessedTextWrapperObjects(List<TweetLabelEntity> labelList, List<ProcessedTextEntity> processedTextList) {
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
