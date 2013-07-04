package org.visualpage.features;

import org.dharts.dia.model.PageItem;
import org.dharts.dia.model.PageModelNode;

public abstract class NumericFeatureExtractor
{
	// TODO allow different features to handle different node types
	private final String featureName;
	protected final DoubleList values = new DoubleList();
	
	public NumericFeatureExtractor(String name) {
		featureName = name;
	}
	
	public String getName()
	{
		return featureName;
	}
	
	/** 
	 * Creates a new bookmark for the selected data. This can be used, for example
	 * to mark the beginning of a new page or poem. Sub-sets of the feature space
	 * can be computed between bookmarks. 
	 *  
	 * @param name The name of the mark to set. If a mark with this name has already been
	 * 		set it will be cleared and overwritten. 
	 */
//	public void mark(String name)
//	{
//		bookmarks.put(name, Integer.valueOf(values.size()));
//	}
	
	protected void addValue(int value) 
	{
		values.add(value);
	}
	
	protected void addValue(double value) 
	{
		values.add(value);
	}
	
	public int size()
	{
		return values.size();
	}
	
	public double get(int ix)
	{
		return values.get(ix);
	}
	
	public void clear() {
		values.clear();
	}
	
	public abstract void handle(PageModelNode<? extends PageItem> node);
}