package com.littleinc.MessageMe.net;

import java.io.Serializable;

public class ItunesMedia implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String wrapperType;
	private String kind;
	private String artistId;
	private String trackId;
	private String trackName;
	private String trackCensoredName;
	private String artistName;
	private String artistViewUrl;
	private String trackViewUrl;
	private String previewUrl;
	private String artworkUrl30;
	private String artworkUrl60;
	private String artworkUrl100;
	private String collectionPrice;
	private String trackPrice;
	private String releaseDate;
	private String collectionExplicitness;
	private String trackExplicitness;
	private String trackTimeMillis;
	private String currency;
	private String country;
	private String primaryGenreName;

	public String getWrapperType() {
		return wrapperType;
	}

	public void setWrapperType(String wrapperType) {
		this.wrapperType = wrapperType;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getArtistId() {
		return artistId;
	}

	public void setArtistId(String artistId) {
		this.artistId = artistId;
	}

	public String getTrackId() {
		return trackId;
	}

	public void setTrackId(String trackId) {
		this.trackId = trackId;
	}

	public String getTrackName() {
		return trackName;
	}

	public void setTrackName(String trackName) {
		this.trackName = trackName;
	}

	public String getTrackCensoredName() {
		return trackCensoredName;
	}

	public void setTrackCensoredName(String trackCensoredName) {
		this.trackCensoredName = trackCensoredName;
	}

	public String getArtistName() {
		return artistName;
	}

	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}

	public String getArtistViewUrl() {
		return artistViewUrl;
	}

	public void setArtistViewUrl(String artistViewUrl) {
		this.artistViewUrl = artistViewUrl;
	}

	public String getTrackViewUrl() {
		return trackViewUrl;
	}

	public void setTrackViewUrl(String trackViewUrl) {
		this.trackViewUrl = trackViewUrl;
	}

	public String getPreviewUrl() {
		return previewUrl;
	}

	public void setPreviewUrl(String previewUrl) {
		this.previewUrl = previewUrl;
	}

	public String getArtworkUrl30() {
		return artworkUrl30;
	}

	public void setArtworkUrl30(String artworkUrl30) {
		this.artworkUrl30 = artworkUrl30;
	}

	public String getArtworkUrl60() {
		return artworkUrl60;
	}

	public void setArtworkUrl60(String artworkUrl60) {
		this.artworkUrl60 = artworkUrl60;
	}

	public String getArtworkUrl100() {
		return artworkUrl100;
	}

	public void setArtworkUrl100(String artworkUrl100) {
		this.artworkUrl100 = artworkUrl100;
	}

	public String getCollectionPrice() {
		return collectionPrice;
	}

	public void setCollectionPrice(String collectionPrice) {
		this.collectionPrice = collectionPrice;
	}

	public String getTrackPrice() {
		return trackPrice;
	}

	public void setTrackPrice(String trackPrice) {
		this.trackPrice = trackPrice;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getCollectionExplicitness() {
		return collectionExplicitness;
	}

	public void setCollectionExplicitness(String collectionExplicitness) {
		this.collectionExplicitness = collectionExplicitness;
	}

	public String getTrackExplicitness() {
		return trackExplicitness;
	}

	public void setTrackExplicitness(String trackExplicitness) {
		this.trackExplicitness = trackExplicitness;
	}

	public String getTrackTimeMillis() {
		return trackTimeMillis;
	}

	public void setTrackTimeMillis(String trackTimeMillis) {
		this.trackTimeMillis = trackTimeMillis;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getPrimaryGenreName() {
		return primaryGenreName;
	}

	public void setPrimaryGenreName(String primaryGenreName) {
		this.primaryGenreName = primaryGenreName;
	}

}
