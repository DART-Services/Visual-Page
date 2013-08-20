package org.visualpage;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dharts.utils.pdf.PdfParsingException;
import org.dharts.utils.pdf.PdfProcessor;
import org.dharts.utils.pdf.ghostscript.GhostscriptFacade;

/**
 * 
 * @author Neal Audenaert
 */
public class PdfImporter {
	private static final Logger logger = Logger.getLogger("org.visualpage.logger");
	
	private static final String INPUT_PROP = "org.visualpage.pdf.dirs.input";
	private static final String OUTPUT_PROP = "org.visualpage.pdf.dirs.output";
	private static final String FMT_PROP = "org.visualpage.pdf.fmt";
	private static final String PPI_PROP = "org.visualpage.pdf.ppi";

	
	private final Path output;
	private final int ppi;
	private final String fmt;
	
	public PdfImporter(Path output, int ppi, String fmt) {
		this.output = output;
		if (!Files.exists(output))
			throw new IllegalArgumentException("The supplied output directory [" + output + "] does not exist.");
		
		this.ppi = ppi;
		this.fmt = fmt;
	}
	
	public void process(Path pdf) throws IOException
    {
		if (!Files.exists(pdf) || !Files.isReadable(pdf) || !Files.isRegularFile(pdf))
			throw new IOException("The supplied path [" + pdf + "] does not reference a readable file");
        
        // extract images from the PDF document
        PdfProcessor gs = new GhostscriptFacade(GhostscriptFacade.DEFAULT_CMD);
        	
        String fname = pdf.getFileName().toString();
        fname = fname.substring(0, fname.lastIndexOf('.'));
        
        Path pdfOutput = output.resolve(fname);
        if (Files.exists(pdfOutput))
        	return;		// already processed
        
        Files.createDirectories(pdfOutput);
        
//        List<File> files;
        try {
            gs.toImageFiles(pdf.toFile(), pdfOutput.toFile(), fmt, ppi);
        } catch (PdfParsingException e) {
        	logger.log(Level.SEVERE, "Failed to extract page images for [" + pdf + "]", e);
        }
    }
	
	private static int getResolution() {
		int ppi = 400;
		try {
			String ppiProp = System.getProperty(PPI_PROP, "400");
			ppi = Integer.parseInt(ppiProp);
		}  catch (NumberFormatException nfe) {
			
		}
		return ppi;
	}
	
	private static Path getInputPath() {
		String input = System.getProperty(INPUT_PROP, null);
		if (input == null || input.trim().isEmpty())
			throw new IllegalStateException("No input directory specified.");
		
		return Paths.get(input);
	}
	
	private static Path getOutputPath() {
		String output = System.getProperty(OUTPUT_PROP, null);
		if (output == null || output.trim().isEmpty())
		{
			try {
				return Files.createTempDirectory("pdf_image_export");
			} catch (IOException e) {
				throw new IllegalStateException("No output directory supplied. Failed to create temporary directory", e);
			}
		}
		
		return Paths.get(output);
	}
	
	public static void main(String[] args) {
		Path input = getInputPath();
		Path output = getOutputPath();
		int ppi = getResolution();
		String fmt = System.getProperty(FMT_PROP, "png16m");
		
		PdfImporter importer = new PdfImporter(output, ppi, fmt);
		
		try {
			Files.walkFileTree(input, new FileVisitorImpl(importer));
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to process input directory", e);
		}
	}
	
	private static class FileVisitorImpl implements FileVisitor<Path>
	{
		private final PdfImporter importer;
		
		public FileVisitorImpl(PdfImporter importer) {
			this.importer = importer;
		}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			// no-op
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			String name = file.getFileName().toString();
			if (!name.endsWith(".pdf"))
				return FileVisitResult.CONTINUE;
				
			importer.process(file);
			
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			logger.log(Level.SEVERE, "Failed to access file.", exc);
			
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}
}
