package de.tudarmstadt.peasec.entity;

import twitter4j.TwitterException;
import twitter4j.User;

import java.sql.Timestamp;
import java.util.Date;

public class UserEntity {

    private long userId;

    private String name;

    private String screenName;

    private Date createdAt;

    private String lang;

    private long friendsCount, favouritesCount, statusesCount;

    public UserEntity() {

    }

    public UserEntity(User user) {
        super();
        this.setUserId(user.getId());
        this.setName(user.getName());
        this.setScreenName(user.getScreenName());
        this.setCreatedAt(user.getCreatedAt());
        this.setLang(user.getLang());
        this.setFriendsCount(user.getFriendsCount());
        this.setFavouritesCount(user.getFavouritesCount());
        this.setStatusesCount(user.getStatusesCount());
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public long getFriendsCount() {
        return friendsCount;
    }

    public void setFriendsCount(long friendsCount) {
        this.friendsCount = friendsCount;
    }

    public long getFavouritesCount() {
        return favouritesCount;
    }

    public void setFavouritesCount(long favouritesCount) {
        this.favouritesCount = favouritesCount;
    }

    public long getStatusesCount() {
        return statusesCount;
    }

    public void setStatusesCount(long statusesCount) {
        this.statusesCount = statusesCount;
    }


}
