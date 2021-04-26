package featurelocation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pitt.search.lucene.IndexFilePositions;
import pitt.search.semanticvectors.DocVectors;
import pitt.search.semanticvectors.FlagConfig;
import pitt.search.semanticvectors.LuceneUtils;
import pitt.search.semanticvectors.Search;
import pitt.search.semanticvectors.SearchResult;
import pitt.search.semanticvectors.TermVectorsFromLucene;
import pitt.search.semanticvectors.VectorStore;
import pitt.search.semanticvectors.VectorStoreRAM;
import pitt.search.semanticvectors.VectorStoreWriter;

public class LsiFeatureLocation implements FeatureLocation 
{
	static String DOCINDEX_PATH = "temp\\docindex";
	static String DOCFILE_PATH  = "temp\\docfiles";
	static String TERM_VECTOR   = "temp\\termvectors.bin";
	static String DOC_VECTOR    = "temp\\docvectors.bin";	
	
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
		try {
			
			Integer rand = (int) ((Math.random()) * 100000);
			String randomName = rand.toString();
			
			//clean up doc directory
			File dataDir = new File(DOCFILE_PATH);			
			String[] entries = dataDir.list();
			
			if (entries != null) {
				for (String s : entries) {
					File currentFile = new File(dataDir.getPath(), s);
					currentFile.delete();
				}
				dataDir.delete();
			}			
			
			//create doc dir
			DOCFILE_PATH = "temp\\doc" + randomName;
			File docDir = new File(DOCFILE_PATH);
			if (!docDir.exists()) 
			{
				docDir.mkdirs();
			}
		
			// clean up index directory
			File index = new File(DOCINDEX_PATH);		
			String[] files = index.list();
			
			if (files != null) {
				for (String file : files) {
					File currentFile = new File(index.getPath(), file);
					currentFile.delete();
				}
				index.delete();
			}
		
			// create Lucene Index dir			
			DOCINDEX_PATH = "temp\\index" + randomName;

			// delete doc vec and term vec
			File docVec = new File(DOC_VECTOR);
			if (docVec.isFile()) {
				docVec.delete();
			}

			File termVec = new File(TERM_VECTOR);
			if (termVec.isFile()) {
				termVec.delete();
			}
			
			//new term and doc vectors
			TERM_VECTOR = "temp\\term" + randomName + ".bin";
			DOC_VECTOR  = "temp\\doc" + randomName + ".bin";

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	public List<String> LsiQuerySearch(String query) throws IOException 
	{
		List<String> docs = new ArrayList<String>();

		try 
		{			
			String Args = new String("-queryvectorfile ") + TERM_VECTOR;
			Args += " -searchvectorfile " + DOC_VECTOR;
			Args += " -luceneindexpath " + DOCINDEX_PATH;
			Args += " -numsearchresults 3 " + query;
			String[] searchArgs = Args.split("\\s+");

			List<SearchResult> testresults = Search.runSearch(FlagConfig.getFlagConfig(searchArgs));
			for (SearchResult result : testresults) {
				String fullPath = (String) result.getObjectVector().getObject();
				String fileName = fullPath.substring(fullPath.lastIndexOf("\\") + 1);
				
				System.out.println("score:" + result.getScore() + "  file:" + fileName);
				docs.add(fileName);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return docs;
	}
	
	public void buildSemanticVectors()
	{
		try 
		{
			//build position indexes from tokens
			String indexCommand = "-luceneindexpath " + DOCINDEX_PATH + " " + DOCFILE_PATH;
			IndexFilePositions.main(indexCommand.split("\\s+"));
			
			//build semantic vectors		
			String buildArgs = new String("-dimension 200 -luceneindexpath ") + DOCINDEX_PATH;
	
			FlagConfig flagConfig;
			flagConfig = FlagConfig.getFlagConfig(buildArgs.split("\\s+"));
			LuceneUtils utils = new LuceneUtils(flagConfig);
	
			// build term vector Indexer
			TermVectorsFromLucene termVectorIndexer;
			if (!flagConfig.initialtermvectors().isEmpty()) {
				termVectorIndexer = TermVectorsFromLucene.createTermBasedRRIVectors(flagConfig);
			} else {
				VectorStore initialDocVectors = null;
				if (!flagConfig.initialdocumentvectors().isEmpty()) {
					initialDocVectors = VectorStoreRAM.readFromFile(flagConfig, flagConfig.initialdocumentvectors());
				}
	
				termVectorIndexer = TermVectorsFromLucene.createTermVectorsFromLucene(flagConfig, initialDocVectors);
			}
	
			// build doc vector Indexer
			DocVectors docVectors = new DocVectors(termVectorIndexer.getSemanticTermVectors(), flagConfig, utils);
			for (int i = 1; i < flagConfig.trainingcycles(); ++i) {
				termVectorIndexer = TermVectorsFromLucene.createTermVectorsFromLucene(flagConfig, docVectors);
				docVectors = new DocVectors(termVectorIndexer.getSemanticTermVectors(), flagConfig, utils);
			}
	
			// write term and doc vector store
			VectorStore writeableDocVectors = docVectors.makeWriteableVectorStore();
			VectorStoreWriter.writeVectors(TERM_VECTOR, flagConfig, termVectorIndexer.getSemanticTermVectors());
			VectorStoreWriter.writeVectors(DOC_VECTOR, flagConfig, writeableDocVectors);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
