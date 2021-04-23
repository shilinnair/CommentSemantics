package parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import featurelocation.FeatureLocation;

public class FileParser {
	
	//private final static String [] RESERVED_WORDS = {"about", "again", "all", "am", "an", "and", "any", "are", "array", "as", "at", "be", "because", "been", "before", "below", "between", "both", "but", "by", "cannot", "class", "could", "did", "do", "does", "down", "double", "each", "few", "for", "from", "further", "get", "had", "has", "have", "he", "her", "here", "hers", "herself", "him", "himself", "his", "how", "if", "in", "int", "integer", "into", "is", "it", "its", "itself", "list", "me", "more", "most", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own", "same", "set", "she", "should", "so", "some", "string", "such", "than", "that", "the", "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why", "with", "would", "you", "your", "yours", "yourself", "yourselves", "String"};
	
	private final String JAVAKEYWORDS = "./data/javakeywords.txt";
	private final String ENGLISHSTOPWORDS = "./data/stopwords.txt";
	
	private List<String> artefact = new ArrayList<String>(); // artifact tokens
	private List<String> comments = new ArrayList<String>(); // comment tokens
	
	private FeatureLocation vsmFL;
	private FeatureLocation lsiFL;
	
	private List<String> StopWords = new ArrayList<String>();
	
	public FileParser()
	{
		populateStopWords();		
	}
	
	private void populateStopWords()
	{		
		//adding java keywords
		try {
			Scanner scanner = new Scanner(new File(JAVAKEYWORDS));
			while (scanner.hasNextLine()) {
				   String word = scanner.nextLine();				   
				   this.StopWords.add(word.trim());
				}
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		}
		
		
		//adding English stop words
		try {
			Scanner scanner = new Scanner(new File(ENGLISHSTOPWORDS));
			while (scanner.hasNextLine()) {
				   String word = scanner.nextLine();
				   this.StopWords.add(word.trim());
				}
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		}		

	}
	
	
	public void setVsmFL(FeatureLocation vsmFL) {
		this.vsmFL = vsmFL;
	}
	
	public void setLsiFL(FeatureLocation lsiFL)
	{
		this.lsiFL = lsiFL;
	}

	private boolean UseAllComments = false;     // use all comments for feature location
	private boolean UseLineComments = false;     // use only line comments for feature location 
	private boolean UseBlockComment = false;     // use only block comments for feature location
	private boolean UseJavadocComment = false;   // use only java doc comments for feature location
	private boolean RemoveCodeComments = false;  // do not consider commented code for feature location
	private boolean IncludeArtefacts = false;    // use artefact with comments for feature location
	
	
	public void setUseAllComments(boolean useAllComments) {
		UseAllComments = useAllComments;
	}

	public void setIncludeArtefacts(boolean includeArtefacts) {
		IncludeArtefacts = includeArtefacts;
	}

	public void setUseLineComments(boolean useLineComments) {
		UseLineComments = useLineComments;
	}

	public void setUseBlockComment(boolean useBlockComment) {
		UseBlockComment = useBlockComment;
	}

	public void setUseJavadocComment(boolean useJavadocComment) {
		UseJavadocComment = useJavadocComment;
	}
	
	public void setRemoveCodeComments(boolean removeCodeComments) {
		this.RemoveCodeComments = removeCodeComments;		
	}
	
		
	public void reset()
	{
		artefact.clear();
		comments.clear();	
		vsmFL.reset();
		lsiFL.reset();
	}
	

	public void parseNode(String fileName, Node node, String parent) 
	{
		// find artifact tokens if enabled
		if(IncludeArtefacts) {
			node.accept(new ArtefactVisitor(artefact), null);
		}
		
		
		// find comment tokens
		if(UseAllComments) {   //full comment search mode
			
			for (Comment comment : node.getAllContainedComments()) 
			{
				String refinedComment = comment.getContent();
				
				//remove commented code
				if(RemoveCodeComments) {				
					refinedComment = CodeCommentParser.parseCodeComments(refinedComment);
					if(refinedComment.isEmpty())
						continue;
				}
				
				comments.addAll(tokenizeName(refinedComment, true));
			}			
		}
		
		
		if(UseLineComments) {  //only line comments search mode			
			
			for (LineComment comment : node.findAll(LineComment.class))
			{
				String refinedComment = comment.getContent();
				
				if(RemoveCodeComments) {  //remove commented code				
					refinedComment = CodeCommentParser.parseCodeComments(refinedComment);
					if(refinedComment.isEmpty())
						continue;
				}
				
				comments.addAll(tokenizeName(refinedComment, true));
			}
		}
		
		if(UseBlockComment) {  //only block comments search mode			
					
			for (BlockComment comment : node.findAll(BlockComment.class))
			{
				String refinedComment = comment.getContent();
				
				if(RemoveCodeComments) {  //remove commented code				
					refinedComment = CodeCommentParser.parseCodeComments(refinedComment);
					if(refinedComment.isEmpty())
						continue;
				}
				
				comments.addAll(tokenizeName(refinedComment, true));
			}
		}
		
		if(UseJavadocComment) {  //only java doc comments search mode			
			
			for (JavadocComment comment : node.findAll(JavadocComment.class))
			{
				String refinedComment = comment.getContent();
				
				if(RemoveCodeComments) {  //remove commented code				
					refinedComment = CodeCommentParser.parseCodeComments(refinedComment);
					if(refinedComment.isEmpty())
						continue;
				}
				
				comments.addAll(tokenizeName(refinedComment, true));
			}
		}
				
		//if artefact also needs to be considered add it to the final data 
		if(IncludeArtefacts) {
			comments.addAll(artefact);
		}
		
		// prepare document for each FL techniques
		vsmFL.prepareDocument(fileName, comments);
		lsiFL.prepareDocument(fileName, comments);			
		
		artefact.clear();
		comments.clear();
	}
	
	//feature location visitor
	private class ArtefactVisitor extends VoidVisitorAdapter<Void>
	{
		private List<String> artefact = null;
		
		public ArtefactVisitor(List<String> artefact ) {
			this.artefact = artefact;
		}
		
		@Override
		public void visit(ClassOrInterfaceDeclaration av, Void arg)
		{
			super.visit(av, arg);			
			artefact.addAll(tokenizeName(av.getNameAsString(), false));
		}
				
		@Override
		public void visit(FieldDeclaration av, Void arg)  
		{	
			super.visit(av, arg);
			
			av.getVariables().forEach(var-> {
				artefact.addAll(tokenizeName(var.getNameAsString(), false));
			});
		}
		
		@Override
		public void visit(MethodDeclaration av, Void arg)
		{
			super.visit(av, arg);
			
			artefact.addAll(tokenizeName(av.getNameAsString(), false));
			
			//input arguments
			av.getParameters().forEach(param -> {
				artefact.addAll(tokenizeName(param.getNameAsString(), false));
			});
		}
		
		@Override
		public void visit(ConstructorDeclaration av, Void arg)
		{
			super.visit(av, arg);			
			artefact.addAll(tokenizeName(av.getNameAsString(), false));
		}
		
		@Override
		public void visit(ClassOrInterfaceType av, Void arg)
		{
			super.visit(av, arg);			
			artefact.addAll(tokenizeName(av.getNameAsString(), false));
		}		
	}
	
		
	private List<String> tokenizeName(String name, boolean removePunct)
	{
		//camel case split
		List<String> tokenList = camelCaseSplit(name);
		
		//snake case split
		List<String> tempTokens =  new ArrayList<String>();
		for (String token : tokenList) {
			tempTokens.addAll(snakeCaseSplit(token));
		}
		tokenList.clear();
		tokenList.addAll(tempTokens);			
		
		//digits removal
		for (int i=0; i<tokenList.size(); ++i) {
			tokenList.set(i, removeDigits(tokenList.get(i)));
		}
		
		//lower case and Stemming
		tempTokens.clear();
		for (String token : tokenList) {
			tempTokens.addAll(tokenizer(token));
		}
		tokenList.clear();
		tokenList.addAll(tempTokens);
		
		//remove stop words && words with less than 2 character 
		for (int i=0; i<tokenList.size(); ++i) {
			
			if(isRservedWord(tokenList.get(i)) || tokenList.get(i).length() < 2) {
				tokenList.remove(i);
				--i;
			}	
		}
		
		//remove punctuation for comments
		if(removePunct) {		
			tempTokens.clear();
			
			for (String token : tokenList) {
				tempTokens.addAll(removePunctuation(token));
			}
			tokenList.clear();
			tokenList.addAll(tempTokens);
		}
				
		return tokenList;
	}
	
	
	private List<String> tokenizer(String string)
	{
		List<String> result = new ArrayList<String>();		

		try {
			
			Analyzer analyzer = CustomAnalyzer.builder()
					.withTokenizer("standard")
					.addTokenFilter("lowercase")
					.addTokenFilter("stop")
					.addTokenFilter("porterstem")
					.build();

			TokenStream stream = analyzer.tokenStream(null, new StringReader(string));

			stream.reset();

			while (stream.incrementToken()) {
				result.add(stream.getAttribute(CharTermAttribute.class).toString());
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return result;
	}
	
	private List<String> camelCaseSplit(String string)
	{		
		String [] res = string.split("(?=[A-Z])");
		
		List<String> splitStrings = new ArrayList<String>();
		
		for(int i=0; i<res.length; ++i) {
			splitStrings.add(res[i]);
		}
		
		return splitStrings;
	}
	
	private List<String> snakeCaseSplit(String string)
	{		
		String [] res = string.split("_");
		
		List<String> splitStrings = new ArrayList<String>();
		
		for(int i=0; i<res.length; ++i) {
			splitStrings.add(res[i]);
		}
		
		return splitStrings;
	}
	
	private String removeDigits(String string)
	{		
		return string.replaceAll("[0-9]", "");
	}
	
	private boolean isRservedWord(String string)
	{
		for (String reserved : StopWords) {
			if(string.equals(reserved))
				return true;
				
		}
		return false;		
	}
	
	private List<String> removePunctuation(String string)
	{		
		Pattern pattern = Pattern.compile("\\p{Punct}+|\\d+|\\s+");
		String [] res = pattern.split(string);		
		
		List<String> splitStrings = new ArrayList<String>();
		
		for(int i=0; i<res.length; ++i) {
			splitStrings.add(res[i]);
		}
		
		return splitStrings;
	}
		
}
