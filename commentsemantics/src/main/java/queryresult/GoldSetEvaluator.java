package queryresult;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class GoldSetEvaluator 
{
	public static final String Goldset_Src = "Goldset\\Src\\ecf\\";
	public static final String Goldset_Query = "Goldset\\Query\\ecf\\";
	public static final String Goldset_Result = "Goldset\\Result\\ecf\\";
	
	public enum FLType {VSM, LSI}
	
		
	private ResultStore resultStore;
	
	private Map<Integer, List<String>> goldsetResults = new HashMap<Integer, List<String>>();
	private Map<Integer, String> goldsetQueries = new HashMap<Integer, String>();
	
	private class Score
	{
		private Float finalPrecision = 0.0f;
		private Float finalRecall = 0.0f;
		private Float finalFScore = 0.0f;
		private int totalExecution = 0;	
	}
	
	private Score vsmScore = new Score();
	private Score lsiScore = new Score();
	private Score vsmSimilarityScore = new Score();
	private Score lsiSimilarityScore = new Score();
	
	public GoldSetEvaluator(ResultStore store)
	{
		this.resultStore = store;
		populateGoldSet();
	}
	
	public void Reset()
	{
		vsmScore = new Score();
		lsiScore = new Score();
		vsmSimilarityScore = new Score();
		lsiSimilarityScore = new Score();
	}
	
	public Map<Integer, String> GetQueries()
	{
		return goldsetQueries;		
	}
	
	public void EvaluateQueryResult(FLType flType, Integer queryNumber, List<String> queryResult)
	{
		List<String> goldResults = goldsetResults.get(queryNumber);
		
		int numMachingDocs = 0;
		Float indexPrecision = 0.0f; 
		
		final int TruePositives = goldResults.size();
		
		for(int i=0; i< goldResults.size(); ++i)
		{
			String goldresult = goldResults.get(i);
			
			if(queryResult.contains(goldresult))
			{
				numMachingDocs++;
			
				int index = queryResult.indexOf(goldresult);		
				if(index < TruePositives) {
					indexPrecision += 1.0f;
				}
				else {
					indexPrecision += ((float)TruePositives / (index + 1));
				}
					
			} 
		}
		
		Float precision = (float)indexPrecision / (float)numMachingDocs;
		Float recall = (float)numMachingDocs / (float)TruePositives;
		Float fscore = (2 * precision * recall) / (precision + recall);
		
		if(flType == FLType.VSM) {
			vsmScore.finalPrecision += precision;
			vsmScore.finalRecall += recall;
			vsmScore.finalFScore += fscore;			
			vsmScore.totalExecution++;
		}
		else {
			lsiScore.finalPrecision += precision;
			lsiScore.finalRecall += recall;
			lsiScore.finalFScore += fscore;			
			lsiScore.totalExecution++;			
		}
		
		resultStore.WriteData("Precision:" + String.format("%.02f", precision));
		resultStore.WriteData("Recall:" + String.format("%.02f", recall));		
		resultStore.WriteData("FScore:" + String.format("%.02f", fscore));
	}
	
	public void EvaluateSimilairyResult(FLType flType, List<String> similarityResult, List<String> queryResult)
	{			
		int numMachingDocs = 0;
		Float indexPrecision = 0.0f; 
		
		final int TruePositives = similarityResult.size();
		
		for(int i=0; i< similarityResult.size(); ++i)
		{
			String similarDoc = similarityResult.get(i);
			
			if(queryResult.contains(similarDoc))
			{
				numMachingDocs++;
				
				int index = queryResult.indexOf(similarDoc);		
				if(index < TruePositives) {
					indexPrecision += 1.0f;
				}
				else {
					indexPrecision += ((float)TruePositives / (index + 1));
				}
			}
		}
		
		Float precision = (float)indexPrecision / (float)numMachingDocs;
		Float recall = (float)numMachingDocs / (float)TruePositives;
		Float fscore = (2 * precision * recall) / (precision + recall);
		
		if(flType == FLType.VSM) {
			vsmSimilarityScore.finalPrecision += precision;
			vsmSimilarityScore.finalRecall += recall;
			vsmSimilarityScore.finalFScore += fscore;			
			vsmSimilarityScore.totalExecution++;
		}
		else {
			lsiSimilarityScore.finalPrecision += precision;
			lsiSimilarityScore.finalRecall += recall;
			lsiSimilarityScore.finalFScore += fscore;			
			lsiSimilarityScore.totalExecution++;			
		}
		
		resultStore.WriteData("SimilarityPrecision:" + String.format("%.02f", precision));
		resultStore.WriteData("SimilarityRecall:" + String.format("%.02f", recall));		
		resultStore.WriteData("SimilarityFScore:" + String.format("%.02f", fscore));
	}
	
	public void printFinalScore(String executionName) 
	{
		resultStore.WriteData("Execution Mode:" + executionName);
		
		//vsm final score
		if(vsmScore.totalExecution > 0) {
			vsmScore.finalPrecision = vsmScore.finalPrecision / vsmScore.totalExecution;
			vsmScore.finalRecall = vsmScore.finalRecall / vsmScore.totalExecution;
			vsmScore.finalFScore = vsmScore.finalFScore / vsmScore.totalExecution;
			
			resultStore.WriteData("VSM Scores:");
			resultStore.WriteData("Precision:" + String.format("%.02f",vsmScore.finalPrecision));
			resultStore.WriteData("Recall:" + String.format("%.02f",vsmScore.finalRecall));
			resultStore.WriteData("FScore:" + String.format("%.02f",vsmScore.finalFScore));
			System.out.println("VSM excution Scores - Precision:" + vsmScore.finalPrecision + " Recall:" + vsmScore.finalRecall + " FScore:" + vsmScore.finalFScore);
		}
		
		//lsi final score
		if(lsiScore.totalExecution > 0) {
			lsiScore.finalPrecision = lsiScore.finalPrecision / lsiScore.totalExecution;
			lsiScore.finalRecall = lsiScore.finalRecall / lsiScore.totalExecution;
			lsiScore.finalFScore = lsiScore.finalFScore / lsiScore.totalExecution;
			
			resultStore.WriteData("LSI Scores:");
			resultStore.WriteData("Precision:" + String.format("%.02f",lsiScore.finalPrecision));
			resultStore.WriteData("Recall:" + String.format("%.02f",lsiScore.finalRecall));
			resultStore.WriteData("FScore:" + String.format("%.02f",lsiScore.finalFScore));
			System.out.println("LSI excution Scores - Precision:" + lsiScore.finalPrecision + " Recall:" + lsiScore.finalRecall + " FScore:" + lsiScore.finalFScore);
		}
		
		//vsmsimilarity final score
		if(vsmSimilarityScore.totalExecution > 0) {
			vsmSimilarityScore.finalPrecision = vsmSimilarityScore.finalPrecision / vsmSimilarityScore.totalExecution;
			vsmSimilarityScore.finalRecall = vsmSimilarityScore.finalRecall / vsmSimilarityScore.totalExecution;
			vsmSimilarityScore.finalFScore = vsmSimilarityScore.finalFScore / vsmSimilarityScore.totalExecution;
			
			resultStore.WriteData("VSM Similarity Scores:");
			resultStore.WriteData("Precision:" + String.format("%.02f",vsmSimilarityScore.finalPrecision));
			resultStore.WriteData("Recall:" + String.format("%.02f",vsmSimilarityScore.finalRecall));
			resultStore.WriteData("FScore:" + String.format("%.02f",vsmSimilarityScore.finalFScore));
			System.out.println("VSM Similarity excution Scores - Precision:" + vsmSimilarityScore.finalPrecision + " Recall:" + vsmSimilarityScore.finalRecall + " FScore:" + vsmSimilarityScore.finalFScore);
		}
		
		//lsisimilarity final score
		if(lsiSimilarityScore.totalExecution > 0) {
			lsiSimilarityScore.finalPrecision = lsiSimilarityScore.finalPrecision / lsiSimilarityScore.totalExecution;
			lsiSimilarityScore.finalRecall = lsiSimilarityScore.finalRecall / lsiSimilarityScore.totalExecution;
			lsiSimilarityScore.finalFScore = lsiSimilarityScore.finalFScore / lsiSimilarityScore.totalExecution;
			
			resultStore.WriteData("LSI Similarity Scores:");
			resultStore.WriteData("Precision:" + String.format("%.02f",lsiSimilarityScore.finalPrecision));
			resultStore.WriteData("Recall:" + String.format("%.02f",lsiSimilarityScore.finalRecall));
			resultStore.WriteData("FScore:" + String.format("%.02f",lsiSimilarityScore.finalFScore));
			System.out.println("LSI Similariy excution Scores - Precision:" + lsiSimilarityScore.finalPrecision + " Recall:" + lsiSimilarityScore.finalRecall + " FScore:" + lsiSimilarityScore.finalFScore);
		}
		
		resultStore.PrintLineSeperator();				
	}
	
	private void populateGoldSet()
	{	
		Scanner scanner = null;
		
		//populate query
		try {
			scanner = new Scanner(new File(Goldset_Query + "proposed-NL.txt"));
			while (scanner.hasNextLine()) 
			{
			   String queryLine = scanner.nextLine();
			   
			   String queryNumber = queryLine.substring(0, 6);
			   String query = queryLine.substring(7);
			   
			   if(queryNumber.trim().length() < 6)
				   query = queryLine.substring(6);
			   
			   goldsetQueries.put(Integer.parseInt(queryNumber.trim()), query);
			}			
		} 
		catch (FileNotFoundException e) {			
			e.printStackTrace();
		}
		finally {
			scanner.close();
		}
		
		//populate results
		try {
			scanner = new Scanner(new File(Goldset_Result + "proposed-NL.txt"));
			List<String> tempResults = new ArrayList<>();
			Integer queryDigits = 0;
			
			while (scanner.hasNextLine()) 
			{
			   String resultLine = scanner.nextLine();
			   
			   String queryNumber = resultLine.substring(0, 6);
			   String javaFile = resultLine.substring(7);
			   
			   if(queryDigits != Integer.parseInt(queryNumber.trim()) && tempResults.size() > 0) 
			   {
				   List<String> resultFiles = new ArrayList<>();
				   
				   //we have got the list of results for the query. Put it into the result map
				   for(String fullPath : tempResults) {
					   String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
					   resultFiles.add(fileName.split(("\\s+"))[0]);
				   }
				   goldsetResults.put(queryDigits, resultFiles);
				   
				   //begin new query and its set of results
				   tempResults.clear();
			   }
			   
				queryDigits = Integer.parseInt(queryNumber.trim());
				tempResults.add(javaFile);				   			   
			}			
		} 
		catch (FileNotFoundException e) {			
			e.printStackTrace();
		}
		finally {
			scanner.close();
		}
		
		
		//test query
		//goldsetQueries.put(112599, "Bug XMPP Room subject updated xmpp chat updated remotely xmpp server title room updated dynamically Bug XMPP Room subject updated xmpp chat room message user chat presence listener invitation connect");
		//goldsetQueries.put(119206, "Bug Add event history hyperlinks collab text chat output problem shared editor collab history participant opens shared editor receiver record shared editor opening replay event control shared editor ECF collab features display editor opening editor selection events chat text output hyperlinks ability receiver click hyperlinks locally remotely replay event receiver preference received events executed shared editor executed presented text chat history Provide additional events resources share key board Bug Add event history hyperlinks collab text chat output chat message send file user handle text create");
		//goldsetQueries.put(172958, "Bug IRC Patch commands Root Channel Containers commands work Channel Container setup command containers patch acceptable write mode commands Mark Bug IRC Patch commands Root Channel Containers channel room message handle type user container connect");
		
		//test result
		String javaFils = "1691.java 2220.java	820.java 1991.java 281.java	1347.java 1834.java	2126.java 241.java 2613.java"; 
		goldsetResults.put(112599, Arrays.asList(javaFils.split("\\s+")));
		
		javaFils = "2397.java 2559.java	901.java 478.java 754.java 461.java	2189.java 1818.java	293.java 2099.java"; 
		goldsetResults.put(119206, Arrays.asList(javaFils.split("\\s+")));
		
		javaFils = "1329.java 2126.java	2300.java 318.java 798.java	1692.java 1380.java	1834.java 1209.java	2008.java"; 
		goldsetResults.put(172958, Arrays.asList(javaFils.split("\\s+")));
	}

}
