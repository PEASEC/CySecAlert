package de.tudarmstadt.peasec.service;

import de.tudarmstadt.peasec.entity.UserEntity;
import de.tudarmstadt.peasec.twitter.crawl.CrawlerUtil;
import de.tudarmstadt.peasec.util.Statistics;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.util.List;

public class TweetTwitterService {

    private Twitter twitter = CrawlerUtil.getTwitter();

    public List<Status> getTweetsFromUser(long userId, Paging paging) {
        List<Status> out = null;
        try {
            out = twitter.getUserTimeline(userId, paging);
        } catch (TwitterException e) {
            CrawlerUtil.waitForLimit(e.getRateLimitStatus().getSecondsUntilReset());
            out = this.getTweetsFromUser(userId, paging);
        }
        Statistics.getInstance().addTo("TweetCount", out.size());
        System.out.println("Got Tweets.");
        return out;
    }

    public List<Status> getTweetsFromUser(UserEntity user, Paging paging) {
        System.out.println("Getting Tweets from User "+ user.getScreenName() +" (Page " + paging.getPage() + ")");
        return getTweetsFromUser(user.getUserId(), paging);
    }

}
