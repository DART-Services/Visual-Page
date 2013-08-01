package org.visualpage;

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

public class TestSauvola {

	@SuppressWarnings("unused")
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

	private static void printStats(int ct, long start, long end) {
		double avg = (double)(end - start) / ct;
		
		System.out.println("\n--------------------------------------------");
		System.out.println("Total Processing Time: " + (end - start) + " ms");
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
		Path base = Paths.get("H:\\dev\\projects\\VisualPage\\data\\output");
		Path work = base.resolve("Digby Hours_with_the_First_Falling_Leaves");

		ExecutorService exec = Executors.newFixedThreadPool(16);
		int ct = 0;
    	
    	long start = System.currentTimeMillis();
		try {
			File[] files = work.toFile().listFiles();
			for (File pg : files) {
				if (!pg.getName().endsWith(".png"))
					continue;
				
				ct++;
				FastSauvola thresholder = new FastSauvola();
				thresholder.initialize(pg);
				thresholder.setGenerateImage(false);
				exec.submit(thresholder);
				
				if (ct >= 50)
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		waitForCompletion(exec);
		printStats(ct, start, System.currentTimeMillis());
	}
}
