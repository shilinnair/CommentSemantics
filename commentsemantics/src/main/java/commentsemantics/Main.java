package commentsemantics;

import java.io.File;
import java.io.IOException;

import featurelocation.LsiFeatureLocation;
import featurelocation.VsmDocSimilarity;
import featurelocation.VsmFeatureLocation;
import parser.FileParser;
import parser.ProjectParser;

public class Main {
	
	private static void beginParsing()
	{			
		VsmFeatureLocation vsmFL = new VsmFeatureLocation();
		LsiFeatureLocation lsiFL = new LsiFeatureLocation();
		VsmDocSimilarity vsmDocSimilarity = new VsmDocSimilarity();
		
		FileParser fileParser = new FileParser();
		fileParser.setVsmFL(vsmFL);
		fileParser.setLsiFL(lsiFL);
		fileParser.setUseJavadocComment(true);
		
		ProjectParser projParser = new ProjectParser(fileParser);
		
		String projDir = "C:\\SAD\\semester4\\CodeAnalysisProject\\assignments\\testprojects\\SimpleIO\\src";
		
		projParser.parseProject(new File(projDir));		
		
		
		try {
			lsiFL.buildSemanticVectors();
			lsiFL.LsiQuerySearch("block line java");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) 
	{
		
		System.out.println("Launching the application..");
		
		new UserScreen().showUI();
		
		//beginParsing();
	
		
	}
}
