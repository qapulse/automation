package com.littleinc.MessageMe.net;

import java.util.Arrays;
import java.util.List;

public class ITunesSearchResult {

	int resultCount;
	ItunesMedia[] results;

	public int getResultCount() {
		return resultCount;
	}

	public void setResultCount(int resultCount) {
		this.resultCount = resultCount;
	}

	public ItunesMedia[] getResults() {
		return results;
	}

	public void setResults(ItunesMedia[] results) {
		this.results = results;
	}

	public List<ItunesMedia> getResultList() {
		return Arrays.asList(results);
	}
}
