package queryresult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ResultStore 
{
	private FileWriter Writer; 
	
	public void OpenStore(String name)
	{
		File resultDir = new File("./result/");
		if(!resultDir.exists())
			resultDir.mkdir();	
		
		String fileName = "./result/" + name.replaceAll("\\s", "") + ".txt";
		
		try 
		{
			Writer = new FileWriter(fileName,true);
		}
		catch (IOException e) 
		{		
			e.printStackTrace();
		}
	}
	
	public void CloseStore()
	{		
		try {
			Writer.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void WriteData(String data)
	{
		try 
		{
			Writer.write(data);
			Writer.write(System.lineSeparator());
		} 
		catch (IOException e) 
		{		
			e.printStackTrace();
		}
	}
	
	
	public void PersistLsiQueryResult(List<String> docs)
	{					
		WriteData("LSIResult:" + String.join(" ", docs));				
	}
	
	public void PersistVsmQueryResult(List<String> docs)
	{		
		WriteData("VSMResult:" + String.join(" ", docs));				
	}

	public void PersistSimilarDocResult(List<String> similarDocs) 
	{		
		WriteData("Similarity:" + String.join(" ", similarDocs));			
	}
	
	public void PrintLineSeperator()
	{
		WriteData("========================================================================================");
	}
}
