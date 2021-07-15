package de.tudarmstadt.peasec.entity;

public class TweetLabelEntity implements IAdressableById, ILabel{

    private long tweetId;

    private String label;

    public TweetLabelEntity() {
    }

    public TweetLabelEntity(long tweetId, String relevant) {
        this.tweetId = tweetId;
        this.label = relevant;
    }

    public long getTweetId() {
        return tweetId;
    }

    public void setTweetId(long tweetId) {
        this.tweetId = tweetId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
