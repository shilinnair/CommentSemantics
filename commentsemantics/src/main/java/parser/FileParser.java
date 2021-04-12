package parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
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
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import featurelocation.FeatureLocation;

public class FileParser {
	
	private final static String [] RESERVED_WORDS = {"about", "again", "all", "am", "an", "and", "any", "are", "array", "as", "at", "be", "because", "been", "before", "below", "between", "both", "but", "by", "cannot", "class", "could", "did", "do", "does", "down", "double", "each", "few", "for", "from", "further", "get", "had", "has", "have", "he", "her", "here", "hers", "herself", "him", "himself", "his", "how", "if", "in", "int", "integer", "into", "is", "it", "its", "itself", "list", "me", "more", "most", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own", "same", "set", "she", "should", "so", "some", "string", "such", "than", "that", "the", "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why", "with", "would", "you", "your", "yours", "yourself", "yourselves"};
	
	private List<String> artefact = new ArrayList<String>(); // artifact tokens
	private List<String> comments = new ArrayList<String>(); // comment tokens
	
	private FeatureLocation vsmFL;
	private FeatureLocation lsiFL;
		
	public void reset()
	{
		artefact.clear();
		comments.clear();	
	}
	
	public void setVsmObject(FeatureLocation vsmFL)
	{
		this.vsmFL = vsmFL;
	}
	
	public void setLsiObject(FeatureLocation lsiFL)
	{
		this.lsiFL = lsiFL;
	}

	public void parseNode(String fileName, Node node, String parent) 
	{
		// find artifact tokens
		node.accept(new ArtefactVisitor(artefact), null);

		// find comment tokens
		for (Comment comment : node.getAllContainedComments()) {
			comments.addAll(tokenizeName(comment.getContent(), true));
		}

		// prepare document for each FL techniques
		vsmFL.prepareDocument(fileName, artefact, comments);
		lsiFL.prepareDocument(fileName, artefact, comments);		
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
			
			av.getModifiers().forEach(mod-> {
				artefact.addAll(tokenizeName(mod.toString(), false));
			});
			
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
		
	
	private static List<String> tokenizeName(String name, boolean removePunct)
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
	
	
	private static List<String> tokenizer(String string)
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
	
	private static List<String> camelCaseSplit(String string)
	{		
		String [] res = string.split("(?=[A-Z])");
		
		List<String> splitStrings = new ArrayList<String>();
		
		for(int i=0; i<res.length; ++i) {
			splitStrings.add(res[i]);
		}
		
		return splitStrings;
	}
	
	private static List<String> snakeCaseSplit(String string)
	{		
		String [] res = string.split("_");
		
		List<String> splitStrings = new ArrayList<String>();
		
		for(int i=0; i<res.length; ++i) {
			splitStrings.add(res[i]);
		}
		
		return splitStrings;
	}
	
	private static String removeDigits(String string)
	{		
		return string.replaceAll("[0-9]", "");
	}
	
	private static boolean isRservedWord(String string)
	{
		for (String reserved : RESERVED_WORDS) {
			if(string.equals(reserved))
				return true;
				
		}
		return false;		
	}
	
	private static List<String> removePunctuation(String string)
	{		
		Pattern pattern = Pattern.compile("\\p{Punct}");
		String [] res = pattern.split(string);		
		
		List<String> splitStrings = new ArrayList<String>();
		
		for(int i=0; i<res.length; ++i) {
			splitStrings.add(res[i]);
		}
		
		return splitStrings;
	}
		
}
