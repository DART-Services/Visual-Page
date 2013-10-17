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
import java.util.Collection;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import org.dharts.dia.BoundingBox;
import org.dharts.dia.seg.ConnectedComponent;
import org.dharts.dia.seg.ConnectedComponents;
import org.dharts.dia.threshold.FastSauvola;
import org.dharts.dia.util.ImageWrapper;

public class TestExtractCC {
	private static final ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

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

	public TestExtractCC() {
		// TODO Auto-generated constructor stub
	}

	private static class CCLableWriter
	{
		int colorIx = -1;
		int[] bg = {255, 255, 255};


		int[] labelColors = new int[50_000];

		public CCLableWriter() {
			for (int i = 0; i < labelColors.length; i++)
			{
				labelColors[i] = -1;
			}
		}

		int[] getColor(int label)
		{
			if (label == 0)
				return bg;

			if (labelColors[label] <= 0)
			{
				colorIx++;
				if (colorIx >= colorMap.length)
					colorIx = 0;

				labelColors[label] = colorIx;
			}

			return colorMap[labelColors[label]];
		}

		private void set(WritableRaster raster, int c, int r, int[] color)
		{
			for (int b = 0; b < color.length; b++)
			{
				raster.setSample(c, r, b, color[b]);
			}

		}

		BufferedImage toImage(int[] ccLabels, BufferedImage model)
		{

			int offset = 0;
			int width = model.getWidth();
			int height = model.getHeight();

			ColorModel colorModel = model.getColorModel();
			WritableRaster raster = colorModel.createCompatibleWritableRaster(width, height);
			for (int r = 0; r < height; r++)
			{
				for (int c = 0; c < width; c++)
				{
					set(raster, c, r, getColor(ccLabels[offset + c]));
				}

				offset += width;
			}

			return new BufferedImage(colorModel, raster, true, new Hashtable<>());
		}
	}

	public static BufferedImage render(Collection<ConnectedComponent> components, BufferedImage model) {
		int width = model.getWidth();
		int height = model.getHeight();

		ColorModel colorModel = model.getColorModel();
		WritableRaster raster = colorModel.createCompatibleWritableRaster(width, height);
		for (int r = 0; r < model.getHeight(); r++)  {
			for (int c = 0; c < model.getWidth(); c++) {
				for (int b = 0; b < 3; b++) {
					raster.setSample(c, r, b, 255);
				}
			}
		}

		int cIx = 0;
		for (ConnectedComponent cc : components)
		{
			BoundingBox box = cc.getBounds();
			if (box.getWidth() > 150 || box.getHeight() > 60 || cc.getNumberOfPixels() < 25)
				continue;
			ConnectedComponent.write(cc, raster, colorMap[cIx]);
			cIx = cIx < colorMap.length - 1 ? cIx + 1 : 0;
		}

		return new BufferedImage(colorModel, raster, true, new Hashtable<>());
	}

	private static void printStats(int ct, long acc) {
		double avg = (double)acc / ct;

		System.out.println("\n--------------------------------------------");
		System.out.println("Total Processing Time: " + acc + " ms");
		System.out.println("Average Processing Time: " + (avg) + " ms");
		System.out.println("Images Processed: " + ct);
	}

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

	public static void main(String[] args) {
		Path base = Paths.get("H:\\dev\\projects\\VisualPage\\data\\output\\45");
		Path work = base.resolve("Digby Hours_with_the_First_Falling_Leaves");

		int ct = 0;

		long acc = 0;
		try {
			File[] files = work.toFile().listFiles();
			ConnectedComponents cc = new ConnectedComponents();
			for (File pg : files) {
				if (!pg.getName().endsWith(".png"))
					continue;

				ct++;
				BufferedImage image = ImageIO.read(pg);
				BufferedImage gs = op.filter(image, null);
				long start = System.currentTimeMillis();
				ImageWrapper wrapper = new ImageWrapper(gs);
				FastSauvola thresholder = new FastSauvola();
				thresholder.initialize(wrapper);

				int[] binaryIm = thresholder.call();
				int[] ccLabels = cc.labeling(binaryIm, wrapper.getWidth(), wrapper.getHeight());
				CCLableWriter writer = new CCLableWriter();
				BufferedImage coloredImage = writer.toImage(ccLabels, image);

				acc += System.currentTimeMillis() - start;
				writeResult(coloredImage, pg);
				writeResult(FastSauvola.toImage(binaryIm, image), pg);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

//		waitForCompletion(exec);
		printStats(ct, acc);
	}
}
