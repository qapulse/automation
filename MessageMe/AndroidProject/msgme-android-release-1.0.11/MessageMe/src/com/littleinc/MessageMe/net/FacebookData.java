package com.littleinc.MessageMe.net;

import java.util.Arrays;
import java.util.List;

public class FacebookData<T> {

	T[] data;

	Paging paging;

	public T[] getData() {
		return data;
	}

	public List<T> getList() {
		return Arrays.asList(data);
	}

	public void setData(T[] data) {
		this.data = data;
	}

	public Paging getPaging() {
		return paging;
	}

	public void setPaging(Paging paging) {
		this.paging = paging;
	}

	public static class Paging {
		String next;

		public String getNext() {
			return next;
		}

		public void setNext(String next) {
			this.next = next;
		}

	}

}
