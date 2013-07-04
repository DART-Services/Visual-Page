package org.visualpage;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
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

import org.dharts.dia.BoundingBox;
import org.dharts.dia.model.PageItem;
import org.dharts.dia.model.PageModel;
import org.dharts.dia.model.PageModelException;
import org.dharts.dia.model.PageModelNode;
import org.dharts.dia.tesseract.ImageAnalyzerFactory;
import org.dharts.dia.tesseract.TesseractException;
import org.dharts.dia.tesseract.model.TesseractLevelCatalog;
import org.dharts.dia.tesseract.model.TesseractPageAnalyzer;
import org.visualpage.features.FeatureExtractorSet;
import org.visualpage.features.NumericFeatureExtractor;

public class ImageAnalyzer implements AutoCloseable {
	
    private ImageAnalyzerFactory factory;
    private FeatureExtractorSet lineFeatures = new FeatureExtractorSet();
    
    public void initialize() 
    {
        // TODO assess thread safety
        if (factory != null) 
            return;
        
        // HACK: need to init from config file or other appropriate place
        //       need to figure out how to load DLL from convenient location
        System.setProperty(ImageAnalyzerFactory.DATAPATH, "./tessdata");
        System.setProperty(ImageAnalyzerFactory.LANGUAGE, "eng");
        
        try {
            factory = ImageAnalyzerFactory.createFactory();
        } catch (IOException | TesseractException e) {
            throw new IllegalStateException("Could not initialize ImageAnalyzerFactory", e);
        }
        
        initFeatures();
    }

	private static boolean isWord(PageModelNode<? extends PageItem> node)
	{
		return TesseractLevelCatalog.WORD.equals(node.getLevel().getName());
	}

	public FeatureExtractorSet getLineFeatures() {
		return lineFeatures;
	}
	
	void initFeatures()
	{
		lineFeatures.addFeature(new NumericFeatureExtractor("Indentation") {
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				BoundingBox box = node.getItem().getBox();
				addValue(box.getLeft());
			}
		});
		lineFeatures.addFeature(new NumericFeatureExtractor("Line Width") {
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				BoundingBox box = node.getItem().getBox();
				addValue(box.getWidth());
			}
		});
		lineFeatures.addFeature(new NumericFeatureExtractor("Line Height") {
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				BoundingBox box = node.getItem().getBox();
				addValue(box.getHeight());
			}
		});
		lineFeatures.addFeature(new NumericFeatureExtractor("Word per Line") {
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				int numOfWords = 0;
	    		for (PageModelNode<? extends PageItem> child : node.getChildren())
	    		{
	    			if (!isWord(child))
	    				continue;
	    			
					numOfWords++;
					// TODO calculate density
	    		}
				addValue(numOfWords);
			}
		});
		lineFeatures.addFeature(new NumericFeatureExtractor("Padding") {
			private PageModelNode<? extends PageItem> prev = null;
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				if (prev != null)
				{
					BoundingBox box = node.getItem().getBox();
					BoundingBox pBox = prev.getItem().getBox();
					addValue(box.getTop() - pBox.getBottom());
				}
				else 
				{
					addValue(Double.NaN);
				}
				
				prev = node;
			}
		});
	}
	
	@Override
    public void close()
    {
        if (factory != null && !factory.isClosed())
            factory.close(); 
    }

	// simple dumb checker for percentage of dark pixels
    private double getPixelDensity(BufferedImage src)
    {
        Raster data = src.getData();
        int w = src.getWidth();
        int h = src.getHeight();
        int numBands = src.getColorModel().getNumColorComponents();
        
//        double sum = 0;
//        double sumSq = 0;
        double ct = 0;
//        double n = 0;
//        double mean = 0;
//        double M2 = 0;
        boolean lastPxWasDark = false;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double[] buff= new double[numBands];
                buff = data.getPixel(x, y, buff);
                
                double avg = 0;
                for (int b = 0; b < numBands; b++) {
                    avg += buff[b];
                }
                
                double px = avg / numBands;
//                n++;
//                double delta = px - mean;
//                mean = mean + delta / n;
//                M2 = M2 + delta * (px - mean);
//                sum += px;
//                sumSq += px * px;
                boolean isDark = (px < 100);
                if (isDark != lastPxWasDark)
                {
                    ct++;
                    lastPxWasDark = isDark;
                }
                // sum += avg / numBands;
            }
        }
//        
//        int sz = w * h;
//        double var = sumSq - ((sum * sum) / sz) / sz;
//        double var = M2 / (n - 1);
        return ct / (w * h);
//        return sum / (w * h);
    }
    
    public void process(String name, BufferedImage src) throws ImageProcessorException {
    	// TODO use this to calculate a variety of page-level features
    	double pixelDensity = getPixelDensity(src);
    	if (pixelDensity < .001) {
    		// HACK: guard against Tesseract seg fault
    		System.out.println("Skipping Page: " + name + ": " + pixelDensity);
    		return;
    	}
    	
    	System.out.println("Prossessing Page: " + name + ": " + pixelDensity);
    	lineFeatures.mark(name);

    	TesseractPageAnalyzer analyser = new TesseractPageAnalyzer(factory); 
    	try {
    		PageModel model = analyser.analyze(src);
    		extractFeatures(model);
    	} catch (PageModelException e) {
    		throw new ImageProcessorException("Could not build page layout model.", e);
    	}
    }
	 
    private void visit(PageModelNode<? extends PageItem> node)
	{
		if (node.getLevel().getName().equals(TesseractLevelCatalog.TEXTLINE))
		{
			lineFeatures.addObservation(node);
		}
		
		for (PageModelNode<?> n : node.getChildren()) {
			visit(n);
		}
	}
	
	private void extractFeatures(PageModel model) {
		for (PageModelNode<?> n : model.getRoots())
		{
			visit(n);
		}
	}
	
	
}
