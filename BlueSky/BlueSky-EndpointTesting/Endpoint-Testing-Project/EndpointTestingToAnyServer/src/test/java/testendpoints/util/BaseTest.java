package testendpoints.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jettison.json.JSONObject;
import javax.ws.rs.core.MediaType;


/**
 * @author : Didier Corrales
 *
 *
 */
public abstract class BaseTest {

    public static String getAuthenticationHeader(){
       return PropertiesPlaceholder.getInstance().getProperty("test.csg.authentication.header");
    }

    public static String getServerInformation(){
        return PropertiesPlaceholder.getInstance().getProperty("test.csg.server.info");
    }

    /*
     * This method retrieves a api response based on the parameters and the url of the API
     *
     * @apiUrl: This is the url where the restApi
     * @parameters: These are the values that are in the json objet to be sent to the API
     *
     */
    public static ClientResponse apiRequestUsingJson(String apiUrl, JSONObject parameters) throws Exception{

        Client client = Client.create();

        WebResource webResource = client.resource(apiUrl);

        ClientResponse response = webResource.type(MediaType.APPLICATION_JSON)
                .header("Authorization", getAuthenticationHeader())
                .post(ClientResponse.class, parameters.toString());

        return response;
    }

    /*
    * This method retrieves a api response based
    *
    * @apiUrl: This is the url where the restApi
    *
    *
    */
    public WebResource  getWebResponse(String apiUrl) throws Exception{

        Client client = Client.create();

        WebResource webResource = client.resource(apiUrl);
        webResource.type(MediaType.APPLICATION_JSON);

        return webResource;
    }


}