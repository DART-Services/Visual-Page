package org.visualpage.features;

class DoubleList {
	private double[] values;
	private int size;

	public DoubleList() 
	{
		this(1000);
	}
	
	public DoubleList(int sz) {
		values = new double[sz];
	}
	
	public int add(int value) 
	{
		int ix = size;
		values[ix] = value;
		increment();
		return ix;
		
	}
	
	public int add(double value) 
	{
		int ix = size;
		values[ix] = value;
		increment();
		return ix;
	}
	
	public double get(int ix) 
	{
		if (ix < 0 || ix >= size)
			throw new IndexOutOfBoundsException("The supplied index (" + ix + ") " + 
					"is out of bounds. Range: [0, " + size + ")");
		
		return values[ix];
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