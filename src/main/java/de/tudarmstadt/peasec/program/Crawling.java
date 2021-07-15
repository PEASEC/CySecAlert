package de.tudarmstadt.peasec.program;

import de.tudarmstadt.peasec.twitter.crawl.TweetCrawler;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;

import java.util.Properties;

public class Crawling {
    public static void crawl(String userCollectionName, String tweetCollectionName) {
        Properties properties = new Properties();
        properties.setProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME, tweetCollectionName);
        properties.setProperty(CollectionNameProperties.USER_ENTITY_COLLECTION_NAME, userCollectionName);
        TweetCrawler crawler = new TweetCrawler(properties);
        crawler.crawl();
    }
}
