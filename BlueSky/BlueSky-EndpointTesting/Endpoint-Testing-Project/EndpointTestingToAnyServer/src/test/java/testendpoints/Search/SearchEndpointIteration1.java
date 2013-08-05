package testendpoints.Search;

import testendpoints.util.BaseTest;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Units for <tt>Search Endpoints</tt> API.
 */
/**
 * @author : Didier Corrales
 *
 *
 */
public class SearchEndpointIteration1 extends BaseTest {

    public static final String BASE_PATH = getServerInformation()+"/csg/api/v1/search";

    private static final Log LOG = LogFactory.getLog(SearchEndpointIteration1.class);

    /*** Search resources by topic   ***/

    /**
     * Call get resources api using a valid topic name in the topic filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesByTopic() throws Exception {

        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("topicName", "pricing")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesByTopic, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api using  an empty topic name in the topic filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesEmptyTopicParameter() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("topicName", "")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesEmptyTopicParameter, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }


    /**
     * Call get resources api using  an invalid topic name in the topic filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesInvalidTopicParameter() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("topicName", "noTopic")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesInvalidTopicParameter, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api using  an invalid topic name in the topic filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesInvalidCharactersInTopicParameter() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("topicName", "#$%^!@##$#$#$#$#$#$")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesInvalidCharactersInTopicParameter, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api using  more than one topic name in the topic filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesSendMoreThanOneTopicParameter() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("topicName", "finance money sports")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesSendMoreThanOneTopicParameter, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }


    /*** Search resources by country   ***/

    /**
     * Call get resources api using a valid country name in the country filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesByCountry() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("country", "France")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesByCountry, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api using a invalid country name in the country filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesSendingInvalidCountryParameter() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("country", "florida")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesSendingInvalidCountryParameter, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api using a  country name that has more than one parameter related to it in the country filter
     *
     * Assume the parameter USA has more than one resource related to it
     *
     * @throws Exception
     */
    @Test
    public void getManyResourcesByCountry() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("country", "USA")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getManyResourcesByCountry, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api using invalid characters the country filter
     *
     *
     * @throws Exception
     */
    @Test
    public void getResourceUsingInvalidCharacters() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("country", "##$#$#@#$#$#,,,,,---++++@#$$#$3")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourceUsingInvalidCharacters, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api using numbers the country filter
     *
     *
     * @throws Exception
     */
    @Test
    public void getResourceUsingNumbersInCountryParameter() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("country", "123456789")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourceUsingNumbersInCountryParameter, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api sending the country parameter as empty the country filter
     *
     *
     * @throws Exception
     */
    @Test
    public void getResourceSendingCountryEmpty() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("country", "")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourceSendingCountryEmpty, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /*** Search resources by topic   ***/

    /**
     * Call get resources api using a valid subject name in the subject filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesBySubject() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("subject", "models")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesBySubject, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api without send subject name in the subject filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesBySubjectWithoutSendSubjectValue() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("subject", "")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesBySubjectWithoutSendSubjectValue, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api sending an invalid subject name in the subject filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesBySubjectUsingInvalidSubjectName() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("subject", "nosubject")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesBySubjectUsingInvalidSubjectName, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api sending an invalid characters as subject name in the subject filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesBySubjectUsingInvalidCharactersSubjectName() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("subject", "#$%^!@##$#$#$#$#$#$")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesBySubjectUsingInvalidCharactersSubjectName, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /**
     * Call get resources api sending an more than one subject value in the subject filter
     *
     * @throws Exception
     */
    @Test
    public void getResourcesBySubjectSendingMoreThanOneSubjectValues() throws Exception {
        ClientResponse result = getWebResponse(BASE_PATH+"/resources")
                .queryParam("searchType", "multiCategory")
                .queryParam("pageNumber", "1")
                .queryParam("pageSize", "1")
                .queryParam("license", "OER,Pearson")
                .queryParam("subject", "finance money sports")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getResourcesBySubjectSendingMoreThanOneSubjectValues, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }
}