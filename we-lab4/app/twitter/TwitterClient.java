package twitter;

/**
 * Created by Nicole on 29.05.15.
 */
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterClient implements ITwitterClient{

    private Twitter twitter;

    public TwitterClient()
    {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("GZ6tiy1XyB9W0P4xEJudQ")
                .setOAuthConsumerSecret("gaJDlW0vf7en46JwHAOkZsTHvtAiZ3QUd2mD1x26J9w")
                .setOAuthAccessToken("1366513208-MutXEbBMAVOwrbFmZtj1r4Ih2vcoHGHE2207002")
                .setOAuthAccessTokenSecret("RMPWOePlus3xtURWRVnv1TgrjTyK7Zk33evp4KKyA");
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();
    }

    @Override
    public void publishUuid(TwitterStatusMessage message) throws Exception
    {
        Status status;
        try{
            status = twitter.updateStatus(message.getTwitterPublicationString());
        }catch(TwitterException e){
            play.Logger.error("UUID "+message.getTwitterPublicationString() + " konnte nicht auf Twitter veröffentlicht werden!");
            throw e;
        }
        play.Logger.info("UUID "+message.getTwitterPublicationString() + " wurde auf Twitter veröffentlicht");
        System.out.println("UUID "+message.getTwitterPublicationString() + " wurde auf Twitter veröffentlicht");
    }
}
