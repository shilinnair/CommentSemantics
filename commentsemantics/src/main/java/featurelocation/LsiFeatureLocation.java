package featurelocation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
	static String DOCINDEX_PATH = "temp\\docindex";
	static final String DOCFILE_PATH = "temp\\docfiles";
	
	public LsiFeatureLocation()
	{
		reset();
	}
	
	@Override
	public void prepareDocument(String fileName, List<String> data) 
	{		
		//create documents for indexing
		String str =  String.join(" ", data);
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
		boolean cleanIndex = true;
		File index = new File(DOCINDEX_PATH);
		
		String[]entries = index.list();
		if(entries != null) 
		{
			for(String s: entries){
			    File currentFile = new File(index.getPath(),s);
			    cleanIndex &= currentFile.delete();
			}
			cleanIndex &= index.delete();
		}
		
		if(!cleanIndex) {
			Integer rand =  (int) ((Math.random()) * 100000);
			DOCINDEX_PATH = "temp\\index" + rand.toString();			
		}
		
		//create testdata path
		File testdata = new File(DOCFILE_PATH);
		if(!testdata.exists()) {
			testdata.mkdirs();	
		}
		
		//delete doc vec and term vec
		File docVec = new File("docvectors.bin");
		if(docVec.isFile())
			docVec.delete();
		
		File termVec = new File("termvectors.bin");
		if(termVec.isFile())
			termVec.delete();
	}
	
	public List<String> LsiQuerySearch(String query) throws IOException
	{	
		List<String> docs = new ArrayList<String>();
		
		//Create document indexes from comments
	    //String testDataPath = DOCFILE_PATH;
	    String indexCommand = "-luceneindexpath " + DOCINDEX_PATH + " " + DOCFILE_PATH;
	    //IndexFilePositions.main(new String[] {"-luceneindexpath", DOCINDEX_PATH, testDataPath});
	    IndexFilePositions.main(indexCommand.split("\\s+"));
	    
	    		
		try {
			  
			  String buildArgs = new String("-dimension 200 -luceneindexpath ") + DOCINDEX_PATH;
		      BuildIndex.main(buildArgs.split("\\s+"));
		      
		      //method1
		      String Args = new String("-queryvectorfile termvectors.bin -searchvectorfile docvectors.bin -luceneindexpath ");
		      Args += DOCINDEX_PATH + " -numsearchresults 3 ";
		      Args += query;
		      String[] searchArgs = Args.split("\\s+");
		      
		      List<SearchResult> testresults = Search.runSearch(FlagConfig.getFlagConfig(searchArgs));
		      for (SearchResult result : testresults) {
		          String filename = (String) result.getObjectVector().getObject();
		          System.out.println("score:" + result.getScore() + "  file:" + filename);
		          
		          docs.add(filename);
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
		
		return docs;
	}
}
