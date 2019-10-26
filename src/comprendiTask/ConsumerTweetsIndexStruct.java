package comprendiTask;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

import twitter4j.Status;

public class ConsumerTweetsIndexStruct extends Thread 
{	
//	public static final String FILES_TO_INDEX_DIRECTORY = "filesToIndex";
	private static final String INDEX_DIRECTORY = "indexDirectory";
	private static final String FIELD_ID = "tweetId";
	private static final String FIELD_CONTENTS = "contents";
	
	boolean blnExit = false;
	private final int id;
	private final BlockingQueue<Status> sharedQueue;
	private final Hashtable<String, Status> tweetsTable;
	private final List<Status> tweetsSearchResult;
	
	
	public ConsumerTweetsIndexStruct(final int id, final BlockingQueue<Status> sharedQueue) {
		this.id = id;
		this.sharedQueue = sharedQueue;
		this.tweetsSearchResult = null;
		this.tweetsTable = new Hashtable<String, Status>();
	}
	public void setExitCondition(final boolean blnDoExit) 
	{
		blnExit = blnDoExit;
	}
	@Override
	public void run() 
	{
		try {
			createIndex();
			searchIndex("what");
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Consumer " + id + " exiting");
	}
	
	public void createIndex() throws CorruptIndexException, LockObtainFailedException, IOException
	{
		Analyzer analyzer = new StandardAnalyzer();
		File IndexDirPath = new File(INDEX_DIRECTORY);
		Directory indexDir = FSDirectory.open(IndexDirPath.toPath());
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter indexWriter = new IndexWriter(indexDir, config);
		while (!blnExit) 
		{
			try {
				if (sharedQueue.size() > 0) 
				{
					System.out.println("Consumer id:" + id + " sent email " + sharedQueue.take());
					
					Status tweet = sharedQueue.take();
					String tweetId = String.valueOf(tweet.getId());
					tweetsTable.put(tweetId, tweet);
						
					Document document = new Document();
					document.add(new StringField(FIELD_ID, tweetId, Field.Store.YES));
					document.add(new TextField(FIELD_CONTENTS, cleanText(tweet.getText()), Field.Store.YES));
					indexWriter.addDocument(document);
				}
				else{
					Thread.sleep(500);
				}
			} 
			catch (final InterruptedException ex) 
			{
				ex.printStackTrace();
			}
		}
		indexWriter.commit();
		indexWriter.close();
	}

	public List<Status> searchIndex(String searchString) throws IOException, ParseException {
		System.out.println("Searching for '" + searchString + "'");
		File IndexDirPath = new File(INDEX_DIRECTORY);
		Directory indexDir = FSDirectory.open(IndexDirPath.toPath());
	    DirectoryReader reader = DirectoryReader.open(indexDir);
	    IndexSearcher indexSearcher = new IndexSearcher(reader);

		Analyzer analyzer = new StandardAnalyzer();
		QueryParser queryParser = new QueryParser(FIELD_CONTENTS, analyzer);
		Query query = queryParser.parse(searchString);
		
		TopDocs hits = indexSearcher.search(query, 100000);
        ScoreDoc[] scoreDocs = hits.scoreDocs;
		
		System.out.println("Number of hits: " + scoreDocs.length);
		for (int i = 0; i < scoreDocs.length; i++) {
		   int docId = scoreDocs[i].doc;
		   Document document = indexSearcher.doc(docId);
		   String tweetId = document.get(FIELD_ID);
		   Status tweet = tweetsTable.get(tweetId);
		   if (tweet!=null)
		   {
			  tweetsSearchResult.add(tweet); 
			  System.out.println("Hit: " + tweetId);
		   }
		}
		
        reader.close();
		return tweetsSearchResult;
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
}

