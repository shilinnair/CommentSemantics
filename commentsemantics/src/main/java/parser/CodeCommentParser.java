package parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeCommentParser 
{	
	private final double THRESHOLD = 50;
	
	private static final Pattern SEMI_COLON = Pattern.compile(";");
	private static final Pattern ROUTINE_CALL = Pattern.compile("(\\w+\\s*\\.)*\\s*\\w+\\s*\\(([^\\)]*)\\)(\\s*|\\s*;)");
	private static final Pattern SCOPE = Pattern.compile("(\\{|\\})");
	private static final Pattern OPERANDS = Pattern.compile("(\\+|\\-|&&|\\||<|>|=|:|_)");
	private static final Pattern KEYWORDS = Pattern.compile("(abstract|assert|boolean|break|byte|case|catch" +
            "char|class|const|continue|default|do|double|else|enum|extends|final|finally|flaot|for|goto|if" +
            "|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return" +
            "|short|static|trictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while)");
    
    List<Pattern> patternList = new ArrayList<>();
    {
        patternList.add(SEMI_COLON);
        patternList.add(KEYWORDS);
        patternList.add(OPERANDS);
        patternList.add(ROUTINE_CALL);
        patternList.add(SCOPE);
    }
    
    // Parse the comments and remove commented codes in it.
 	// Returns comments with out code, if commented code is less than 50% of total comments
 	// If commented code is more than 50% of total comments then returns empty string
 	public String parseCodeComments(String comment)
 	{
 		String result = "";
 		
 		StringBuilder evaluationString = new StringBuilder();
    	
        comment = comment.replaceAll("\\s+", " ");
        double originalCharacterCount = comment.length();
        
        for(Pattern pattern: patternList){
        	
            List<String> matches = getMatches(pattern, comment);
            
            for(String match: matches){
                evaluationString.append(match);
            }
            
            comment = comment.replaceAll(pattern.pattern(), "");
        }
        
        double percentage = ((double) evaluationString.length() / originalCharacterCount ) * 100L;

        if(percentage <= THRESHOLD)
            result = comment;
        else
        	System.out.println("Percentage code exceeds the comment: "+ percentage + "%\n"
                    + "originalcount: " +originalCharacterCount + "\n"
                    + "Remaining: " + comment.length() + "\n"
                    + "comment:" + comment);
 		
 		return result;
 	}
 	
 	private List<String> getMatches(Pattern pattern, String input)
 	{
        List<String> allMatches = new ArrayList<String>();
        Matcher matcher = pattern.matcher(input);
        
        while (matcher.find()) { 
        	allMatches.add(matcher.group()); 
        }
        
        return allMatches;
    }
    
}

