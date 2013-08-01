package org.visualpage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class IngestMain {

	public static void main(String[] args) {
		
//		Path work = base.resolve("Allingham Fifty_Modern_Poems");
//		Path pg = work.resolve("pg_00237.png");
		
			
    	Path base = Paths.get("H:\\dev\\projects\\VisualPage\\data\\output");
		try (ImageAnalyzer analyzer = new ImageAnalyzer())
		{
			analyzer.initialize();
			File[] files = base.toFile().listFiles();
			for (File work : files)
			{
				if (!work.isDirectory())
					continue;
				
				Files.walkFileTree(work.toPath(), new ImageVisitor(analyzer));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    private static class ImageVisitor implements FileVisitor<Path>
    {
    	private static Pattern p = Pattern.compile("pg_0*(\\d+).png$");
    	private ImageAnalyzer analyzer;
		private long start;
		private long ct = 0;
		
		ImageVisitor(ImageAnalyzer analyzer)
    	{
			this.analyzer = analyzer;
    	}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			Path features = dir.resolve("lineFeatures.txt");
			if (Files.exists(features))
				return FileVisitResult.SKIP_SUBTREE;
//				Files.delete(features);
			
			start = System.currentTimeMillis();
			System.out.println("Processing Work: " + dir.getFileName());
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			
			String filename = file.getFileName().toString();
			Matcher matcher = p.matcher(filename);
			if (!matcher.find())
				return FileVisitResult.CONTINUE;
			
			BufferedImage image = ImageIO.read(file.toFile());
			try {
				boolean handled = analyzer.process(matcher.group(1), image);
				if (handled)
					ct++;
			} catch (ImageProcessorException e) {
				throw new IOException("Failed to process file: " + file + ". ", e);
			}
			
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.TERMINATE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			// write output
			Path linePath = dir.resolve("lineFeatures.txt");
			Path pagesPath = dir.resolve("pageFeatures.txt");
			try (PrintStream out = new PrintStream(Files.newOutputStream(linePath)))
			{
				analyzer.getLineFeatures().print(out);
				out.flush();
			}
			try (PrintStream out = new PrintStream(Files.newOutputStream(pagesPath)))
			{
				analyzer.getPageFeatures().print(out);
				out.flush();
			}
			
			long elapsed = System.currentTimeMillis() - start;
			System.out.printf("\tElapsed Time: " + (double)elapsed / (1000 * 60));
			System.out.println("\tTime per Page: " + (double)elapsed / ct);
			
			// HACK this is not extensible
			analyzer.getLineFeatures().clear();
			analyzer.getLineFeatures().setContext(null);
			
			analyzer.getPageFeatures().clear();
			analyzer.getPageFeatures().setContext(null);
			
			return FileVisitResult.CONTINUE;
		}
    }
}
