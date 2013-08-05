package testendpoints.Search;

import testendpoints.util.BaseTest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Units for the <tt>Search Endpoints</tt> API.
 */

/**
 * @author : diegogamboa
 *         Date: 4/24/13
 *         Time: 3:21 PM
 */
public class SearchRestServiceTest extends BaseTest {

    public static String BASE_PATH = getServerInformation() + "/csg/api/v1/search";


    /**
     * Resources call Tests
     *
     * @throws Exception
     */
    @Test
    public void resourcesTest() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH + "/resources")
                .queryParam("searchKeyword", "test")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);
        Assert.assertEquals(200, result.getStatus());
    }


    /**
     * Method to search resources by criteria call Tests
     *
     * @throws Exception
     */
    @Test
    public void thirdPartyResourcesTest() throws Exception {
        WebResource webResource = getWebResponse(BASE_PATH + "/third_party");
        ClientResponse result = webResource
                .queryParam("type", "print")
                .queryParam("title", "new")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Resources call Tests
     *
     * @throws Exception
     */
    @Test
    public void collectionsTest() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH + "/collections")
                .queryParam("searchKeyword", "*")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "10")
                .queryParam("isbn", "0205698034")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);
        Assert.assertEquals(200, result.getStatus());
    }
}
