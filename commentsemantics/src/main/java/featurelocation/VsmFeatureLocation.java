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
	private VsmDocSimilarity vsmDocSimilarity;
	
	private StandardAnalyzer standardAnalyzer = null;
	private Directory directory = null;
	private IndexWriter writer = null;
	private FieldType fieldType = null;

	public VsmFeatureLocation(VsmDocSimilarity vsmDocSimilarity)
	{
		this.vsmDocSimilarity = vsmDocSimilarity;
		reset();
	}

	@Override
	public void prepareDocument(String fileName, List<String> data) 
	{
		if(data.isEmpty())
			return;
		
		//pass it on to doc similarity
		vsmDocSimilarity.prepareDocument(fileName, data);
		
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
		vsmDocSimilarity.reset();
		
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
		
		List<String> similarDocs = null;
		
		try {

			//if (writer.isOpen())
				writer.close();

			IndexReader reader = DirectoryReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);

			QueryParser parser = new QueryParser("data", standardAnalyzer);

			TopDocs results = null;

			// create the query object and search the document
			results = searcher.search(parser.parse(query), 3);
			

			if (results.totalHits > 0) {
				docs.add(reader.document(results.scoreDocs[0].doc).getField("filename").stringValue());		
				
				System.out.println("Query search returned top document: " + docs.get(0));
				
				//Now we have the top document matching the query. Find the similar document using the VSM cosine similarity.
				similarDocs = vsmDocSimilarity.vsmGetSimilarDocuments(docs.get(0));
			} 
			else {
				System.out.println("Query:" + query + "- No document matches the query!");
			}

			if (results.totalHits > 1) {
				docs.add(reader.document(results.scoreDocs[1].doc).getField("filename").stringValue());				
			}
			if (results.totalHits > 2) {
				docs.add(reader.document(results.scoreDocs[2].doc).getField("filename").stringValue());				
			}
			
			System.out.println("Query: " + query + ". Search results: \n" + String.join(System.lineSeparator(), docs));

			reader.close();
		}	
		catch (IOException | ParseException e) {
			e.printStackTrace();
		}

		
		if(!docs.isEmpty() && !similarDocs.isEmpty())
		{
			//natural similarity comparison
			System.out.println("Natural similarity of query results with vsm dcument similarity;");
			
			for(String doc : docs) {
				
				if(similarDocs.contains(doc))
				{
					System.out.println("query result match with natural similarity, document:" + doc);
				}
				else
				{
					System.out.println("Doc not found in similarity docs, document:" + doc);
				}
			}				
				
		}
		
		return docs;
	}

}
