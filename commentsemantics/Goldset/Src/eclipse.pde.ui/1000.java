package $packageName$;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;

public class XMLTagScanner extends RuleBasedScanner {

    public  XMLTagScanner(ColorManager manager) {
        IToken string = new Token(new TextAttribute(manager.getColor(IXMLColorConstants.STRING)));
        IRule[] rules = new IRule[3];
        // Add rule for double quotes
        rules[0] = new SingleLineRule("\"", "\"", string, '\\');
        rules[1] = new SingleLineRule("'", "'", string, '\\');
        rules[2] = new WhitespaceRule(new XMLWhitespaceDetector());
        setRules(rules);
    }
}
