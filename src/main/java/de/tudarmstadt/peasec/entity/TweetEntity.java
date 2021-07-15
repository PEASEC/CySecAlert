package de.tudarmstadt.peasec.entity;

import twitter4j.Status;

import java.util.Date;

/**
 * Representation of all informations a tweet may contain, that are relevant in the context of this application.
 */
public class TweetEntity implements IAdressableById {

    private long tweetId;

    private String text;

    //User who posted the tweet
    private long userId;

    private Date createdAt;

    private int displayTextRangeStart;

    private int getDisplayTextRangeEnd;

    public TweetEntity() {

    }

    public TweetEntity(Status status) {
        this.setTweetId(status.getId());
        this.setText(status.getText());
        this.setUserId(status.getUser().getId());
        this.setCreatedAt(status.getCreatedAt());
        this.setDisplayTextRangeStart(status.getDisplayTextRangeStart());
        this.setGetDisplayTextRangeEnd(status.getDisplayTextRangeEnd());
    }

    public long getTweetId() {
        return tweetId;
    }

    public void setTweetId(long tweetId) {
        this.tweetId = tweetId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getDisplayTextRangeStart() {
        return displayTextRangeStart;
    }

    public void setDisplayTextRangeStart(int displayTextRangeStart) {
        this.displayTextRangeStart = displayTextRangeStart;
    }

    public int getGetDisplayTextRangeEnd() {
        return getDisplayTextRangeEnd;
    }

    public void setGetDisplayTextRangeEnd(int getDisplayTextRangeEnd) {
        this.getDisplayTextRangeEnd = getDisplayTextRangeEnd;
    }
}
