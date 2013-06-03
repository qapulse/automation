package com.littleinc.MessageMe.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.messageMe.OpenUDID.OpenUDID_manager;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Disposition;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.engine.Engine;
import org.restlet.ext.html.FormData;
import org.restlet.ext.html.FormDataSet;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.net.HttpClientHelper;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import com.coredroid.util.LogIt;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConfig;
import com.littleinc.MessageMe.MessageMeExternalAPI;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.bo.WebImageSearchResults;
import com.littleinc.MessageMe.chat.MediaManager;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Objects.PBImageBundle;
import com.littleinc.MessageMe.ui.MessageMeLauncher.AppVersion;
import com.littleinc.MessageMe.ui.SearchImagesActivity;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageUtil;

/**
 * This class handles all REST communication with the server.
 */
public class RestfulClient {

    private static final String GZIP_SUFFIX = ".gz";

    private static final String QUERY_KEY = "query";

    private static final String USER_ID_KEY = "user_id";

    private static final String ADDRESS_BOOK_KEY = "addressbook";

    private static RestfulClient instance;

    public static String sRestfulServerURL = null;

    public static String sRestfulServerMeta = null;

    private static final String restfulABUpload = "/ab/upload";

    private static final String restfulAccountFBCheck = "/account/facebook_check";

    private static final String restfulAccountFBClaim = "/account/facebook_claim";

    private static final String restfulAccountPwdReset = "/account/password_reset";

    private static final String restfulAccountNewPhone = "/account/phone_new";

    private static final String restfulAccountPhoneVerify = "/account/phone_verify";

    private static final String restfulCommandQuery = "/command/query";

    private static final String restfulRoomJoin = "/room/join";

    private static final String restfulRoomLeave = "/room/leave";

    private static final String restfulRoomNew = "/room/new";

    private static final String restfulRoomUpdate = "/room/update";

    private static final String restfulUserFriend = "/user/friend";

    private static final String restfulUserBlock = "/user/block";

    private static final String restfulUserLogin = "/user/login";

    private static final String restfulUserSearch = "/user/search";

    private static final String restfulUserSignUp = "/user/signup";

    private static final String restfulUserUnblock = "/user/unblock";

    private static final String restfulUserUnfriend = "/user/unfriend";

    private static final String restfulUserUpdate = "/user/update";

    private static final String restfulImageUpload = "/image/upload";

    private static final String restfulAccountInvite = "/account/invite";

    private static final String imageSearchEndpoint = "https://api.datamarket.azure.com/Data.ashx/Bing/Search/v1/Image";

    private static final String restfulAppUpgrade = "/meta/android_version";

    // Endpoints not yet implemented:
    //  /user/fb/login
    //  /account/email_new
    //
    // Unused endpoints:
    //  /ab/sync
    //  /user/auth

    public static synchronized String getRestfulURLBase() {
        if (sRestfulServerURL == null) {
            sRestfulServerURL = MessageMeApplication.getTargetConfig().get(
                    MessageMeConfig.KEY_REST_URL);
            LogIt.i(RestfulClient.class, "Loaded REST URL", sRestfulServerURL);
        }

        return sRestfulServerURL;
    }

    /**
     * Helper method to get the authorization token when the access token
     * and user information are both already saved in the MessageMeApplication
     * preferences.
     */
    public static String getAuthorizationToken() {

        if (MessageMeApplication.getPreferences().getUser() == null) {

            // This would indicate a code error.  We should never try to do
            // an operation which requires an auth token without already being
            // logged in by the time we call this method
            LogIt.w(RestfulClient.class, "No user to include in auth token");
            return "";
        }

        return buildAuthorizationToken(MessageMeApplication.getPreferences()
                .getToken(), MessageMeApplication.getPreferences().getUser()
                .getUserId(), OpenUDID_manager.getOpenUDID());
    }

    /**
     * Build the token required in the HTTP "Authorization" header, in the format: 
     *   "token=access_token|user_id|device_id". 
     *   
     * When used with the HTTP_BASIC scheme this results in a header like this:
     *   Authorization: Basic token=62abff53194c4a568a9a1d0523695891|894494663153684480|c9953f120aefa7fd
     *   
     * This is only required on some requests - see the definition of the
     * "restful_*" constants to see which require this.
     */
    public static String buildAuthorizationToken(String authToken, long userID,
            String deviceID) {

        if ((authToken == null) || (authToken.length() == 0) || (userID == -1)
                || (deviceID == null) || (deviceID.length() == 0)) {

            // This would indicate a code error.  We should never try to do
            // an operation which requires an auth token without already being
            // logged in by the time we call this method
            LogIt.w(RestfulClient.class, "Some auth information is missing");
            return "";
        }

        StringBuilder token = new StringBuilder();
        token.append("token=");
        token.append(authToken);
        token.append("|");
        token.append(Long.valueOf(userID));
        token.append("|");
        token.append(deviceID);

        // For security do not routinely log this.
        // LogIt.d(RestfulClient.class, "Authorization token", token);

        return token.toString();
    }

    public Room createNewGroup(Room room) throws ResourceException, IOException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulRoomNew);

        LogIt.i(this, "Create new group/room", endpoint);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        Representation representation = clientResource.post(room.toForm());
        PBCommandEnvelope commandEnvelope = PBCommandEnvelope
                .parseFrom(representation.getStream());

        return Room.parseFrom(commandEnvelope.getRoomNew().getRoom());
    }

    public void updateRoom(Room room, String pictureType)
            throws ResourceException, IOException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulRoomUpdate);

        LogIt.i(this, "Update group/room", endpoint);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        // The REST endpoint always returns success, so no need to check it. It
        // triggers a ROOM_UPDATE message on the web socket which we will 
        // handle separately. 
        clientResource.post(room.toFormForUpdate(pictureType));
    }

    /**
     * Search a user in the server that matches the given query
     * 
     * @param query Search term, this could be a phone, email or pin
     * @return The user/search endpoint returns a PBCommandEnvelope with commandType 309 (USER_SEARCH)
     */
    public PBCommandEnvelope userSearch(String query) throws ResourceException,
            IOException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulUserSearch);

        LogIt.i(this, "Search user/search", endpoint);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        Form form = new Form();
        form.add(QUERY_KEY, query);
        // Include unverified emails in the results
        form.add("include_unverified", "1");

        int countryCode = MessageMeApplication.getPreferences()
                .getUserCountryCode();

        // If the user has a country code then include it here.  This helps
        // the server guess the country code if one was not provided on the
        // search.
        if (countryCode != -1) {
            LogIt.d(this, "User country code", countryCode);
            form.add("country_code", String.valueOf(countryCode));
        }

        Representation representation = clientResource.post(form);
        return PBCommandEnvelope.parseFrom(representation.getStream());
    }

    /**
     * Uploads the address book
     * 
     * The upload call can also send a UserFriend command down the socket. If the upload matches 
     * any users on the back-end, you will get one UserFriend command sent to all user devices 
     * with any new friends that have been made
     * 
     * @return The ab/upload endpoint returns a PBCommandEnvelope with commandType 500 (AB_UPLOAD)
     */
    public PBCommandEnvelope abUpload(JSONObject data)
            throws ResourceException, IOException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulABUpload);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        File gzipFile = File.createTempFile(ADDRESS_BOOK_KEY, GZIP_SUFFIX,
                ImageUtil.getCacheFilesDir());

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(gzipFile);

            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(
                    fileOutputStream);
            gzipOutputStream.write(data.toString().getBytes());
            gzipOutputStream.flush();
            gzipOutputStream.close();

            FileRepresentation fileRepresentation = new FileRepresentation(
                    gzipFile, MediaType.APPLICATION_GNU_ZIP);

            FormDataSet form = new FormDataSet();
            form.setMultipart(true);

            int countryCode = MessageMeApplication.getPreferences()
                    .getUserCountryCode();

            // If the user has a country code then include it here.  This is
            // needed if they have not registered a phone number, as 
            // otherwise the server doesn't know what country to match 
            // phone numbers for (unless the phone numbers already include the 
            // country code in their device address book).
            if (countryCode != -1) {
                LogIt.d(this, "User country code", countryCode);
                form.add("country_code", String.valueOf(countryCode));
            }

            FormData attachment = new FormData(ADDRESS_BOOK_KEY,
                    fileRepresentation);
            form.getEntries().add(attachment);

            Representation representation = clientResource.post(form);
            return PBCommandEnvelope.parseFrom(representation.getStream());
        } finally {
            if (gzipFile != null) {
                gzipFile.delete();
            }
        }
    }

    /**
     * Send a list of contacts we've invited to the server so it can track it.
     * Does not return anything since the client doesn't care about the response.
     */
    public void contactInvite(JSONObject data) {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulAccountInvite);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        try {
            String filename = String.format("%s_%d", "invite",
                    (int) System.currentTimeMillis() / 1000);
            File gzipFile = File.createTempFile(filename, GZIP_SUFFIX,
                    ImageUtil.getCacheFilesDir());

            FileOutputStream fileOutputStream = new FileOutputStream(gzipFile);

            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(
                    fileOutputStream);
            gzipOutputStream.write(data.toString().getBytes());
            gzipOutputStream.flush();
            gzipOutputStream.close();

            FileRepresentation fileRepresentation = new FileRepresentation(
                    gzipFile, MediaType.APPLICATION_GNU_ZIP);

            FormDataSet form = new FormDataSet();
            form.setMultipart(true);

            FormData attachment = new FormData("invites", fileRepresentation);
            form.getEntries().add(attachment);

            clientResource.post(form);
        } catch (Exception e) {
            LogIt.d(this, "Contact invite operation failed", e);
        }
    }

    /**
     * Make friendship with a given user
     * This endpoint adds the given user into the contact list of the current user
     * 
     * @return The user/friend endpoint returns a PBCommandEnvelope with commandType 312 (USER_FRIEND)
     */
    public PBCommandEnvelope userFriend(long userId) throws ResourceException,
            IOException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulUserFriend);

        LogIt.i(this, "Add user as friend", endpoint);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        Form form = new Form();
        form.add(USER_ID_KEY, String.valueOf(userId));

        Representation representation = clientResource.post(form);
        return PBCommandEnvelope.parseFrom(representation.getStream());
    }

    /**
     * Remove friendship with a given user
     * This endpoint removes the given user from the contact list of the current user
     * 
     * @return The user/unfriend endpoint returns a PBCommandEnvelope with commandType 313 (USER_UNFRIEND)
     */
    public PBCommandEnvelope userUnfriend(long userId)
            throws ResourceException, IOException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulUserUnfriend);

        LogIt.i(this, "Remove user from friends", endpoint);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        Form form = new Form();
        form.add(USER_ID_KEY, String.valueOf(userId));

        Representation representation = clientResource.post(form);
        return PBCommandEnvelope.parseFrom(representation.getStream());
    }

    /**
     * Block the specified user
     * 
     * @return The REST endpoint returns a PBCommandEnvelope with a 
     *         USER_BLOCK command in it
     */
    public PBCommandEnvelope userBlock(long userToBlock)
            throws ResourceException, IOException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulUserBlock);

        LogIt.i(this, "Block user", endpoint);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        Form form = new Form();
        form.add(USER_ID_KEY, String.valueOf(userToBlock));

        Representation representation = clientResource.post(form);
        return PBCommandEnvelope.parseFrom(representation.getStream());
    }

    /**
     * Unblock the specified user
     * 
     * @return The REST endpoint returns a PBCommandEnvelope with a 
     *         USER_UNBLOCK command in it
     */
    public PBCommandEnvelope userUnblock(long userToUnblock)
            throws ResourceException, IOException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulUserUnblock);

        LogIt.i(this, "Unblock user", endpoint);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        Form form = new Form();
        form.add(USER_ID_KEY, String.valueOf(userToUnblock));

        Representation representation = clientResource.post(form);
        return PBCommandEnvelope.parseFrom(representation.getStream());
    }

    public void updateUser(User user, String pictureType)
            throws ResourceException, IOException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulUserUpdate);

        LogIt.i(this, "Update user", endpoint);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        // The REST endpoint always returns success, so no need to check it. It
        // triggers a USER_UPDATE message on the web socket which we will 
        // handle separately. 
        clientResource.post(user.toFormForUpdate(pictureType));
    }

    /**
     * Upload an image file to our Diesel image server.
     * 
     * This seems to fail silently on very large images.  On my Galaxy Nexus
     * a 3 megapixel photo uploads fine, but a 5 megapixel one fails.  The
     * message thread will show the upload has failed, but the only way to 
     * notice through Logcat is this warning log: 
     *   W System.err: Starting the HTTP client
     *   
     * This method should only be called with images up to approx 1000 pixels
     * in maximum dimension (that limit would need to be determined accurately
     * later if we ever need to stretch it). 
     * 
     * This returns the PBImageBundle that should be included in the 
     * MESSAGE_NEW being sent to the server over the WSChatConnection.
     */
    public PBImageBundle uploadPhoto(String imageFilePath,
            String imgSrvrFilename) throws ResourceException, IOException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulImageUpload);

        LogIt.i(this, "Upload photo", endpoint, imageFilePath, imgSrvrFilename);

        ClientResource clientResource = getClientResource(endpoint);

        File imageFile = new File(imageFilePath);

        InputStream response = null;

        if (imageFile.exists()) {

            FormDataSet form = new FormDataSet();

            form.setMultipart(true);

            form.getEntries().add(
                    new FormData("aws_bucket", MediaManager.AMAZON_S3_BUCKET));
            form.getEntries().add(
                    new FormData("aws_access_key", MessageMeApplication
                            .getPreferences().getAwsAccessKey()));
            form.getEntries().add(
                    new FormData("aws_secret_key", MessageMeApplication
                            .getPreferences().getAwsSecretKey()));
            form.getEntries().add(
                    new FormData("aws_security_token", MessageMeApplication
                            .getPreferences().getAwsSessionToken()));

            MediaType mediaType = MediaType.IMAGE_JPEG;

            // The server requires us to explicitly set the media type.  We 
            // generally only expect to see JPEGs, but try to support some
            // other common formats too.
            if (FileSystemUtil.getExtension(imageFilePath).equalsIgnoreCase(
                    ".jpg")
                    || FileSystemUtil.getExtension(imageFilePath)
                            .equalsIgnoreCase(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            } else if (FileSystemUtil.getExtension(imageFilePath)
                    .equalsIgnoreCase(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (FileSystemUtil.getExtension(imageFilePath)
                    .equalsIgnoreCase(".gif")) {
                mediaType = MediaType.IMAGE_GIF;
            } else {
                LogIt.w(this, "Unsupported image type", imageFilePath);
            }

            Representation file = new FileRepresentation(imageFile, mediaType);

            // Include the filename in the upload
            Disposition disp = new Disposition(Disposition.NAME_FILENAME);
            disp.setFilename(imgSrvrFilename);
            file.setDisposition(disp);

            FormData attachment = new FormData("image", file);
            form.getEntries().add(attachment);

            response = clientResource.post(form).getStream();
        } else {
            LogIt.w(this, "File does not exist, do not try to upload",
                    imageFilePath);
        }

        return PBImageBundle.parseFrom(response);
    }

    public PBCommandEnvelope newPhone(int countryCode, String phoneNumber,
            boolean isRequestPhoneCall) throws IOException {

        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulAccountNewPhone);

        LogIt.i(this, "Create new phone", endpoint);

        Form form = new Form();
        form.add("country_code", String.valueOf(countryCode));
        form.add("phone_number", phoneNumber);
        if (isRequestPhoneCall) {
            LogIt.d(this, "Requesting Voice SMS");
            form.add("request_phone_call", "1");
        }

        ClientResource clientResource = getClientResource(endpoint);

        Representation representation = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(representation.getStream());

    }

    public PBCommandEnvelope verifyPhone(String phoneNumber, int verification)
            throws IOException {

        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulAccountPhoneVerify);

        LogIt.i(this, "Verify phone", endpoint);

        Form form = new Form();
        form.add("phone_number_e164", phoneNumber);
        form.add("verification", String.valueOf(verification));

        ClientResource clientResource = getClientResource(endpoint);

        Representation representation = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(representation.getStream());
    }

    public PBCommandEnvelope userSignUp(String firstName, String lastName,
            String email, String deviceId, String deviceName, String password,
            String phoneNumber, String phoneSignature) throws IOException {

        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulUserSignUp);

        LogIt.i(this, "Sign Up", endpoint);

        Form form = new Form();
        form.add("first_name", firstName);
        form.add("last_name", lastName);
        form.add("email", email);
        form.add("password", password);
        form.add("device_id", deviceId);
        form.add("device_name", deviceName);

        if (phoneNumber != null && phoneSignature != null) {
            form.add("phone_number_e164", phoneNumber);
            form.add("phone_number_signature", phoneSignature);
        }

        ClientResource clientResource = getClientResource(endpoint);

        Representation rep = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(rep.getStream());
    }

    public PBCommandEnvelope userLoginWithEmail(String email, String deviceId,
            String deviceName, String password) throws IOException {

        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulUserLogin);

        LogIt.i(this, "Login with email", endpoint);

        Form form = new Form();

        form.add("email", email);
        form.add("password", password);
        form.add("device_id", deviceId);
        form.add("device_name", deviceName);

        ClientResource clientResource = getClientResource(endpoint);

        Representation rep = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(rep.getStream());
    }

    public PBCommandEnvelope userLoginWithPhone(String phone, int countryCode,
            String deviceId, String deviceName, String password)
            throws IOException {

        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulUserLogin);

        LogIt.i(this, "Login with phone", endpoint, phone, countryCode);

        Form form = new Form();

        form.add("phone_number", phone);
        form.add("password", password);
        form.add("device_id", deviceId);
        form.add("device_name", deviceName);

        // Add the country from the SIM card.  This field is an ordered, comma
        // separated list of numeric country codes that the server can use to
        // identify the phone number being logged in with.  In future we could 
        // use other sources of information to add other country codes here. 
        if ((countryCode != -1) && (countryCode != 0)) {
            form.add("country_codes", String.valueOf(countryCode));
        }

        ClientResource clientResource = getClientResource(endpoint);

        Representation rep = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(rep.getStream());
    }

    /**
     * Sends the server a POST with the new user and room to be added
     */
    public PBCommandEnvelope groupUserJoin(long roomId, long userId)
            throws IOException, ResourceException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulRoomJoin);

        LogIt.d(this, "User group join");

        Form form = new Form();

        form.add("room_id", String.valueOf(roomId));
        form.add("join_user_id", String.valueOf(userId));

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);
        Representation representation = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(representation.getStream());
    }

    /**
     * Sends the server a POST with the roomID the current
     * user will left. 
     */
    public PBCommandEnvelope groupUserLeave(long roomId) throws IOException,
            ResourceException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulRoomLeave);

        LogIt.d(this, "User group leave");

        Form form = new Form();

        form.add("room_id", String.valueOf(roomId));

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);
        Representation representation = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(representation.getStream());
    }

    /**
     * Checks if the provided facebook token
     * is already associated to an account
     */
    public PBCommandEnvelope facebookCheck(String token) throws IOException,
            ResourceException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulAccountFBCheck);

        LogIt.d(this, "Facebook check");

        Form form = new Form();
        form.add("auth_token", token);

        ClientResource clientResource = getClientResource(endpoint);

        Representation rep = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(rep.getStream());
    }

    /**
     * Associates a Facebook account with MM
     */
    public PBCommandEnvelope facebookClaim(String token) throws IOException,
            ResourceException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulAccountFBClaim);

        LogIt.d(this, "Facebook claim");

        Form form = new Form();
        form.add("auth_token", token);

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        Representation rep = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(rep.getStream());
    }

    /**
     * Gets commands for the provided channelID from before the provided
     * upperBoundCursor, up to a maximum of COMMAND_QUERY_BATCH_LIMIT.
     */
    public PBCommandEnvelope commandQuery(long channelID, long upperBoundCursor)
            throws IOException, ResourceException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulCommandQuery);

        LogIt.d(this, "Command query", channelID, upperBoundCursor);

        // There are some optional params we ignore:
        //  "limit" - the maximum number of commands to get (we prefer to
        //            let the server decide how many to give us).
        //  "after" - how far back to fetch commands to.
        Form form = new Form();
        form.add("recipient_id", String.valueOf(channelID));
        form.add("before", String.valueOf(upperBoundCursor));

        ClientResource clientResource = getClientResourceWithAuthentication(endpoint);

        Representation rep = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(rep.getStream());
    }

    /**
     * Resets the password of the current user account
     */
    public PBCommandEnvelope forgotPassword(String email) throws IOException,
            ResourceException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulAccountPwdReset);

        LogIt.d(this, "Forgot password");

        Form form = new Form();
        form.add("email", email);

        ClientResource clientResource = getClientResource(endpoint);

        Representation rep = clientResource.post(form);

        return PBCommandEnvelope.parseFrom(rep.getStream());
    }

    /**
     * Do a image web search using the Bing services
     * The response from the Bing services is a JSON which is 
     * converted to a java object using the GSON library
     * 
     * @see WebImageSearchResults for an example JSON response.
     */
    public WebImageSearchResults searchWebImages(String term)
            throws IOException, JsonSyntaxException, JSONException {
        // Azure link setup with Adult filter as Moderate
        String query = "?Query=%27" + term + "%27&Adult=%27Moderate%27&$top="
                + SearchImagesActivity.NUMBER_OF_RESULTS_TO_SHOW
                + "&$format=json";
        String endpoint = String.format("%s%s", imageSearchEndpoint, query);

        String accountKey = MessageMeExternalAPI.IMAGE_SEARCH_KEY;
        byte[] accountKeyBytes = Base64
                .encodeBase64((accountKey + ":" + accountKey).getBytes());
        String accountKeyEnc = new String(accountKeyBytes);

        WebImageSearchResults webImage = null;

        // Add Authorization header with account key       
        ChallengeResponse challengeResponse = new ChallengeResponse(
                ChallengeScheme.HTTP_BASIC);
        challengeResponse.setRawValue(accountKeyEnc);

        ClientResource clientResource = new ClientResource(endpoint);
        clientResource.setChallengeResponse(challengeResponse);

        JsonRepresentation rep = new JsonRepresentation(clientResource.get());

        String jsonResponse = rep.getJsonObject().toString();

        Gson gson = new Gson();

        // Parse the JSON response
        webImage = gson.fromJson(jsonResponse, WebImageSearchResults.class);

        return webImage;
    }

    /**
     * Obtain from the server the permitted app versions of the client
     */
    public AppVersion checkApplicationVersion() throws IOException,
            JsonSyntaxException, JSONException, ResourceException {
        String endpoint = String.format("%s%s", getRestfulURLBase(),
                restfulAppUpgrade);

        LogIt.d(this, "Check application version");

        ClientResource clientResource = getClientResource(endpoint);

        JsonRepresentation rep = new JsonRepresentation(clientResource.get());

        String jsonResponse = rep.getJsonObject().toString();

        Gson gson = new Gson();

        AppVersion appVersion = gson.fromJson(jsonResponse, AppVersion.class);

        return appVersion;

    }

    /** 
     * Get a ClientResource suitable for use against REST endpoints which do
     * not require authentication.  It is configured with a nice User-Agent. 
     */
    private ClientResource getClientResource(String endpoint) {

        ClientResource clientResource = new ClientResource(endpoint);

        // Set a useful User-Agent string 
        clientResource.getClientInfo().setAgent(
                MessageMeApplication.getUserAgent());

        return clientResource;
    }

    /** 
     * Get a ClientResource suitable for use against authenticated REST endpoints,
     * configured with a nice User-Agent. 
     */
    private ClientResource getClientResourceWithAuthentication(String endpoint) {

        ChallengeResponse challengeResponse = new ChallengeResponse(
                ChallengeScheme.HTTP_BASIC);
        challengeResponse.setRawValue(getAuthorizationToken());

        ClientResource clientResource = new ClientResource(endpoint);
        clientResource.setChallengeResponse(challengeResponse);

        // Set a useful User-Agent string 
        clientResource.getClientInfo().setAgent(
                MessageMeApplication.getUserAgent());

        return clientResource;
    }

    public static RestfulClient getInstance() {
        if (instance == null) {
            instance = new RestfulClient();

            // In Restlet 2.1 we need to register our connectors:
            //   http://wiki.restlet.org/docs_2.1/13-restlet/275-restlet/266-restlet.html
            //
            // The list of available connectors is here (the imports define
            // which HttpClientHelper is actually used):
            //   http://wiki.restlet.org/docs_1.1/13-restlet/27-restlet/37-restlet.html
            LogIt.d(RestfulClient.class, "Register Restlet connectors");

            // Clear old connectors
            Engine.getInstance().getRegisteredClients().clear();

            // This HttpClientHelper handles HTTPS communication too.  Attempting to 
            // add an HTTPS specific one seems to break HTTPS access. 
            Engine.getInstance().getRegisteredClients()
                    .add(new HttpClientHelper(null));

            // Avoid a Restlet bug which breaks HTTPS connection on some Android 
            // devices (seen on a Samsung Mini OS 2.2). 
            //   http://stackoverflow.com/q/12192536/112705
            System.setProperty("ssl.TrustManagerFactory.algorithm",
                    javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        }

        return instance;
    }
}