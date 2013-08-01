package org.visualpage;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Packages the feature data files into a more convenient form.
 */
public class FeaturePackager {

public static void main(String[] args) {
		
//		Path work = base.resolve("Allingham Fifty_Modern_Poems");
//		Path pg = work.resolve("pg_00237.png");
		
			
    	Path base = Paths.get("H:\\dev\\projects\\VisualPage\\data\\output");
		try//  (ImageAnalyzer analyzer = new ImageAnalyzer())
		{
//			analyzer.initialize();
//			File[] files = base.toFile().listFiles();
//			for (File work : files)
//			{
//				if (!work.isDirectory())
//					continue;
				
				Files.walkFileTree(base, new Packager(base));
//			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static class Packager implements FileVisitor<Path>
	{
		private static final Path lineFeatures = Paths.get("lineFeatures.txt");
		private static final Path pageFeatures = Paths.get("pageFeatures.txt");
		private volatile String dirname;
		private final Path base;
		public Packager(Path base) {
			this.base = base;
		}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			dirname = dir.getFileName().toString();
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (file.endsWith(lineFeatures))
			{
				String fname = "lines_" + dirname + ".txt";
				Files.copy(file, base.resolve(fname));
			}
			if (file.endsWith(pageFeatures))
			{
				String fname = "pages_" + dirname + ".txt";
				Files.copy(file, base.resolve(fname));
			}
			
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}
}
