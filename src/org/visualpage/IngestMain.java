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

		ImageVisitor(ImageAnalyzer analyzer)
    	{
			this.analyzer = analyzer;
    	}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			Path features = dir.resolve("lineFeatures.txt");
			if (Files.exists(features))
				Files.delete(features);
//				return FileVisitResult.SKIP_SUBTREE;
			
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
				analyzer.process(matcher.group(1), image);
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
			Path path = dir.resolve("lineFeatures.txt");
			try (PrintStream out = new PrintStream(Files.newOutputStream(path)))
			{
				analyzer.getLineFeatures().print(out);
				out.flush();
			}
			
			analyzer.getLineFeatures().clear();
			return FileVisitResult.CONTINUE;
		}
    }
}
