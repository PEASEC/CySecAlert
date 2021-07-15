package de.tudarmstadt.peasec.twitter.crawl;

import de.tudarmstadt.peasec.util.Statistics;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class CrawlerUtil {

    //TODO: Insert your Twitter Credentials here
    public static String OAuthConsumerKey = "";
    public static String OAuthConsumerSecret = "";
    public static String OAuthAccessToken = "";
    public static String OAuthAccessTokenSecret = "";

    public static Twitter getTwitter() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDaemonEnabled(true)
                .setOAuthConsumerKey(OAuthConsumerKey)
                .setOAuthConsumerSecret(OAuthConsumerSecret)
                .setOAuthAccessToken(OAuthAccessToken)
                .setOAuthAccessTokenSecret(OAuthAccessTokenSecret);
        TwitterFactory factory = new TwitterFactory(cb.build());
        Twitter twitter = factory.getInstance();
        return twitter;
    }

    public static void waitForLimit(int secondsUntilReset) {
        try {
            System.out.println("Ran into limit. Seconds remaining: " + secondsUntilReset);
            Statistics.getInstance().print();
            Thread.sleep((secondsUntilReset + 10) * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
