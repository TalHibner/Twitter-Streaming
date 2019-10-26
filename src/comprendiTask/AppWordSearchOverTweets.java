package comprendiTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import twitter4j.Status;

public class AppWordSearchOverTweets 
{
	private final int queueCapacity = 100100;
	private int numberOfThreads = 1;
	
	public static void main(final String args[]) 
	{
		new AppWordSearchOverTweets(1);
	}
	
	public AppWordSearchOverTweets(final int numberOfThreads) 
	{
		if (numberOfThreads <= 0 || numberOfThreads > 100)
		{
			throw new IllegalArgumentException("The number of threads should be a number between 1 and 100");
		}
		this.numberOfThreads = numberOfThreads;
		//Creating shared object
		final BlockingQueue<Status> sharedQueue = new LinkedBlockingQueue<Status>(queueCapacity);
		// Creating and starting the Consumer Threads
		final List<ConsumerTweetsIndexStruct> consumers = new ArrayList<ConsumerTweetsIndexStruct>();
		for (int i = 0; i <= this.numberOfThreads; i++) 
		{
			final ConsumerTweetsIndexStruct consThread = new ConsumerTweetsIndexStruct(i, sharedQueue);
			consThread.start();
			consumers.add(consThread);
		}
		// Creating and starting the Producer Thread
		final ProducerTweets prodThread = new ProducerTweets(sharedQueue, consumers);
		prodThread.start();
	}
}
