                                 package featurelocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;


public class VsmDocSimilarity implements FeatureLocation 
{
	private StandardAnalyzer standardAnalyzer = null;
	private Directory directory = null;
	private IndexWriter writer = null;
	private FieldType fieldType = null;
	
	public VsmDocSimilarity()
	{
		reset();
	}
	
	
	@Override
	public void prepareDocument(String fileName, List<String> data)
	{	
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
	
	public List<String> vsmGetSimilarDocuments(String fileName) throws IOException
	{	
		List<String> similarDocuments = new ArrayList<String>();
		
		if (writer.isOpen())
			writer.close();

		IndexReader reader = DirectoryReader.open(directory);		

		List<RealVector> realVectors = new ArrayList<RealVector>();
		ArrayList<String> allTerms = getAllTerms(reader);

		Integer fileindex = 0;

		for (int i = 0; i < reader.numDocs(); i++) 
		{
			Terms vector = reader.getTermVector(i, "data");   
			realVectors.add(getRealVector(vector, allTerms, reader));
		
			//find the index of file name
			if (reader.document(i).getField("filename").stringValue().compareToIgnoreCase(fileName) == 0) 
				fileindex = i;			
		}

		RealVector rvInputFile = realVectors.get(fileindex); // get the real vector associated with input file

		Map<Double, Integer> cosineMap = new HashMap<Double, Integer>(); // cosine value : index

		for (int i = 0; i < realVectors.size(); i++) 
		{
			if (i == fileindex)
				continue; // do not compare self

			try {

				double cosineValue = rvInputFile.cosine(realVectors.get(i));
				cosineMap.put(cosineValue, i);

			} catch (ArithmeticException e) {
				System.out.println("ArithmeticException - " + e.getMessage());
			}
		}

		// we have the cosinemap ready, now find the highest similarity document
		List<Double> sortedCosine = new ArrayList<Double>(cosineMap.keySet());
		Collections.sort(sortedCosine);

		System.out.println("Finding similar documents to: " + reader.document(fileindex).getField("filename").stringValue());
		
		similarDocuments.add(reader.document(fileindex).getField("filename").stringValue());

		if (sortedCosine.size() > 0 && sortedCosine.get(sortedCosine.size() - 1) > 0) { // do not add if cosine value is zero
			
			Double cos_key1 = sortedCosine.get(sortedCosine.size() - 1);
			similarDocuments.add(reader.document(cosineMap.get(cos_key1)).getField("filename").stringValue());
			System.out.println("document 1 cosine score:" + cos_key1);
		}

		if (sortedCosine.size() > 1 && sortedCosine.get(sortedCosine.size() - 2) > 0) { // do not add if cosine value is zero
			
			Double cos_key2 = sortedCosine.get(sortedCosine.size() - 2);
			similarDocuments.add(reader.document(cosineMap.get(cos_key2)).getField("filename").stringValue());
			System.out.println("document 2 cosine score:" + cos_key2);
		}
		
		if (sortedCosine.size() > 2 && sortedCosine.get(sortedCosine.size() - 3) > 0) { // do not add if cosine value is zero
			
			Double cos_key3 = sortedCosine.get(sortedCosine.size() - 3);
			similarDocuments.add(reader.document(cosineMap.get(cos_key3)).getField("filename").stringValue());
			System.out.println("document 3 cosine score:" + cos_key3);
		}
		
		if (sortedCosine.size() > 3 && sortedCosine.get(sortedCosine.size() - 4) > 0) { // do not add if cosine value is zero
					
			Double cos_key4 = sortedCosine.get(sortedCosine.size() - 4);
			similarDocuments.add(reader.document(cosineMap.get(cos_key4)).getField("filename").stringValue());
			System.out.println("document 4 cosine score:" + cos_key4);
		}
		
		System.out.println("Similar documets are:");
		System.out.println(String.join(System.lineSeparator(), similarDocuments));
		
		reader.close();
		
		return similarDocuments;
	}
	
	
	static RealVector getRealVector(Terms vector, ArrayList<String> allTerms, IndexReader reader) throws IOException 
	{
		RealVector R_Vector = new ArrayRealVector((int) allTerms.size());
		
		if(vector == null)
			return R_Vector;

		TermsEnum terms = vector.iterator();

		int n=0;

		while(terms.next() != null) 
		{
			n = allTerms.indexOf(terms.term().utf8ToString());
	
			TFIDFSimilarity  kk1 = new ClassicSimilarity();
			Term term = new Term("data", terms.term().utf8ToString() );
			
			double tf_idf = kk1.idf(reader.docFreq(term), reader.numDocs())*kk1.tf(terms.totalTermFreq());
			R_Vector.setEntry(n, tf_idf);
		}
		
		return R_Vector;

	}
	
	static ArrayList<String> getAllTerms(IndexReader reader) throws IOException 
	{
		Set<String> allTerm = new HashSet<String>();

		for(int i=0;i<reader.numDocs();i++) 
		{
			Terms vector = reader.getTermVector(i, "data");
			
			if(vector == null)
				continue;
	
			TermsEnum terms = vector.iterator();		 
			while(terms.next() != null) {
				allTerm.add(terms.term().utf8ToString());
			}			
		}

		ArrayList<String> res = new ArrayList<String>(allTerm);
		return res;
	}

}
