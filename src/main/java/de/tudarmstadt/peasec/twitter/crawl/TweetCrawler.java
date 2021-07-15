package de.tudarmstadt.peasec.twitter.crawl;

import de.tudarmstadt.peasec.entity.UserEntity;
import de.tudarmstadt.peasec.service.TweetMongoService;
import de.tudarmstadt.peasec.service.TweetTwitterService;
import de.tudarmstadt.peasec.service.UserMongoService;
import de.tudarmstadt.peasec.util.Statistics;
import de.tudarmstadt.peasec.util.config.CollectionNameProperties;
import twitter4j.Paging;
import twitter4j.Status;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class TweetCrawler {

    private TweetTwitterService twitterService = new TweetTwitterService();

    private TweetMongoService mongoService;

    private UserMongoService userMongoService;

    public TweetCrawler(Properties properties) {
        String tweetCollectionName = properties.getProperty(CollectionNameProperties.TWEET_ENTITY_COLLECTION_NAME);
        this.setTweetMongoCollection(tweetCollectionName);
        this.userMongoService = new UserMongoService(properties);
    }

    public void setTweetMongoCollection(String name) {
        this.mongoService = new TweetMongoService(name);
    }

    public void crawl() {
        List<UserEntity> userList = userMongoService.getUserEntityList();
        for(UserEntity u : userList) {
            if(mongoService.getTweetCountByUser(u) > 1000) {
                System.out.println("User skipped: " + u.getScreenName());
                continue;
            }
            this.downloadUserTweets(u);
        }
    }

    public void downloadUserTweets(UserEntity user) {
        System.out.println("Start with new User: " + user.getScreenName() + "; TweetCount: " + user.getStatusesCount());
        Paging page = new Paging(1, 20);
        for(int i = 1; i < user.getStatusesCount() / 20; i++) {
            page.setPage(i);
            List<Status> statusList = twitterService.getTweetsFromUser(user, page);
            if (statusList == null || statusList.size() == 0) {
                System.out.println("Finished on page " + i);
                break;
            }
            //filter out retweets and non-english tweets
            statusList = statusList.stream()
                    .filter(t ->
                            t.getText().length() > 2 && !t.getText().substring(0, 2).equals("RT")
                    )
                    .filter(t -> t.getLang().equals("en"))
                    .collect(Collectors.toList());
            if(statusList.size() > 0)
                mongoService.saveStatusList(statusList);
        }
        Statistics.getInstance().addTo("User finished");
    }

}
