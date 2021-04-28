package parser;

import java.io.File;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class ProjectParser 
{
	private FileParser fileParser;
	
	public ProjectParser(FileParser fileParser)
	{
		this.fileParser = fileParser;
	}
	
	public void parseProject(File projDir) 
	{
		
		for(final File file : projDir.listFiles()) {
			
			if(file.isDirectory())
				parseProject(file);  //no need to parse directory, enumerate files instead
			
			if(file.getName().endsWith(".java")) {
				
				System.out.println("Parsing file " + file.getName() + ":");
				
				try {
					CompilationUnit cu = StaticJavaParser.parse(file);
					fileParser.parseNode(file.getName(), cu, "cu");
				}
				catch(Exception e) {
					System.out.println("Filr parser exception!");
				}	
			}			
		}
	}
}
