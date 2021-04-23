package queryresult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ResultStore 
{
	private FileWriter Writer; 
	
	public void OpenStore(String path)
	{
		File resultDir = new File("./result/");
		if(!resultDir.exists())
			resultDir.mkdir();	
		
		String fileName = "./result/" + path.replaceAll("\\s", "") + ".txt";
		
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
	
	
	public void PersistLsiQueryResult(String query, List<String> docs)
	{
		WriteData("LSI");
		WriteData("Query:" + query);			
		WriteData("Search result:" + String.join(" ", docs));		
		WriteData("================================================================================");		
	}
	
	public void PersistVsmQueryResult(String query, List<String> docs)
	{
		WriteData("VSM");
		WriteData("Query:" + query);			
		WriteData("Search result:" + String.join(" ", docs));		
		WriteData("================================================================================");		
	}
}
