package org.visualpage.features;

public abstract class NumericFeatureExtractor<T> implements FeatureExtractor<T>
{
	private final String featureName;
	protected final DoubleList values = new DoubleList();
	
	public NumericFeatureExtractor(String name) {
		featureName = name;
	}
	
	@Override
	public String getName()
	{
		return featureName;
	}
	
	@Override
	public final int size()
	{
		return values.size();
	}
	
	public final double get(int ix)
	{
		return values.get(ix);
	}
	
	@Override
	public String getAsString(int ix) {
		return Double.toString(values.get(ix));
	}
	
	@Override
	public void setContext(FeatureContext ctx) 
	{
		
	}
	
	@Override
	public void clear() {
		values.clear();
	}
	
	protected final void addValue(int value) 
	{
		values.add(value);
	}
	
	protected final void addValue(double value) 
	{
		values.add(value);
	}
	
	@Override
	public abstract void handle(T observation);
}