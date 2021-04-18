package featurelocation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import pitt.search.lucene.IndexFilePositions;
import pitt.search.semanticvectors.BuildIndex;
import pitt.search.semanticvectors.CloseableVectorStore;
import pitt.search.semanticvectors.FlagConfig;
import pitt.search.semanticvectors.LuceneUtils;
import pitt.search.semanticvectors.Search;
import pitt.search.semanticvectors.SearchResult;
import pitt.search.semanticvectors.VectorSearcher;
import pitt.search.semanticvectors.VectorStoreReader;

public class LsiFeatureLocation implements FeatureLocation 
{
	static final String DOCINDEX_PATH = "document_index";
	static final String DOCFILE_PATH = "document_files";
	
	public LsiFeatureLocation()
	{
		reset();
	}
	
	@Override
	public void prepareDocument(String fileName, List<String> artefact, List<String> comments) 
	{		
		//create documents for indexing
		String str =  String.join(" ", comments);
	    BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(DOCFILE_PATH + "\\" +  fileName));
			writer.write(str);		    
		    writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void reset() 
	{
		//clean up index directory
		File index = new File(DOCINDEX_PATH);
		
		String[]entries = index.list();
		if(entries != null) 
		{
			for(String s: entries){
			    File currentFile = new File(index.getPath(),s);
			    currentFile.delete();
			}
			index.delete();
		}
		
		//create testdata path
		File testdata = new File(DOCFILE_PATH);
		if(!testdata.exists())
			testdata.mkdir();			
	}
	
	public void printSimilarDocuments() throws IOException
	{	
		//Create document indexes from comments
	    String testDataPath = DOCFILE_PATH;	    	    
	    IndexFilePositions.main(new String[] {"-luceneindexpath", DOCINDEX_PATH, testDataPath});
	   
	    
	    String[] buildArgs = new String("-dimension 200 -luceneindexpath document_index").split("\\s+");
		
		try {
		      BuildIndex.main(buildArgs);
		      
		      //method1
		      String[] searchArgs = new String("-queryvectorfile termvectors.bin -searchvectorfile docvectors.bin -luceneindexpath document_index -numsearchresults 3 block line java").split("\\s+");
		      
		      List<SearchResult> testresults = Search.runSearch(FlagConfig.getFlagConfig(searchArgs));
		      for (SearchResult result : testresults) {
		          String filename = (String) result.getObjectVector().getObject();
		          System.out.println("score:" + result.getScore() + "  file:" + filename);
		      }
		      
		      
		      //method2
		      FlagConfig config = FlagConfig.getFlagConfig(searchArgs);
		      CloseableVectorStore queryVecReader = VectorStoreReader.openVectorStore(config.termvectorsfile(), config); 
		      CloseableVectorStore resultsVecReader = VectorStoreReader.openVectorStore(config.docvectorsfile(), config);
		      LuceneUtils luceneUtils = new LuceneUtils(config);; 
		      VectorSearcher  vecSearcher = new VectorSearcher.VectorSearcherCosine( 
		                      queryVecReader, resultsVecReader, luceneUtils, config, new String[] {"block", "line", "java"}); 
		      LinkedList<SearchResult> results1 = vecSearcher.getNearestNeighbors(3);

		      for (SearchResult result: results1) {
		        System.out.println(String.format(
		            "%f:%s",
		            result.getScore(),
		            result.getObjectVector().getObject().toString()));
		      }	      
		      
		}
		catch (Exception e) {
		      e.printStackTrace();
		}
		
		
	}
}
