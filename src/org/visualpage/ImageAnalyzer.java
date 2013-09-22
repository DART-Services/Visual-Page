package org.visualpage;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.dharts.dia.BoundingBox;
import org.dharts.dia.SimpleBoundingBox;
import org.dharts.dia.model.PageItem;
import org.dharts.dia.model.PageModel;
import org.dharts.dia.model.PageModelException;
import org.dharts.dia.model.PageModelNode;
import org.dharts.dia.tesseract.ImageAnalyzerFactory;
import org.dharts.dia.tesseract.TesseractException;
import org.dharts.dia.tesseract.model.TesseractLevelCatalog;
import org.dharts.dia.tesseract.model.TesseractPageAnalyzer;
import org.dharts.dia.threshold.FastSauvola;
import org.visualpage.features.DoubleList;
import org.visualpage.features.FeatureContext;
import org.visualpage.features.FeatureExtractorSet;
import org.visualpage.features.NumericFeatureExtractor;

public class ImageAnalyzer implements AutoCloseable {

    private ImageAnalyzerFactory factory;
    private FeatureExtractorSet<PageModelNode<? extends PageItem>> lineFeatures = new FeatureExtractorSet<>();
    private FeatureExtractorSet<PageContext> pageFeatures = new FeatureExtractorSet<>();
	private PageContext pageContext;

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

        initLineFeatures();
        initPageFeatures();
    }

	private static boolean isWord(PageModelNode<? extends PageItem> node)
	{
		return TesseractLevelCatalog.WORD.equals(node.getLevel().getName());
	}

	public FeatureExtractorSet<?> getLineFeatures() {
		return lineFeatures;
	}

	public FeatureExtractorSet<?> getPageFeatures() {
		return pageFeatures;
	}

	void initLineFeatures()
	{
		lineFeatures.addFeature(new BaseNodeExtractor("Indentation") {
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				BoundingBox box = node.getItem().getBox();
				addValue(box.getLeft());
			}
		});

		lineFeatures.addFeature(new BaseNodeExtractor("Line Width") {
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				BoundingBox box = node.getItem().getBox();
				addValue(box.getWidth());
			}
		});

		lineFeatures.addFeature(new BaseNodeExtractor("Line Width %") {
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				BoundingBox box = node.getItem().getBox();
				addValue((double)box.getWidth() / ctx.getWidth());
			}
		});

		lineFeatures.addFeature(new BaseNodeExtractor("Line Height") {
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				BoundingBox box = node.getItem().getBox();
				addValue(box.getHeight());
			}
		});

		lineFeatures.addFeature(new BaseNodeExtractor("Line Height %") {
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				BoundingBox box = node.getItem().getBox();
				addValue((double)box.getHeight() / ctx.getHeight());
			}
		});

		lineFeatures.addFeature(new BaseNodeExtractor("Words per Line") {
			@Override
			public void handle(PageModelNode<? extends PageItem> node) {
				int numOfWords = 0;
				for (PageModelNode<? extends PageItem> child : node.getChildren())
				{
					if (!isWord(child))
						continue;

					numOfWords++;
				}

				addValue(numOfWords);
			}
		});

		lineFeatures.addFeature(new PaddingExtractor());
	}

	void initPageFeatures()
	{
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Left Margin") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.getMargins().getLeft());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Top Margin") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.getMargins().getTop());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Right Margin") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.getMargins().getRight());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Bottom Margin") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.getMargins().getBottom());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Indent StdDev") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.getLeftSigma());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Foreground Ratio") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.getForegroundRatio());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Line Height Mean") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.lineHeight.getMean());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Line Height StdDev") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.lineHeight.getSigma());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Line Width Mean") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.lineWidth.getMean());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Line Width StdDev") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.lineWidth.getSigma());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Line Padding Mean") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.linePadding.getMean());
			}
		});
		pageFeatures.addFeature(new NumericFeatureExtractor<PageContext>("Line Padding StdDev") {

			@Override
			public void handle(PageContext observation) {
				addValue(observation.linePadding.getSigma());
			}
		});
	}

	@Override
    public void close()
    {
        if (factory != null && !factory.isClosed())
            factory.close();
    }

    public boolean process(String name, BufferedImage src) throws ImageProcessorException {
    	// TODO use this to calculate a variety of page-level features
//    	IntegralImageImpl iImage = new IntegralImageImpl();
//    	iImage.initialize(ImageUtils.grayscale(src));
//
//    	FastSauvola thresholder = new FastSauvola();
//    	thresholder.initialize(iImage);
//    	thresholder.setGenerateImage(false);
//
//    	// estimate number of lines based on projection profile and determine
//    	//		whether or not to proceed accordingly
//    	ProjectionProfiler profiler = ProjectionProfiler.create(iImage);
//		List<Integer> lines = profiler.findLines();
//		if (lines.size() < 4)
//			return false;

    	try {
			if (!BlankPageDetector.hasText(src))
				return false;
    	} catch (IOException | InterruptedException e) {
    		throw new ImageProcessorException("Failed to test for text", e);

    	}

    	TextLineContext ctx = new TextLineContext(src);
    	lineFeatures.mark(name);
    	pageFeatures.mark(name);
		lineFeatures.setContext(ctx);

		pageContext = new PageContext(src);
    	TesseractPageAnalyzer analyser = new TesseractPageAnalyzer(factory);
    	try {
    		PageModel model = analyser.analyze(src);
    		extractFeatures(model);

    		pageFeatures.addObservation(pageContext);
    		return true;
    	} catch (PageModelException e) {
    		throw new ImageProcessorException("Could not build page layout model.", e);
    	}
    }

    private void visit(PageModelNode<? extends PageItem> node)
	{
		if (node.getLevel().getName().equals(TesseractLevelCatalog.TEXTLINE))
		{
			lineFeatures.addObservation(node);
			pageContext.observeLine(node);
		}
		else if (node.getLevel().getName().equals(TesseractLevelCatalog.WORD))
		{
			pageContext.observeWord(node);
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

	private abstract static class BaseNodeExtractor extends NumericFeatureExtractor<PageModelNode<? extends PageItem>> {
		protected volatile TextLineContext ctx = null;

		private BaseNodeExtractor(String name) {
			super(name);
		}

		@Override
		public void setContext(FeatureContext ctx) {
			if (ctx != null && !TextLineContext.class.isInstance(ctx))
				throw new IllegalArgumentException("Invalid feature context. Expected instance of '" + TextLineContext.class.getName() + "'. Found '" + ctx.getClass().getName() + "'.");

			this.ctx = (TextLineContext)ctx;
		}
	}

	private final static class PaddingExtractor extends BaseNodeExtractor {
		private PageModelNode<? extends PageItem> prevTextline = null;

		private PaddingExtractor() {
			super("Padding");
		}

		@Override
		public void handle(PageModelNode<? extends PageItem> node) {
			if (prevTextline != null)
			{
				BoundingBox box = node.getItem().getBox();
				BoundingBox pBox = prevTextline.getItem().getBox();
				addValue(box.getTop() - pBox.getBottom());
			}
			else
			{
				addValue(Double.NaN);
			}

			prevTextline = node;
		}

		/**
		 * This implementation requires that the context be updated for each new
		 * document image to be processed, prior to performing any analytical work.
		 */
		@Override
		public void setContext(FeatureContext ctx) {
			prevTextline = null;
		}
	}

	final static class TextLineContext implements FeatureContext
	{
		private int width;
		private int height;

		TextLineContext(BufferedImage pg)
		{
			width = pg.getWidth();
			height = pg.getHeight();
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}
	}

	final static class PageContext implements FeatureContext
	{
		private int top, right, bottom;
		private int height;
		private int width;
		private int area;

		private BoundingBox pBox = null;
		DoubleList leftMargin = new DoubleList();

		DoubleList lineHeight = new DoubleList();
		DoubleList lineWidth = new DoubleList();
		DoubleList linePadding = new DoubleList();

		private int wordCoverage = 0;
		private FastSauvola thresholder;

		private PageContext(BufferedImage src)
		{
			thresholder = new FastSauvola();
	    	thresholder.initialize(src);

	    	try {
				thresholder.call();
			} catch (Exception e) {
				throw new IllegalStateException(e);		// HACK do something more intelligent/informative
			}

	    	width = thresholder.getWidth();
	    	height = thresholder.getHeight();
	    	area = thresholder.getArea();
	    	top = height;

	    	right = 0;
	    	bottom = 0;
		}

		/**
		 * Called while visiting the page for every
		 * @param node
		 */
		public void observeLine(PageModelNode<? extends PageItem> node) {
			BoundingBox box = node.getBox();

			// HACK: simple approach, we'll assume any textline we encounter is
			//       valid for determining margins

			leftMargin.add(box.getLeft());

			if (box.getTop() < top)
				top = box.getTop();

			if (box.getRight() > right)
				right = box.getRight();

			if (box.getBottom() > bottom)
				bottom = box.getBottom();

			lineHeight.add(box.getBottom() - box.getTop());
			lineWidth.add(box.getRight() - box.getLeft());
			lineWidth.add(box.getRight() - box.getLeft());

			if (pBox != null)
			{
				linePadding.add(box.getTop() - pBox.getBottom());
			}
			else
			{
				linePadding.add(Double.NaN);
			}

			pBox = box;
		}

		public void observeWord(PageModelNode<? extends PageItem> node) {
			wordCoverage += node.getBox().getArea();
		}

		/**
		 * @return The leftmost edge of a bounding box on the page. Note that this
		 * 		is only roughly approximates a margin.
		 */
		public long getLeftMargin() {
			return Math.round(leftMargin.getMin());
		}

		public BoundingBox getMargins()
		{
			long left = Math.round(leftMargin.getMin());
			if (left < right)
				left = right;
			if (top > bottom)
				top = bottom;
			return new SimpleBoundingBox(Long.valueOf(left).intValue(), top, right, bottom);
		}

		public double getLeftSigma()
		{
			return leftMargin.getSigma();
		}

		public double getForegroundRatio()
		{
			return (double)thresholder.getForegroundPixelCount() / area;
		}

		/**
		 * @return The precentage of the image area that is covered by
		 * 		the bounding boxes for words. This measure provides a
		 */
		public double getWordCoverage() {
			return (double)wordCoverage/area;
		}
	}
}
