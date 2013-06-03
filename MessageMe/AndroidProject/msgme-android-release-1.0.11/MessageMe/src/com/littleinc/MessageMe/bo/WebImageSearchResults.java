package com.littleinc.MessageMe.bo;

import java.util.List;

import com.littleinc.MessageMe.util.FileSystemUtil;

/**
 * This class is intended to use as an object to parse the JSON
 * response from the Bing Search engine.
 * 
 * Not all the JSON fields of the actual response are represented 
 * in this class, only the ones that are more representative
 * 
 * As the JSON will be parsed into this class using GSON
 * the actual variable names MUST match their corresponding JSON fields.
 *
 * Here is an actual JSON response example:
 * 
 * {
 *   "d": {
 *       "results": [
 *           {
 *               "FileSize": "66043",
 *               "MediaUrl": "http:\/\/2.bp.blogspot.com\/-jcUxIdd9C0E\/T64OzLTzMlI\/AAAAAAAAB20\/yisn-Sp_Cps\/s1600\/microsoft-bing.jpg",
 *               "SourceUrl": "http:\/\/dreamtechnosoftworld.blogspot.com\/2012\/05\/microsofts-bing-gets-major-facelift.html",
 *               "DisplayUrl": "dreamtechnosoftworld.blogspot.com\/2012\/05\/microsofts-bing-gets...",
 *               "ID": "d1eccdfc-5daa-457c-a2a2-4f4a1c6e8b74",
 *               "Height": "481",
 *               "Width": "960",
 *               "Thumbnail": {
 *                   "FileSize": "6648",
 *                   "Height": "150",
 *                   "Width": "300",
 *                   "MediaUrl": "http:\/\/ts3.mm.bing.net\/th?id=H.4748886039332314&pid=15.1",
 *                   "__metadata": {
 *                       "type": "Bing.Thumbnail"
 *                   },
 *                   "ContentType": "image\/jpg"
 *               },
 *               "__metadata": {
 *                   "type": "ImageResult",
 *                   "uri": "https:\/\/api.datamarket.azure.com\/Data.ashx\/Bing\/Search\/v1\/Image?Query='bing'&Adult='Moderate'&$skip=0&$top=1"
 *               },
 *               "ContentType": "image\/jpeg",
 *               "Title": "IT'S ALL ABOUT TECH AND SOFT: Microsoft's Bing gets major facelift ..."
 *           },
 *           {
 *               "FileSize": "336774",
 *               "MediaUrl": "http:\/\/rockstartemplate.com\/wp-content\/gallery\/bing\/bing_homepage.jpg",
 *               "SourceUrl": "http:\/\/rockstartemplate.com\/tutorial\/websites\/microsoft-bing-pictures-show\/",
 *               "DisplayUrl": "rockstartemplate.com\/tutorial\/websites\/microsoft-bing-pictures-show",
 *               "ID": "eb30d134-6bb0-4b71-bf0b-ad819cf9736e",
 *               "Height": "515",
 *               "Width": "955",
 *               "Thumbnail": {
 *                   "FileSize": "5926",
 *                   "Height": "161",
 *                   "Width": "300",
 *                   "MediaUrl": "http:\/\/ts4.mm.bing.net\/th?id=H.4857892277388779&pid=15.1",
 *                   "__metadata": {
 *                       "type": "Bing.Thumbnail"
 *                   },
 *                   "ContentType": "image\/jpg"
 *               },
 *               "__metadata": {
 *                   "type": "ImageResult",
 *                   "uri": "https:\/\/api.datamarket.azure.com\/Data.ashx\/Bing\/Search\/v1\/Image?Query='bing'&Adult='Moderate'&$skip=1&$top=1"
 *               },
 *               "ContentType": "image\/jpeg",
 *               "Title": "Collection of Microsoft Bing Search Engine Pictures | images ..."
 *           }
 */
public class WebImageSearchResults {

    private ResultImages d;

    public ResultImages getWebImagesResults() {
        return d;
    }

    public class ResultImages {
        private List<ImageData> results;

        public List<ImageData> getResults() {
            return results;
        }

    }

    /**
     * The MediaUrl variable, will contain the actual image (full size)
     * 
     * The Thumbnail object will contain another MediaUrl, which will be
     * url of the thumbnail of the full sized image
     *
     */
    public class ImageData {

        private String Title;

        private String MediaUrl;

        private ImageThumbnail Thumbnail;

        public String getTitle() {
            return Title;
        }

        public String getMediaUrl() {
            return MediaUrl;
        }

        public ImageThumbnail getThumbnail() {
            return this.Thumbnail;
        }

        public String getMd5Key() {
            return FileSystemUtil.md5(getThumbnail().getMediaUrl());
        }

        public class ImageThumbnail {
            private String MediaUrl;

            public String getMediaUrl() {
                return MediaUrl;
            }
        }
    }
}