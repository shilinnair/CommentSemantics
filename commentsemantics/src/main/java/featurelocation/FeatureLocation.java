package featurelocation;

import java.util.List;

public interface FeatureLocation 
{	
	public void prepareDocument(String fileName, List<String> artefact, List<String> comments);
	
	public void reset();
}
