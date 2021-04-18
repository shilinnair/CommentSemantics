package featurelocation;

import java.util.List;

public interface FeatureLocation 
{	
	public void prepareDocument(String fileName, List<String> data);
	
	public void reset();
}
