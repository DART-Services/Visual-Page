package org.visualpage.test;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.dharts.dia.threshold.FastSauvola;
import org.dharts.dia.util.ImageWrapper;

public class TestSauvola {

	private static final ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

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

	private static void printStats(int ct, long acc) {
		double avg = (double)acc / ct;

		System.out.println("\n--------------------------------------------");
		System.out.println("Total Processing Time: " + acc + " ms");
		System.out.println("Average Processing Time: " + (avg) + " ms");
		System.out.println("Images Processed: " + ct);
	}

	private static void waitForCompletion(ExecutorService exec) {
		exec.shutdown();
		try {
			exec.awaitTermination(2, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Path base = Paths.get("H:\\dev\\projects\\VisualPage\\data\\output\\45");
		Path work = base.resolve("Digby Hours_with_the_First_Falling_Leaves");

		ExecutorService exec = Executors.newFixedThreadPool(16);
		int ct = 0;

		long acc = 0;
		try {
			File[] files = work.toFile().listFiles();
			for (File pg : files) {
				if (!pg.getName().endsWith(".png"))
					continue;

				ct++;
				BufferedImage image = ImageIO.read(pg);
				image = op.filter(image, null);
				long start = System.currentTimeMillis();
				ImageWrapper wrapper = new ImageWrapper(image);
				FastSauvola thresholder = new FastSauvola();
				thresholder.initialize(wrapper);

				int[] binaryIm = thresholder.call();
				acc += System.currentTimeMillis() - start;
				writeResult(FastSauvola.toImage(binaryIm, image), pg);
//				Future<int[]> future = exec.submit(thresholder);

				if (ct >= 50)
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		waitForCompletion(exec);
		printStats(ct, acc);
	}
}
