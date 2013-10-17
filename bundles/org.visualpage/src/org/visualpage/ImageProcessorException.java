package org.visualpage;

public class ImageProcessorException extends Exception {

	public ImageProcessorException() {
	}

	public ImageProcessorException(String message) {
		super(message);
	}

	public ImageProcessorException(Throwable cause) {
		super(cause);
	}

	public ImageProcessorException(String message, Throwable cause) {
		super(message, cause);
	}

	public ImageProcessorException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
