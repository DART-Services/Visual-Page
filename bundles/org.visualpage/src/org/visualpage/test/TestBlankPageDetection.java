package org.visualpage.test;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.dharts.dia.BoundingBox;
import org.dharts.dia.seg.ConnectedComponent;
import org.dharts.dia.seg.ConnectedComponents;
import org.dharts.dia.seg.lines.ProjectionProfiler;
import org.dharts.dia.threshold.FastSauvola;
import org.dharts.dia.util.ImageWrapper;
import org.dharts.dia.util.IntegralImage;
import org.visualpage.BlankPageDetector;

public class TestBlankPageDetection {
	private static final ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

	public TestBlankPageDetection() {
		// TODO Auto-generated constructor stub
	}

	public static Collection<ConnectedComponent> filter(Collection<ConnectedComponent> components)
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

	private static int[][] colorMap = new int[][]
			{
				{127, 201, 127},
				{190, 174, 212},
				{253, 192, 134},
				{255, 255, 153},
				{56, 108, 176},
				{240, 2, 127},
				{191, 91, 23},
				{102, 102, 102}
			};

	private static void writeResult(RenderedImage result, File source)  throws IOException {
		String subdir = source.getName();
		subdir = subdir.substring(0, subdir.indexOf("."));
		File dir = new File(source.getParentFile(), "threshold");
		if (!dir.exists()) dir.mkdir();

		File outfile = new File(dir, source.getName());
		if (outfile.exists()) {
			outfile.delete();
		}

		ImageIO.write(result, "png", outfile);
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

	private static BufferedImage render(Collection<ConnectedComponent> components, List<Integer> lines, BufferedImage model) {
		int width = model.getWidth();
		int height = model.getHeight();

		ColorModel colorModel = ColorModel.getRGBdefault();
		WritableRaster raster = colorModel.createCompatibleWritableRaster(width, height);
		int numBands = raster.getNumBands();
		for (int r = 0; r < model.getHeight(); r++)  {
			for (int c = 0; c < model.getWidth(); c++) {
				for (int b = 0; b < numBands; b++) {
					raster.setSample(c, r, b, 255);
				}
			}
		}

		int cIx = 0;
		for (ConnectedComponent cc : components)
		{
			BoundingBox box = cc.getBounds();
			if (box.getWidth() > 150 || box.getHeight() > 60 || cc.getNumberOfPixels() < 150)
				continue;
			ConnectedComponent.write(cc, raster, colorMap[cIx]);
			cIx = cIx < colorMap.length - 1 ? cIx + 1 : 0;
		}

		// MARK LINES
		for (Integer ln : lines)
		{
			int r = ln.intValue();
			for (int c = 0; c < width; c++)
			{
				raster.setSample(c, r, 0, 0);
				raster.setSample(c, r, 1, 0);
				raster.setSample(c, r, 2, 0);
			}
		}

		return new BufferedImage(colorModel, raster, true, new Hashtable<>());
	}

	public static boolean hasText(BufferedImage image, File f) throws InterruptedException, IOException
	{
		System.out.println("Evaluating: " + f.getName());
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
		System.out.println("\tLines:    " + ccsByLine.keySet().size());
		components = extractCCs(ccsByLine);


		BufferedImage coloredImage = render(components, lines, image);
		writeResult(coloredImage, f);
		if (components.size() < 100)
		{
			return false;
		}

		IntegralImage iIm = wrapper.getIntegralImage();
		double[] gaus = iIm.getGausModel(0, 0, iIm.getWidth() - 1, iIm.getHeight() - 1);
		if (gaus[1] < 250)
		{
//			System.out.println("too uniform");
			return false;
		}
		System.out.println("\tVariance: " + gaus[1]);

//		BufferedImage coloredImage = render(components, lines, image);
//		writeResult(coloredImage, f);

		return true;
	}

	public static void main(String[] args) {
		Path base = Paths.get("H:\\dev\\projects\\VisualPage\\data\\output\\45");
		Path work = base.resolve("01_samples");

		int ct = 0;
		int err = 0;
		long acc = 0;
		try {
			File[] files = work.toFile().listFiles();
			ConnectedComponents cc = new ConnectedComponents();
			for (File pg : files) {
//				if (!pg.getName().endsWith("137.png"))
//					continue;

				if (pg.isDirectory())
					continue;

				ct++;
				BufferedImage image = ImageIO.read(pg);
				BufferedImage gs = op.filter(image, null);
				long start = System.currentTimeMillis();

				if (BlankPageDetector.hasText(gs)) {
					err++;
					System.out.println("Has Text: " + pg.getName());
				}
				else
				{
					System.out.println("No Text:  " + pg.getName());
				}

				acc += System.currentTimeMillis() - start;
				if (ct > 50)
					break;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

//		waitForCompletion(exec);
		printStats(ct, acc, err);
	}

	private static void printStats(int ct, long acc, int err) {
		double avg = (double)acc / ct;

		System.out.println("\n--------------------------------------------");
		System.out.println("Total Processing Time:   " + acc + " ms");
		System.out.println("Average Processing Time: " + (avg) + " ms");
		System.out.println("Errors:                  " + err);
		System.out.println("Images Processed:        " + ct);
	}

}
