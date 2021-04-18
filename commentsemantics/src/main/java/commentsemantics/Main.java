package commentsemantics;

import java.io.File;
import java.io.IOException;

import featurelocation.LsiFeatureLocation;
import featurelocation.VsmFeatureLocation;
import parser.FileParser;
import parser.ProjectParser;

public class Main {
	
	
	
	private static void beginParsing()
	{
		LsiFeatureLocation lsiFL = new LsiFeatureLocation();
		VsmFeatureLocation vsmFL = new VsmFeatureLocation();
		
		FileParser fileParser = new FileParser();
		fileParser.setLsiObject(lsiFL);
		fileParser.setVsmObject(vsmFL);
		
		ProjectParser projParser = new ProjectParser(fileParser);
		
		String projDir = "C:\\SAD\\semester4\\CodeAnalysisProject\\assignments\\testprojects\\SimpleIO\\src";
		
		projParser.parseProject(new File(projDir));		
		
		
		try {
			lsiFL.printSimilarDocuments();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) 
	{
		System.out.println("Launching the application..");
		
		//new UserScreen().showUI();
		
		beginParsing();
	
		
	}
}
