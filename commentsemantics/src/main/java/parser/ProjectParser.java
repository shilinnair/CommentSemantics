package parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.NoSuchElementException;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.UnsolvedSymbolException;

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
				catch(NoSuchElementException e1) {
					System.out.println("Exception caught in parseProject. NoSuchElementException! " + e1.getMessage());
					System.out.println(e1.getStackTrace()[1].toString() + System.lineSeparator() + e1.getStackTrace()[2].toString());
				} catch (FileNotFoundException e2) {
					System.out.println("Exception caught in parseProject. FileNotFoundException! " + e2.getMessage());
					System.out.println(e2.getStackTrace()[1].toString() + System.lineSeparator() + e2.getStackTrace()[2].toString());
				}			
				catch (java.lang.UnsupportedOperationException e3) {
					System.out.println("Exception caught in parseProject. UnsupportedOperationException! " + e3.getMessage());
					System.out.println(e3.getStackTrace()[1].toString() + System.lineSeparator() + e3.getStackTrace()[2].toString());
				}
				catch (UnsolvedSymbolException e4) {
					System.out.println("Exception caught in parseProject. UnsolvedSymbolException! " + e4.getMessage());
					System.out.println(e4.getStackTrace()[1].toString() + System.lineSeparator() + e4.getStackTrace()[2].toString());
				}
			}			
		}
	}
}
