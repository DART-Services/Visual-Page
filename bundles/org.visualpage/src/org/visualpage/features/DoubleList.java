package org.visualpage.features;

public class DoubleList {
	private double[] values;
	private int size;
	
	private double mean = 0;
	private double var = 0;
	private double min = Double.POSITIVE_INFINITY;		// Need negative value for min, so using integer
	private double max = Double.NEGATIVE_INFINITY;

	// used in online calculations of mean and variance. 
	// See http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm
	private double M2 = 0;
	
	public DoubleList() 
	{
		this(1000);
	}
	
	public DoubleList(int sz) {
		values = new double[sz];
	}
	
	public int add(int value) 
	{
		return add((double)value);
	}
	
	public int add(double value) 
	{
		int ix = size;
		values[ix] = value;
		increment();

		if (value == Double.NaN)
			return ix;
		
		// compute statistics
		if (value < min)
			min = value;
		
		if (value > max)
			max = value;
		
		// update mean and var
		double delta = value - mean;
		mean = mean + delta / size;
		M2 = M2 + delta * (value - mean);
		var = M2 / size;

		return ix;
	}
	
	public double get(int ix) 
	{
		if (ix < 0 || ix >= size)
			throw new IndexOutOfBoundsException("The supplied index (" + ix + ") " + 
					"is out of bounds. Range: [0, " + size + ")");
		
		return values[ix];
	}
	
	public double getMin() {
		return min;
	}
	
	public double getMax() {
		return max;
	}
	
	public double getMean() {
		return mean;
	}
	
	public double getVariance() {
		return var;
	}
	
	public double getSigma() {
		return Math.sqrt(var);
	}
	
	public void clear()
	{
		size = 0;
	}
	
	public int size()
	{
		return size;
	}
	
	private void increment()
	{
		size += 1;
		if (size >= values.length) 
		{
    		double[] newValues = new double[values.length * 2];
    		for (int i = 0; i < values.length; i++)
    		{
    			newValues[i] = values[i];
    		}
    		
    		values = newValues;
		}
	}
}