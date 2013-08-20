package org.visualpage.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

import org.dharts.dia.seg.lines.ProjectionProfiler;
import org.dharts.dia.util.ImageWrapper;

public class TestProjectionProfile {



	public static void main(String[] args) {
		Path base = Paths.get("H:\\dev\\projects\\VisualPage\\data\\output\\45");
		Path work = base.resolve("Digby Hours_with_the_First_Falling_Leaves");

		int i = 0;
		for (String fname : work.toFile().list())
		{
			if (!fname.endsWith("png"))
				continue;

			i++;
			if (i < 370)
				continue;

			Path pg = work.resolve(fname);
//			IntegralImage iImage = new IntegralImage();
			try {
				File file = pg.toFile();
				BufferedImage image = ImageIO.read(file);
				image.getData();
				ImageWrapper wrapper = new ImageWrapper(image);
				ProjectionProfiler profiler = ProjectionProfiler.create(wrapper.getIntegralImage());
				wrapper.getRaster();
				List<Integer> lines = profiler.findLines();
				if (lines.size() < 4)
					System.out.println("skipping image: " + pg);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

//		long end = System.currentTimeMillis();
//		System.out.println();
//		System.out.println(end - start);
	}

//
//	private static WritableRaster initOutputImage(BufferedImage image, int leftPadding) {
//		int h = image.getHeight();
//		int w = image.getWidth();
//		int bands = image.getData().getNumBands();
//
//		WritableRaster output = image.getData().createCompatibleWritableRaster(leftPadding + w, h);
//
//		output.setRect(leftPadding, 0, image.getData());
//		for (int x = 0; x < leftPadding; x++)
//		{
//			for (int y = 0; y < h; y++)
//			{
//				for (int b = 0; b < bands; b++)
//				{
//					output.setSample(x, y, b, 255);
//				}
//			}
//		}
//
//		return output;
//	}
}
