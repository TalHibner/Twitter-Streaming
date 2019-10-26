package example;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class IndexAndSearchTextStructure
{

	public static final String FILES_TO_INDEX_DIRECTORY = "filesToIndex";
	public static final String INDEX_DIRECTORY = "indexDirectory";

	public static final String FIELD_PATH = "path";
	public static final String FIELD_CONTENTS = "contents";

	public static void main(String[] args) throws Exception {

		createIndex();
		searchIndex("mushrooms");
		searchIndex("steak");
		searchIndex("steak AND cheese");
		searchIndex("steak and cheese");
		searchIndex("bacon OR cheese");

	}

	public static void createIndex() throws CorruptIndexException, LockObtainFailedException, IOException {
		Analyzer analyzer = new StandardAnalyzer();
		File IndexDirPath = new File(INDEX_DIRECTORY);
		Directory indexDir = FSDirectory.open(IndexDirPath.toPath());
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter indexWriter = new IndexWriter(indexDir, config);
		File dir = new File(FILES_TO_INDEX_DIRECTORY);
		File[] files = dir.listFiles();
		for (File file : files) {
			Document document = new Document();

			String path = file.getCanonicalPath();
			document.add(new StringField(FIELD_PATH, path, Field.Store.YES));

			Reader reader = new FileReader(file);
			document.add(new TextField(FIELD_CONTENTS, reader));

			indexWriter.addDocument(document);
		}
		indexWriter.commit();
		indexWriter.close();
		
		
	}

	public static void searchIndex(String searchString) throws IOException, ParseException {
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
		   String path = document.get(FIELD_PATH);
			System.out.println("Hit: " + path);
		}
        System.out.println("Hits (rank,score,docId)");
        for (int n = 0; n < scoreDocs.length; ++n) {
            ScoreDoc sd = scoreDocs[n];
            float score = sd.score;
            int docId = sd.doc;
            System.out.printf("%3d %4.2f %d\n",
                              n, score, docId);
        }
        reader.close();
    }


}