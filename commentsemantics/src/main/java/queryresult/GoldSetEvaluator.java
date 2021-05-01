package queryresult;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
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
		private Float finalReciprocalRank = 0.0f;
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
		if(goldResults == null)
			return;
		
		int numMachingDocs = 0;
		Float indexPrecision = 0.0f;
		Float ReciprocalRank = 0.0f;
		
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
				
				if(ReciprocalRank == 0.0f) {  //find the top document RR
					ReciprocalRank =  1.0f / (index+1);
				}						
			} 
		}
		
		if(numMachingDocs > 0) {
			
			Float precision = (float)indexPrecision / (float)numMachingDocs;
			Float recall = (float)numMachingDocs / (float)TruePositives;
			Float fscore = (2 * precision * recall) / (precision + recall);
			
			if(flType == FLType.VSM) {
				vsmScore.finalPrecision += precision;
				vsmScore.finalRecall += recall;
				vsmScore.finalFScore += fscore;			
				vsmScore.finalReciprocalRank += ReciprocalRank;
				vsmScore.totalExecution++;
			}
			else {
				lsiScore.finalPrecision += precision;
				lsiScore.finalRecall += recall;
				lsiScore.finalFScore += fscore;
				lsiScore.finalReciprocalRank += ReciprocalRank;
				lsiScore.totalExecution++;			
			}
			
			resultStore.WriteData("AveragePrecision:" + String.format("%.02f", precision));
			resultStore.WriteData("Recall:" + String.format("%.02f", recall));		
			resultStore.WriteData("FScore:" + String.format("%.02f", fscore));
			resultStore.WriteData("ReciprocalRank:" + String.format("%.02f", ReciprocalRank));
		}
		
	}
	
	public void EvaluateSimilairyResult(FLType flType, Integer queryNumber, List<String> similarityResult)
	{			
		List<String> goldResults = goldsetResults.get(queryNumber);
		if(goldResults == null)
			return;
		
		int numMachingDocs = 0;
		Float indexPrecision = 0.0f; 
		Float ReciprocalRank = 0.0f;
		
		final int TruePositives = goldResults.size();
		
		for(int i=0; i< similarityResult.size(); ++i)
		{
			String similarDoc = similarityResult.get(i);
			
			if(goldResults.contains(similarDoc))
			{
				numMachingDocs++;
				
				int index = goldResults.indexOf(similarDoc);		
				if(index < TruePositives) {
					indexPrecision += 1.0f;
				}
				else {
					indexPrecision += ((float)TruePositives / (index + 1));
				}
				
				if(ReciprocalRank == 0.0f) {  //find the top document RR
					ReciprocalRank =  1.0f / (index+1);
				}
			}
		}
		
		if(numMachingDocs > 0) {
		
			Float precision = (float)indexPrecision / (float)numMachingDocs;
			Float recall = (float)numMachingDocs / (float)TruePositives;
			Float fscore = (2 * precision * recall) / (precision + recall);
			
			if(flType == FLType.VSM) {
				vsmSimilarityScore.finalPrecision += precision;
				vsmSimilarityScore.finalRecall += recall;
				vsmSimilarityScore.finalFScore += fscore;			
				vsmSimilarityScore.finalReciprocalRank += ReciprocalRank;
				vsmSimilarityScore.totalExecution++;
			}
			else {
				lsiSimilarityScore.finalPrecision += precision;
				lsiSimilarityScore.finalRecall += recall;
				lsiSimilarityScore.finalFScore += fscore;
				lsiSimilarityScore.finalReciprocalRank += ReciprocalRank;
				lsiSimilarityScore.totalExecution++;			
			}
			
			resultStore.WriteData("SimilarityAveragePrecision:" + String.format("%.02f", precision));
			resultStore.WriteData("SimilarityRecall:" + String.format("%.02f", recall));		
			resultStore.WriteData("SimilarityFScore:" + String.format("%.02f", fscore));
			resultStore.WriteData("SimilarityReciprocalRank:" + String.format("%.02f", ReciprocalRank));
		}
	}
	
	public void printFinalScore(String executionName) 
	{
		resultStore.WriteData("Execution Mode:" + executionName);
		
		//vsm final score
		if(vsmScore.totalExecution > 0) {
			vsmScore.finalPrecision = vsmScore.finalPrecision / vsmScore.totalExecution;
			vsmScore.finalRecall = vsmScore.finalRecall / vsmScore.totalExecution;
			vsmScore.finalFScore = vsmScore.finalFScore / vsmScore.totalExecution;
			vsmScore.finalReciprocalRank = vsmScore.finalReciprocalRank / vsmScore.totalExecution;
			
			resultStore.WriteData("VSM Scores:");
			resultStore.WriteData("MeanAveragePrecision(mAP):" + String.format("%.02f",vsmScore.finalPrecision));
			resultStore.WriteData("MeanRecall(mR):" + String.format("%.02f",vsmScore.finalRecall));
			resultStore.WriteData("MeanF-Score(mFS):" + String.format("%.02f",vsmScore.finalFScore));
			resultStore.WriteData("MeanReciprocalRank(mRR):" + String.format("%.02f",vsmScore.finalReciprocalRank));
			System.out.println("VSM excution Scores - MeanAveragePrecision(mAP):" + vsmScore.finalPrecision + " MeanRecall(mR):" + vsmScore.finalRecall + 
					" MeanF-Score(mFS):" + vsmScore.finalFScore + " MeanReciprocalRank(mRR):" + vsmScore.finalReciprocalRank);
		}
		
		//lsi final score
		if(lsiScore.totalExecution > 0) {
			lsiScore.finalPrecision = lsiScore.finalPrecision / lsiScore.totalExecution;
			lsiScore.finalRecall = lsiScore.finalRecall / lsiScore.totalExecution;
			lsiScore.finalFScore = lsiScore.finalFScore / lsiScore.totalExecution;
			lsiScore.finalReciprocalRank = lsiScore.finalReciprocalRank / lsiScore.totalExecution;
			
			resultStore.WriteData("LSI Scores:");
			resultStore.WriteData("MeanAveragePrecision(mAP):" + String.format("%.02f",lsiScore.finalPrecision));
			resultStore.WriteData("MeanRecall(mR):" + String.format("%.02f",lsiScore.finalRecall));
			resultStore.WriteData("MeanF-Score(mFS):" + String.format("%.02f",lsiScore.finalFScore));
			resultStore.WriteData("MeanReciprocalRank(mRR):" + String.format("%.02f",lsiScore.finalReciprocalRank));
			System.out.println("LSI excution Scores - MeanAveragePrecision(mAP):" + lsiScore.finalPrecision + " MeanRecall(mR):" + lsiScore.finalRecall + 
					" MeanF-Score(mFS):" + lsiScore.finalFScore + " MeanReciprocalRank(mRR):" + lsiScore.finalReciprocalRank);
		}
		
		//vsmsimilarity final score
		if(vsmSimilarityScore.totalExecution > 0) {
			vsmSimilarityScore.finalPrecision = vsmSimilarityScore.finalPrecision / vsmSimilarityScore.totalExecution;
			vsmSimilarityScore.finalRecall = vsmSimilarityScore.finalRecall / vsmSimilarityScore.totalExecution;
			vsmSimilarityScore.finalFScore = vsmSimilarityScore.finalFScore / vsmSimilarityScore.totalExecution;
			vsmSimilarityScore.finalReciprocalRank = vsmSimilarityScore.finalReciprocalRank / vsmSimilarityScore.totalExecution;
			
			resultStore.WriteData("VSM Similarity Scores:");
			resultStore.WriteData("MeanAveragePrecision(mAP):" + String.format("%.02f",vsmSimilarityScore.finalPrecision));
			resultStore.WriteData("MeanRecall(mR):" + String.format("%.02f",vsmSimilarityScore.finalRecall));
			resultStore.WriteData("MeanF-Score(mFS):" + String.format("%.02f",vsmSimilarityScore.finalFScore));
			resultStore.WriteData("MeanReciprocalRank(mRR):" + String.format("%.02f",vsmSimilarityScore.finalReciprocalRank));
			System.out.println("VSM Similarity excution Scores - MeanAveragePrecision(mAP):" + vsmSimilarityScore.finalPrecision + " MeanRecall(mR):" + 
					vsmSimilarityScore.finalRecall + " MeanF-Score(mFS):" + vsmSimilarityScore.finalFScore + " MeanReciprocalRank(mRR):" + vsmSimilarityScore.finalReciprocalRank);
		}
		
		//lsisimilarity final score
		if(lsiSimilarityScore.totalExecution > 0) {
			lsiSimilarityScore.finalPrecision = lsiSimilarityScore.finalPrecision / lsiSimilarityScore.totalExecution;
			lsiSimilarityScore.finalRecall = lsiSimilarityScore.finalRecall / lsiSimilarityScore.totalExecution;
			lsiSimilarityScore.finalFScore = lsiSimilarityScore.finalFScore / lsiSimilarityScore.totalExecution;
			lsiSimilarityScore.finalReciprocalRank = lsiSimilarityScore.finalReciprocalRank / lsiSimilarityScore.totalExecution;
			
			resultStore.WriteData("LSI Similarity Scores:");
			resultStore.WriteData("MeanAveragePrecision(mAP):" + String.format("%.02f",lsiSimilarityScore.finalPrecision));
			resultStore.WriteData("MeanRecall(mR):" + String.format("%.02f",lsiSimilarityScore.finalRecall));
			resultStore.WriteData("FScore:" + String.format("%.02f",lsiSimilarityScore.finalFScore));
			resultStore.WriteData("MeanReciprocalRank(mRR):" + String.format("%.02f",lsiSimilarityScore.finalReciprocalRank));
			System.out.println("LSI Similariy excution Scores - MeanAveragePrecision(mAP):" + lsiSimilarityScore.finalPrecision + " MeanRecall(mR):" + 
					lsiSimilarityScore.finalRecall + " MeanF-Score(mFS):" + lsiSimilarityScore.finalFScore + " MeanReciprocalRank(mRR):" + lsiSimilarityScore.finalReciprocalRank);
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
	}

}
