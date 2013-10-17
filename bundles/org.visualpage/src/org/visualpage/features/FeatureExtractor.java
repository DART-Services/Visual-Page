package org.visualpage.features;

public interface FeatureExtractor<T> {

	String getName();

	int size();
	
	String getAsString(int ix);

	/**
	 * When processing an image or collection of images it is often necessary to have 
	 * access to contextual information that informs the interpretation of specific 
	 * observations. For example, a feature that computes the length of text lines
	 * as a ratio of page width will need 
	 * 
	 */
	void setContext(FeatureContext ctx);

	void clear();

	void handle(T observation);
}