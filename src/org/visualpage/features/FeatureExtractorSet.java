package org.visualpage.features;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class FeatureExtractorSet<T>
{
	private final Map<String, FeatureExtractor<T>> extractors = new HashMap<>();
	protected final Map<String, Mark> bookmarks = new HashMap<>();
	private int size = 0;
	
	public void addFeature(FeatureExtractor<T> extractor)
	{
		synchronized (this) {
			extractors.put(extractor.getName(), extractor);
		}
	}
	
	public void setContext(FeatureContext ctx) {
		synchronized (this) {
			for (FeatureExtractor<T> extractor : extractors.values())  {
				extractor.setContext(ctx);
			}
		}
	}

	public void addObservation(T observation) {
		synchronized (this) {
			size++;
			for (FeatureExtractor<T> extractor : extractors.values())  {
				extractor.handle(observation);
			}
		}
	}
	
	public void clear() {
		synchronized (this) {
			size = 0;
			bookmarks.clear();
			for (FeatureExtractor<T> extractor : extractors.values())  {
				extractor.clear();
			}
		}
	}
	
	public void mark(String name) {
		synchronized (this) {
			Mark mark = new Mark(name, size);
			
			bookmarks.put(name, mark);
		}
	}
	
	public void print(PrintStream out) {
		print(out, "\t");
	}
	
	public void print(PrintStream out, String separator) {
		synchronized (this) {
			printHeader(out, separator);
			printValues(out, separator);
		}
	}
	
	private void printHeader(PrintStream out, String separator) {
		if (!bookmarks.isEmpty())
			out.print("Bookmarks\t");
		
		for (FeatureExtractor<T> extractor : extractors.values())
		{
			out.print(extractor.getName() + separator);
		}
		
		out.println();
	}
	
	private void printValues(PrintStream out, String separator) {
		MarkIterator marks = new MarkIterator(new TreeSet<Mark>(bookmarks.values()));
		for (int i = 0; i < size; i++)
		{
			out.print(marks.next() + separator);
			for (FeatureExtractor<T> extractor : extractors.values())
			{
				out.print(extractor.getAsString(i) + separator);
			}
			
			out.println();
		}
	}
	
	private static class MarkIterator implements Iterator<String>
	{
		private final Iterator<Mark> iterator;
		private int ix;
		private String name = "";
		private Mark next;

		private int pos = 0;
		
		public MarkIterator(SortedSet<Mark> marks) {
			iterator = marks.iterator();
			
			if (iterator.hasNext())
			{
				next = iterator.next();
				ix = next.getIndex();
			}
		}
		
		@Override
		public boolean hasNext() {
			return pos < Integer.MAX_VALUE;
		}

		@Override
		public String next()
		{
			if (next == null)
				return "";
			
			// increment if needed
			if (pos >= ix) {
				name = next.getName();
				if (iterator.hasNext()) {
					next = iterator.next();
					ix = next.getIndex();
				} else {
					next = null;
					ix = Integer.MAX_VALUE;
				}
			}
			
			pos++;
			return (name == null) ? "" : name;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}