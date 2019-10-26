package comprendiTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import twitter4j.*;
import twitter4j.auth.OAuth2Token;
import twitter4j.conf.ConfigurationBuilder;

public class ProducerTweets extends Thread 
{
	private static final String CONSUMER_KEY = "hu0mrDOS2ibY8t7sPCGMxKMiL";
	private static final String CONSUMER_SECRET = "QUtIX8ecOu7teuH4cQddfqCJHjNaou0sfwQeXKEBKVyN77bffP";
	private static final int TWEETS_PER_QUERY = 100;
	private static final int MAX_QUERIES = 15;
	private static final String SEARCH_TERM	= "Justin Bieber";
	
	private  boolean blnExit = false;
	private final List<ConsumerTweetsIndexStruct> consumers;
	private final BlockingQueue<Status> sharedQueue;
	
	public ProducerTweets(final BlockingQueue<Status> sharedQueue, final List<ConsumerTweetsIndexStruct> consumers) 
	{
		this.sharedQueue = sharedQueue;
		this.consumers = consumers;
	}
	@Override
	//PRODUCING THE Tweets TO BE CONSUMED
	public void run() 
	{
		while (!blnExit) 
		{
			tweetsRetrieving() ;
			// WAIT UNTIL THE QUEUE IS EMPTY
			while (sharedQueue.size() > 0) {
				try 
				{
					Thread.sleep(200);
					System.out.println("Producer waiting to end.");
				} 
				catch (final InterruptedException e) 
				{
					break;
				}
			}
			// SEND TO ALL CONSUMERS THE EXIT CONDITION
			for (final ConsumerTweetsIndexStruct consumer : consumers) 
			{
				consumer.setExitCondition(true);
			}
		}
	}
	
	public void tweetsRetrieving() 
	{ 
		int	totalTweets = 0; 
		long maxID = -1; //Without setting the MaxId in the query, Twitter will always retrieve the most recent tweets.
		Twitter twitter = getTwitter(); 
		try {
			// The proper thing to do is always check your limits BEFORE making a call, and if you have 
			//	hit your limits sleeping until you are allowed to make calls again.
			
			//	This returns all the various rate limits in effect for us with the Twitter API 
			Map<String, RateLimitStatus> rateLimitStatus = twitter.getRateLimitStatus("search"); 
			//	This finds the rate limit specifically for doing the search API call we use in this program 
			RateLimitStatus searchTweetsRateLimit = rateLimitStatus.get("/search/tweets"); 
			//	Always nice to see these things when debugging code... 
			System.out.printf("You have %d calls remaining out of %d, Limit resets in %d seconds\n", searchTweetsRateLimit.getRemaining(), searchTweetsRateLimit.getLimit(), searchTweetsRateLimit.getSecondsUntilReset()); 
			//	This is the loop that retrieve multiple blocks of tweets from Twitter 
			for (int queryNumber=0;queryNumber < MAX_QUERIES; queryNumber++) 
			{ 
				System.out.printf("\n\n!!! Starting loop %d\n\n", queryNumber); 
				//	Do we need to delay because we've already hit our rate limits? 
				if (searchTweetsRateLimit.getRemaining() == 0) 
				{ 
					System.out.printf("!!! Sleeping for %d seconds due to rate limits\n", searchTweetsRateLimit.getSecondsUntilReset()); 
					Thread.sleep((searchTweetsRateLimit.getSecondsUntilReset()+2) * 1000l); 
					} 
				Query q = new Query(SEARCH_TERM);// Search for tweets that contains this term  - Query query = new Query(args[0]);
				q.setCount(TWEETS_PER_QUERY);	// How many tweets, max, to retrieve 
				q.resultType("recent");	// Get all tweets 
				q.setLang("en");	// English language tweets
				//	This is our first call
				if (maxID != -1) 
				{ 
					q.setMaxId(maxID - 1); 
				} 
				//	This actually does the search on Twitter and makes the call across the network 
				QueryResult r = twitter.search(q); 
				List<Status> tweets = r.getTweets();
				if (tweets.size() == 0) 
				{ 
					break;
				} 
				for (Status tweet: tweets)	// Loop through all the tweets... 
				{ 
					totalTweets++;//Increment our count of tweets retrieved 
					if (maxID == -1 || tweet.getId() < maxID) 
					{ //	Keep track of the lowest tweet ID. If you do not do this, you cannot retrieve multiple locks of tweets... 
						maxID = tweet.getId(); 
					} 
					
				//	Do something with the tweet.... 
				sharedQueue.put(tweet);
				System.out.printf("At %s, @%-20s said: %s\n", tweet.getCreatedAt().toString(), tweet.getUser().getScreenName(), cleanText(tweet.getText()));
				}  
			searchTweetsRateLimit = r.getRateLimitStatus(); 
			} 
		} 
		catch (Exception e) 
		{ 
			System.out.println("That didn't work well...wonder why?");
			e.printStackTrace();
			} 
		System.out.printf("\n\nA total of %d tweets retrieved\n", totalTweets);
		}
	
	/** * Replace newlines and tabs in text with escaped versions to making printing cleaner 
	 * 
	 * @param text	The text of a tweet, sometimes with embedded newlines and tabs 
	 * @return	The text passed in, but with the newlines and tabs replaced 
	 * */ 
	public static String cleanText(String text) 
	{ 
		text = text.replace("\n", "\\n");
		text = text.replace("\t", "\\t");
		return text;
	} 
	/**   
	 * Retrieve the "bearer" token from Twitter in order to make application-authenticated calls.
	 * This is the first step in doing application authentication, as described in Twitter's documentation at 
	 * 
	 * @return The oAuth2 bearer token 
	 * */ 
	public static OAuth2Token getOAuth2Token() 
	{ 
		OAuth2Token token = null;
		ConfigurationBuilder cb;
		cb = new ConfigurationBuilder();
		cb.setApplicationOnlyAuthEnabled(true);
		cb.setOAuthConsumerKey(CONSUMER_KEY).setOAuthConsumerSecret(CONSUMER_SECRET);
		try 
		{ 
			token = new TwitterFactory(cb.build()).getInstance().getOAuth2Token();
		} 
		catch (Exception e) 
		{ 
			System.out.println("Could not get OAuth2 token");
			e.printStackTrace();
			System.exit(0);
		} 
		return token; 
	}
	
	/** 
	 * * Get a fully application-authenticated Twitter object useful for making subsequent calls. 
	 * * * @return	Twitter4J Twitter object that's ready for API calls 
	 * */ 
	public static Twitter getTwitter() 
	{ 
		OAuth2Token token; 
		//	First step, get a "bearer" token that can be used for our requests 
		token = getOAuth2Token(); 
		//	Now, configure our new Twitter object to use application authentication and provide it with 
		//	our CONSUMER key and secret and the bearer token we got back from Twitter 
		ConfigurationBuilder cb = new ConfigurationBuilder(); 
		cb.setApplicationOnlyAuthEnabled(true); 
		cb.setOAuthConsumerKey(CONSUMER_KEY); 
		cb.setOAuthConsumerSecret(CONSUMER_SECRET); 
		cb.setOAuth2TokenType(token.getTokenType()); 
		cb.setOAuth2AccessToken(token.getAccessToken()); 
		//	And create the Twitter object! 
		return new TwitterFactory(cb.build()).getInstance(); 
	}
}
