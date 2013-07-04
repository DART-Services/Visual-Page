package org.visualpage.features;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dharts.dia.model.PageItem;
import org.dharts.dia.model.PageModelNode;

public class FeatureExtractorSet 
{
	private final Map<String, NumericFeatureExtractor> extractors = new HashMap<>();
	private int size = 0;
	protected final Map<String, Mark> bookmarks = new HashMap<>();
	
	public void addFeature(NumericFeatureExtractor extractor)
	{
		synchronized (this) {
			extractors.put(extractor.getName(), extractor);
		}
	}
	
	public void addObservation(PageModelNode<? extends PageItem> node) {
		synchronized (this) {
			size++;
			for (NumericFeatureExtractor extractor : extractors.values())  {
				extractor.handle(node);
			}
		}
	}
	
	public void clear() {
		synchronized (this) {
			size = 0;
			bookmarks.clear();
			for (NumericFeatureExtractor extractor : extractors.values())  {
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
		synchronized (this) {
			printHeader(out);
			printValues(out);
		}
	}
	
	private void printHeader(PrintStream out) {
		if (!bookmarks.isEmpty())
			out.print("Bookmarks\t");
		
		for (NumericFeatureExtractor extractor : extractors.values())
		{
			out.print(extractor.getName() + "\t");
		}
		
		out.println();
	}
	
	private void printValues(PrintStream out) {
		MarkIterator marks = new MarkIterator(new TreeSet<Mark>(bookmarks.values()));
		for (int i = 0; i < size; i++)
		{
			out.print(marks.next() + "\t");
			for (NumericFeatureExtractor extractor : extractors.values())
			{
				out.print(extractor.get(i) + "\t");
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