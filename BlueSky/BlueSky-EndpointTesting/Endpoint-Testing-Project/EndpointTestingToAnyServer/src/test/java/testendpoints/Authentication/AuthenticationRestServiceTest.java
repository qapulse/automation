package testendpoints.Authentication;

import testendpoints.util.BaseTest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import javax.ws.rs.core.MediaType;

/**
 * Units for <tt>Authentication</tt> API.
 */
/**
 * @author : diegogamboa
 * Date: 4/24/13
 * Time: 3:20 PM
 */
public class AuthenticationRestServiceTest extends BaseTest {

    public static final String BASE_PATH = getServerInformation()+"/csg/api/v1/authentication";

    public static final String BASE_URL_AUTHENTICATE = BASE_PATH + "/signin";

    private static final Log LOG = LogFactory.getLog(AuthenticationRestServiceTest.class);

    /**
     * Authenticate API call Tests.
     *
     * @throws JSONException in case there are errors.
     */
    @Test
    public void authenticateTest() throws Exception {
        JSONObject jsonRequest = getJSONObjectToAuthenticate();
        ClientResponse result = getWebResponse(BASE_URL_AUTHENTICATE)
                .header("Authorization", this.getAuthenticationHeader())
                .post(ClientResponse.class, jsonRequest.toString());

        LOG.info("Method: authenticateTest, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Creates JSON to Authenticate.
     *
     * @return JSON with authentication information.
     * @throws JSONException in case there are errors.
     */
    public static JSONObject getJSONObjectToAuthenticate() throws JSONException{
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.accumulate("userName", "logntest");
        jsonRequest.accumulate("password", "passw0rd");
        jsonRequest.accumulate("authenticationType", "SMS");
        return  jsonRequest;
    }

    /**
     * Bluesky Authenticate API call Tests.
     *
     * @throws JSONException in case there are errors.
     */
    //TODO Uncomment test once a deploy to Dev is done due to /logout API in BSBE changed
    //@Test
    public void blueSkyAuthenticateTest()  throws Exception {
        //Login
        LOG.info("blueSkyLogin Test");
        WebResource webResource = getWebResponse(BASE_PATH+"/login/logntest/bed128365216c019988915ed3add75fb");
        ClientResponse response = webResource.accept("application/json")
                .header("Authorization", getAuthenticationHeader())
                .get(ClientResponse.class);


        LOG.info("Method: blueSkyAuthenticateTest - Login, Results:");
        LOG.info(response.getEntity(String.class));
        Assert.assertEquals(200, response.getStatus());

        //Logout
        LOG.info("blueSkyLogout Test");
        webResource = getWebResponse(BASE_PATH+"/logout");
        response = webResource.accept("application/json")
                .header("Authorization", getAuthenticationHeader())
                .get(ClientResponse.class);


        LOG.info("Method: blueSkyAuthenticateTest - Logout, Results:");
        LOG.info(response.getEntity(String.class));
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * SignUp API call Tests.
     * @throws JSONException in case there are errors.
     */
    @Test
    public void signUpTest() throws Exception {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.accumulate("firstName", "logn");
        jsonRequest.accumulate("lastName", "user");
        jsonRequest.accumulate("userName", "logn.user");
        jsonRequest.accumulate("emailId", "blueskyadmins@lognllc.com");
        jsonRequest.accumulate("password", "lognp@ss");
        jsonRequest.accumulate("organizationCode", "global");
        jsonRequest.accumulate("sessionToken", "NA");
        ClientResponse result = getWebResponse(BASE_PATH + "/signup")
                .header("Authorization", this.getAuthenticationHeader())
                .post(ClientResponse.class, jsonRequest.toString());

        LOG.info("Method: signUpTest, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Rumba Access Flow Test (login - samlValidate -  logout) API call Tests.
     * @throws JSONException in case there are errors.
     */
  /*  @Test
    public void accessFlowRumbaAPITest() throws JSONException {
        LOG.info("accessFlowRumbaAPITest Test");
        LOG.info("rumbaLogin Test");
        WebResource webResource = getWebResponse(BASE_PATH + "/rumba/login/user_name/password123");
        JSONObject result = webResource.header("Authorization", this.getAuthenticationHeader())
                .accept(MediaType.APPLICATION_JSON)
                .get(JSONObject.class);

        //Login successful
        Assert.assertEquals("Login successful", result.getString("responseMessage"));

        //SAML Validate
        String serviceTicket = result.getString("serviceTicket");
        if (!serviceTicket.isEmpty()){

            LOG.info("samlValidate Test");
            webResource = getWebResponse(BASE_PATH + "/rumba/saml/validate")
                    .queryParam("serviceTicket", serviceTicket);
            result = webResource.header("Authorization", this.getAuthenticationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .get(JSONObject.class);

            //SAML validation successful
            Assert.assertNotNull(result.get("UserId"));

            //Logout from Rumba
            LOG.info("rumbaLogout Test");
            ClientResponse profile = resource().path(BASE_PATH + "/rumba/logout")
                    .header("Authorization", this.getAuthenticationHeader())
                    .accept(MediaType.APPLICATION_JSON)
                    .get(ClientResponse.class);

            //Logout successful
            Assert.assertEquals(200, profile.getStatus());

        }
    }

    /**
     * Function to test the searching for organizations by postal code in Rumba.
     *
     * @throws Exception in case there are errors.
     */
   /* @Test
    public void rumbaSearchOrgByPostalCodeTest() throws Exception {
        LOG.info("rumbaSearchOrgByPostalCodeTest (GET)");
        WebResource webResource = getWebResponse(BASE_PATH + "/rumba/search/organization")
                .queryParam("postalCode", "01020");
        ClientResponse result = webResource.header("Authorization", this.getAuthenticationHeader())
                .get(ClientResponse.class);

        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Function to test the create user in Rumba.
     *
     * @throws Exception in case there are errors.
     */
    //Commented to avoid creating users in Rumba DB
    //@Test
  /*  public void rumbaCreateUserTest() throws Exception{
        LOG.info("rumbaCreateUserTest (POST)");
        String info = "{\"CreatedBy\":\"Self-reg\",\"SendEmailNotification\":\"true\",\"FirstName\":\"LOGN\",\"LastName\":\"USER\"," +
                "\"MiddleName\":\"TEST\",\"DisplayName\":\"LOGN_TEST_USER\",\"Gender\":\"Male\",\"UserName\":\"LOGN_CSG_NEW_USR5\"," +
                "\"Password\":\"password123\",\"EncryptionType\":\"SHA\",\"AuthenticationType\":\"SSO\",\"Title\": \"Professor.\",\"Suffix\":\"USR\"," +
                "\"BirthDate\":\"1975-12-30\",\"Language\":\"en-US\",\"UserStatus\":\"Active\",\"ResetFlag\":\"true\"," +
                "\"PasswordExpiryDate\":\"2013-10-29\",\"PreferredTimeZone\":\"America/Phoenix\",\"BusinessRuleSet\":\"HE\"," +
                "\"EmailInfo\":[{\"EmailIsPrimary\":\"true\",\"EmailAddress\":\"test@lognllc.com\",\"EmailStatus\":\"Active\"}]," +
                "\"AffiliationInfo\":[{\"AffiliationType\":\"User\",\"AffiliationStatus\":\"Confirmed\",\"OrgRole\":\"T\"," +
                "\"OrganizationId\":\"ff8080812b39f971012b633ee46605fc\",\"OtherOrg\":\"\"}],\"ConsentInfo\":[{\"PolicyDesc\":\"Terms of Use\"," +
                "\"ConsentMethod\":\"Self\",\"PolicyCategory\":\"Terms of Use\",\"PolicyVersion\":\"9\",\"ConsentFlag\":\"true\",\"ConsentDate\":\"2012-11-20\"}]," +
                "\"PhoneInfo\":[{\"PhoneType\":\"Work\",\"PhoneIsPrimary\":\"true\",\"CountryCode\":\"1\",\"PhoneNumber\":\"5557779999\",\"Extension\":\"1\"," +
                "\"PhoneStatus\":\"Active\"}],\"AddressInfo\":[{\"AddressType\":\"Shipping\",\"AddressName\":\"\",\"AddressIsPrimary\":\"true\"," +
                "\"AddressStatus\":\"Active\",\"AddressLine1\":\"10 Downing Street\",\"AddressLine2\":\"\",\"AddressLine3\":\"\",\"City\":\"London\"," +
                "\"DistrictCounty\":\"\",\"ProvinceState\":\"\",\"PostalCode\":\"77889\",\"Country\":\"GB\"}],\"AutoGenerated\":\"true\"}";

        ClientResponse result = resource().path(BASE_PATH + "/rumba/create/user")
                .header("Authorization", this.getAuthenticationHeader())
                .type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, info);

        Assert.assertEquals(200, result.getStatus());
    }      */
}
