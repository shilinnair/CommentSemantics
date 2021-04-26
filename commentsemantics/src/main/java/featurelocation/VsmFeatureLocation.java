package featurelocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class VsmFeatureLocation implements FeatureLocation 
{	
	private final int QUERYDOC_COUNT = 20;
	
	private StandardAnalyzer standardAnalyzer = null;
	private Directory directory = null;
	private IndexWriter writer = null;
	private FieldType fieldType = null;

	public VsmFeatureLocation()
	{
		reset();
	}

	@Override
	public void prepareDocument(String fileName, List<String> data) 
	{
		if(data.isEmpty())
			return;
		
		Field field1 = new Field("filename", fileName, fieldType);
		Field field2 = new Field("data", String.join(" ", data), fieldType);

		Document document = new Document();
		document.add(field1);
		document.add(field2);

		try {
			writer.addDocument(document);

		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	@Override
	public void reset() 
	{		
		try {
			standardAnalyzer = new StandardAnalyzer();
			directory = new RAMDirectory();
			
			IndexWriterConfig config = new IndexWriterConfig(standardAnalyzer); 
			writer = new IndexWriter(directory, config);
			
			//initialize field
			fieldType = new FieldType();
			fieldType.setStoreTermVectors(true);
			fieldType.setTokenized(true);
			fieldType.setStored(true);
			fieldType.setStoreTermVectorOffsets(true);
			fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		}
		catch(IOException e) {
			e.printStackTrace();
		}	
		
	}
	
	public List<String> VsmQuerySearch(String query) throws IOException
	{
		List<String> docs = new ArrayList<String>();
		
		try {

			writer.close();

			IndexReader reader = DirectoryReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);

			QueryParser parser = new QueryParser("data", standardAnalyzer);

			TopDocs results = null;

			// create the query object and search the document
			results = searcher.search(parser.parse(query), QUERYDOC_COUNT);
			
			int numDocuments = QUERYDOC_COUNT;
			if(results.totalHits < QUERYDOC_COUNT)
				numDocuments = results.totalHits; 
			
			if(numDocuments > 0) {
				
				for(int i=0; i< numDocuments; ++i)
				{
					docs.add(reader.document(results.scoreDocs[i].doc).getField("filename").stringValue());	
				}
			}
			else {
				System.out.println("Query:" + query + "- No document matches the query!");
			}
			
						
			System.out.println("Query: " + query + ". Search results: \n" + String.join(System.lineSeparator(), docs));

			reader.close();
		}	
		catch (IOException | ParseException e) {
			e.printStackTrace();
		}

		return docs;
	}

}
