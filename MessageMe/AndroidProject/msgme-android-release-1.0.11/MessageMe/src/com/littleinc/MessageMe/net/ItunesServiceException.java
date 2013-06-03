package com.littleinc.MessageMe.net;

public class ItunesServiceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ItunesServiceException() {
		super();
	}

	public ItunesServiceException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public ItunesServiceException(String detailMessage) {
		super(detailMessage);
	}

	public ItunesServiceException(Throwable throwable) {
		super(throwable);
	}

}
