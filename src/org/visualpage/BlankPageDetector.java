package org.visualpage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dharts.dia.BoundingBox;
import org.dharts.dia.seg.ConnectedComponent;
import org.dharts.dia.seg.ConnectedComponents;
import org.dharts.dia.seg.lines.ProjectionProfiler;
import org.dharts.dia.threshold.FastSauvola;
import org.dharts.dia.util.ImageWrapper;
import org.dharts.dia.util.IntegralImage;

/**
 * A hackish attempt to detect blank pages and prevent them from tripping up Tesseract.
 */
public class BlankPageDetector {

	public static boolean hasText(BufferedImage image) throws InterruptedException, IOException
	{
		ImageWrapper wrapper = new ImageWrapper(image);
		ProjectionProfiler profiler = ProjectionProfiler.create(wrapper.getIntegralImage());
		wrapper.getRaster();
		List<Integer> lines = profiler.findLines();
		if (lines.size() < 4) {
			return false;
		}

		FastSauvola thresholder = new FastSauvola();
		thresholder.initialize(wrapper);

		int[] binaryIm = thresholder.call();
		ConnectedComponents cc = new ConnectedComponents();
		Collection<ConnectedComponent> components = cc.findCCs(binaryIm, wrapper.getWidth(), wrapper.getHeight());
		components = filter(components);
		Map<Integer, List<ConnectedComponent>> ccsByLine = matchLines(components, lines);
		components = extractCCs(ccsByLine);

		if (components.size() < 100)
			return false;

		IntegralImage iIm = wrapper.getIntegralImage();
		double[] gaus = iIm.getGausModel(0, 0, iIm.getWidth() - 1, iIm.getHeight() - 1);
		if (gaus[1] < 750)
			return false;
		if (gaus[0] < 100)
			return false;

		return true;
	}

	private static Collection<ConnectedComponent> filter(Collection<ConnectedComponent> components)
	{
		Collection<ConnectedComponent> result = new HashSet<>();
		for (ConnectedComponent cc : components)
		{
			BoundingBox box = cc.getBounds();
			if (box.getWidth() > 150 || box.getHeight() > 60 || cc.getNumberOfPixels() < 150)
				continue;

			result.add(cc);
		}

		return result;
	}

	/**
	 * Matches connected components to lines.
	 * @param components
	 * @param lines
	 */
	private static Map<Integer, List<ConnectedComponent>> matchLines(Collection<ConnectedComponent> components, List<Integer> lines)
	{
		Map<Integer, List<ConnectedComponent>> matchedCCs = new HashMap<>();
		for (Integer line : lines)
		{
			matchedCCs.put(line, new ArrayList<ConnectedComponent>());
		}

		for (ConnectedComponent cc : components)
		{
			BoundingBox box = cc.getBounds();
			for (int line : lines) {
				if (box.getTop() <= line && box.getBottom() >= line)
				{
					matchedCCs.get(Integer.valueOf(line)).add(cc);
				}
			}
		}

		Set<Integer> remove = new HashSet<>();
		for (Integer i : matchedCCs.keySet())
		{
			if (matchedCCs.get(i).size() < 1)
				remove.add(i);
		}

		for (Integer i : remove)
			matchedCCs.remove(i);

		return matchedCCs;
	}

	private static Set<ConnectedComponent> extractCCs(Map<Integer, List<ConnectedComponent>> matchedCCs)
	{
		Set<ConnectedComponent> result = new HashSet<>();
		for (Integer i : matchedCCs.keySet())
		{
			result.addAll(matchedCCs.get(i));
		}

		return result;
	}

	private BlankPageDetector() {
		// TODO Auto-generated constructor stub
	}

}
