package featurelocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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
	
	public List<String> querySearch(String query) throws IOException
	{
		List<String> docs = new ArrayList<String>();
		
		return docs;
	}

}
