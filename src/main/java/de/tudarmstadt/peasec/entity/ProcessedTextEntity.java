package de.tudarmstadt.peasec.entity;

public class ProcessedTextEntity implements IAdressableById, IProcessedText{
    private String text;

    private long tweetId;

    public ProcessedTextEntity() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTweetId() {
        return tweetId;
    }

    public void setTweetId(long tweetId) {
        this.tweetId = tweetId;
    }
}
