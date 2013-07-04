package org.visualpage.features;

public final class Mark implements Comparable<Mark>
{
	private final String name;
	private final int ix;
	
	public Mark(String name, int ix) {
		this.name = name;
		this.ix = ix;
	}
	
	public String getName() {
		return name;
	}
	
	public int getIndex() {
		return ix;
	}
	
	@Override
	public int compareTo(Mark other) {
		int result = Integer.compare(ix, other.ix);
		if (result == 0)
			result = name.compareTo(other.name);
			
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Mark)
		{
			Mark other = (Mark)obj;
			return name.equals(other.name) && ix == other.ix;
		}
		
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + ix;
		result = 37 * result + name.hashCode();
		
		return result;
	}
}