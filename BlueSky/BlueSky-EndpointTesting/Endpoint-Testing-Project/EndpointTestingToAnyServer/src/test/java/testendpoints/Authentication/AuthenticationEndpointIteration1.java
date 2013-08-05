package testendpoints.Authentication;

import testendpoints.util.BaseTest;
import com.sun.jersey.api.client.ClientResponse;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Units for <tt>Authentication testing</tt> API.
 */
/**
 * @author : Didier Corrales
 *
 *
 */
public class AuthenticationEndpointIteration1 extends BaseTest{

    public static final String BASE_PATH = getServerInformation()+"/csg/api/v1/authentication";

    public static final String BASE_URL_AUTHENTICATE = BASE_PATH + "/signin";

    private static final Log LOG = LogFactory.getLog(AuthenticationEndpointIteration1.class);


    @Test
    public void authenticationUserSMS() throws Exception{

        JSONObject jsonRequest = getJSONObjectToAuthenticate();

        ClientResponse response = apiRequestUsingJson(BASE_URL_AUTHENTICATE,jsonRequest);

        LOG.info(response.getEntity(String.class));
        Assert.assertEquals(200,response.getStatus());
    }


    /**
     * This method return the information to authenticate
     * @return JSONObject with information to authenticate
     * @throws JSONException in case there are errors.
     */
    public static JSONObject getJSONObjectToAuthenticate() throws JSONException{
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.accumulate("userName", "logntest");
        jsonRequest.accumulate("password", "passwrd");
        jsonRequest.accumulate("authenticationType", "SMS");
        return  jsonRequest;
    }



}
