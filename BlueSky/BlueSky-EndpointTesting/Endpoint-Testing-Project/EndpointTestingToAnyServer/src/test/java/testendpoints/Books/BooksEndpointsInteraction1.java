package testendpoints.Books;

/**
 * Created with IntelliJ IDEA.
 * User: dcorrales
 * Date: 8/5/13
 * Time: 12:15 PM
 *
 */


import testendpoints.util.BaseTest;
import com.sun.jersey.api.client.ClientResponse;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BooksEndpointsInteraction1  extends BaseTest{

    public static final String BASE_PATH = getServerInformation()+"/csg/api/v1/books";
    private static final Log LOG = LogFactory.getLog(BooksEndpointsInteraction1.class);


    /**
     *  call Get details api
     *
     * @throws Exception
     */
    @Test
    public void getBookRegisteredDetails() throws Exception {

        String details = "296071";

        ClientResponse result = getWebResponse(BASE_PATH + "/registered/details/" + details)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getBookRegisteredDetails, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(404, result.getStatus());
    }

    /**
     *  call Get details api without details parameters
     *
     * @throws Exception
     */
    @Test
    public void getBookRegisteredDetailsWithoutDetail() throws Exception {

        String details = "";

        ClientResponse result = getWebResponse(BASE_PATH + "/registered/details/" + details)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getBookRegisteredDetailsWithoutDetail, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

    /**
     *  call Get territories api without details parameters
     *
     * @throws Exception
     */
    @Test
    public void getTerritories() throws Exception {

        ClientResponse result = getWebResponse(BASE_PATH + "/territories")
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getTerritories, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /*** Authors *****/

    /*
    *  call get school  api
    *
            * @throws Exception
    */
    @Test
    public void getAuthors() throws Exception {

        String territoryId = "1";
        String schoolId = "1";

        ClientResponse result = getWebResponse(BASE_PATH + "/authors")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getAuthors, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }


  /*
  *  call get Authors  api without correct territory value
  *
  *  @throws Exception
  */
    @Test
    public void getAuthorsSendingStringTerritory() throws Exception {

        String territoryId = "Florida";
        String schoolId = "1";


        ClientResponse result = getWebResponse(BASE_PATH + "/authors")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getAuthorsSendingStringTerritory, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

   /*
    *  call get Authors  api without  territory value
    *
    *  @throws Exception
    */
    @Test
    public void getAuthorsSendingNoTerritory() throws Exception {

        String territoryId = "";
        String schoolId = "1";

        ClientResponse result = getWebResponse(BASE_PATH + "/authors")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getAuthorsSendingNoTerritory, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(400, result.getStatus());
    }

    /*
     *  call get authors  api sending invalid parameters in territory value
     *
     *  @throws Exception
     */
    @Test
    public void getAuthorsSendingInvalidValuesTerritory() throws Exception {

        String territoryId = "@@@@33$$$%%%%%";
        String schoolId = "1";

        ClientResponse result = getWebResponse(BASE_PATH + "/authors")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getAuthorsSendingInvalidValuesTerritory, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

    /*
    *  call get authors  api sending invalid parameters in territory value   and in school
    *
    *  @throws Exception
    */
    @Test
    public void getAuthorsSendingInvalidValuesTerritoryAndSchool() throws Exception {

        String territoryId = "@@@@33$$$%%%%%";
        String schoolId = "#########";

        ClientResponse result = getWebResponse(BASE_PATH + "/authors")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getAuthorsSendingInvalidValuesTerritoryAndSchool, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

  /*
   *  call get authors  api sending invalid parameters in territory value   and in school
   *
   *  @throws Exception
   */
    @Test
    public void getAuthorsSendingInvalidValuesSchool() throws Exception {

        String territoryId = "1";
        String schoolId = "#########";

        ClientResponse result = getWebResponse(BASE_PATH + "/authors")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getAuthorsSendingInvalidValuesSchool, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

    /*
   *  call get authors  api without  school value
   *
   *  @throws Exception
   */
    @Test
    public void getAuthorsWithoutSchool() throws Exception {

        String territoryId = "1";
        String schoolId = "";

        ClientResponse result = getWebResponse(BASE_PATH + "/authors")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getAuthorsWithoutSchool, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(400, result.getStatus());
    }


    /*** Territories *****/

    /*
    *  call get school  api
    *
            * @throws Exception
    */
    @Test
    public void getSchools() throws Exception {

        String territoryId = "1";

        ClientResponse result = getWebResponse(BASE_PATH + "/schools")
                .queryParam("territoryId", territoryId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getSchools, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }


    /*
    *  call get school  api without correct territory value
    *
    *  @throws Exception
    */
    @Test
    public void getSchoolsSendingStringTerritory() throws Exception {

        String territoryId = "Florida";

        ClientResponse result = getWebResponse(BASE_PATH + "/schools")
                .queryParam("territoryId", territoryId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getSchoolsSendingStringTerritory, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

    /*
     *  call get school  api without  territory value
     *
     *  @throws Exception
     */
    @Test
    public void getSchoolsSendingNoTerritory() throws Exception {

        String territoryId = "";

        ClientResponse result = getWebResponse(BASE_PATH + "/schools")
                .queryParam("territoryId", territoryId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getSchoolsSendingNoTerritory, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(400, result.getStatus());
    }

    /*
     *  call get school  api without  sending invalid parameters in territory value
     *
     *  @throws Exception
     */
    @Test
    public void getSchoolsSendingInvalidValuesTerritory() throws Exception {

        String territoryId = "@@@@33$$$%%%%%";

        ClientResponse result = getWebResponse(BASE_PATH + "/schools")
                .queryParam("territoryId", territoryId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getSchoolsSendingInvalidValuesTerritory, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

    /***  salesRepReport *****/


    /*
    *  call get SalesRepReport  api without correct territory value
    *
    *  @throws Exception
    */
    @Test
    public void getSalesRepReportSendingStringTerritory() throws Exception {

        String territoryId = "Florida";
        String schoolId = "1";
        String author = "Public,User";

        ClientResponse result = getWebResponse(BASE_PATH + "/salesRepReport")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .queryParam("author", author)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getSalesRepReportSendingStringTerritory, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

    /*
     *  call get SalesRepReport  api without  territory value
     *
     *  @throws Exception
     */
    @Test
    public void getSalesRepReportSendingNoTerritory() throws Exception {

        String territoryId = "";
        String schoolId = "1";
        String author = "Public,User";

        ClientResponse result = getWebResponse(BASE_PATH + "/salesRepReport")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .queryParam("author", author)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);


      
        LOG.info("Method: getSalesRepReportSendingNoTerritory, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(400, result.getStatus());
    }

    /*
     *  call get SalesRepReport  api sending invalid parameters in territory value
     *
     *  @throws Exception
     */
    @Test
    public void getSalesRepReportSendingInvalidValuesTerritory() throws Exception {

        String territoryId = "@@@@33$$$%%%%%";
        String schoolId = "1";
        String author = "Public,User";

        ClientResponse result = getWebResponse(BASE_PATH + "/salesRepReport")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .queryParam("author", author)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getSalesRepReportSendingInvalidValuesTerritory, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

    /*
    *  call get SalesRepReport  api sending invalid parameters in territory value   and in school
    *
    *  @throws Exception
    */
    @Test
    public void getSalesRepReportSendingInvalidValuesTerritoryAndSchool() throws Exception {

        String territoryId = "@@@@33$$$%%%%%";
        String schoolId = "#########";
        String author = "Public,User";

        ClientResponse result = getWebResponse(BASE_PATH + "/salesRepReport")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .queryParam("author", author)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getSalesRepReportSendingInvalidValuesTerritoryAndSchool, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

    /*
     *  call get SalesRepReport  api sending invalid parameters in territory value   and in school
     *
     *  @throws Exception
     */
    @Test
    public void getSalesRepReportSendingInvalidValuesSchool() throws Exception {

        String territoryId = "1";
        String schoolId = "#########";
        String author = "Public,User";

        ClientResponse result = getWebResponse(BASE_PATH + "/salesRepReport")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .queryParam("author", author)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getSalesRepReportSendingInvalidValuesSchool, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(417, result.getStatus());
    }

   /*
   *  call get SalesRepReport  api without  school value
   *
   *  @throws Exception
   */
    @Test
    public void getSalesRepReportsWithoutSchool() throws Exception {

        String territoryId = "1";
        String schoolId = "";
        String author = "Public,User";

        ClientResponse result = getWebResponse(BASE_PATH + "/salesRepReport")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .queryParam("author", author)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getSalesRepReportsWithoutSchool, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(400, result.getStatus());
    }

  /*
   *  call get SalesRepReport  api sending invalid author
   *
   *  @throws Exception
   */
    @Test
    public void getSalesRepReportSendingInvalidAuthor() throws Exception {

        String territoryId = "1";
        String schoolId = "1";
        String author = "0000";

        ClientResponse result = getWebResponse(BASE_PATH + "/salesRepReport")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .queryParam("authorName", author)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);


        LOG.info("Method: getSalesRepReportSendingInvalidAuthor , Results:" );
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

 /*
  *  call get SalesRepReport  api sending invalid values in author parameter
  *
  *  @throws Exception
  */
    @Test
    public void getSalesRepReportSendingInvalidValuesAuthor() throws Exception {

        String territoryId = "1";
        String schoolId = "1";
        String author = "##########";

        ClientResponse result = getWebResponse(BASE_PATH + "/salesRepReport")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .queryParam("authorName", author)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);


        LOG.info("Method: getSalesRepReportSendingInvalidAuthor , Results:" );
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /*
  *  call get SalesRepReport  api sending invalid values in author parameter
  *
  *  @throws Exception
  */
    @Test
    public void getSalesRepReportSendingJustOneValueAuthor() throws Exception {

        String territoryId = "1";
        String schoolId = "1";
        String author = "LAPOG,Maheshwari";

        ClientResponse result = getWebResponse(BASE_PATH + "/salesRepReport")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .queryParam("authorName", author)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);


        LOG.info("Method: getSalesRepReportSendingInvalidAuthor , Results:" );
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }


    /*
*  call get  salesRepReport  api
*
        * @throws Exception
*/
    @Test
    public void getSalesRepReport() throws Exception {

        String territoryId = "1";
        String schoolId = "1";
        String author = "Public,User";

        ClientResponse result = getWebResponse(BASE_PATH + "/salesRepReport")
                .queryParam("territoryId", territoryId)
                .queryParam("schoolId", schoolId)
                .queryParam("authorName", author)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getSalesRepReport, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

   /******* getBookCCCOrders ********/

    /*
    *  call get  getBookCCCOrders  api
    *
    * @throws Exception
    */
   @Test
   public void getBookCCCOrders() throws Exception {
       String bookBuildContentId="402881843a1e8233013a1e8493a60002";

       ClientResponse result = getWebResponse(BASE_PATH + "/getCCCOrders")
               .queryParam("bookBuildContentId", bookBuildContentId)
               .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

       LOG.info("Method: getBookCCCOrders, Results:");
       LOG.info(result.getEntity(String.class));
       Assert.assertEquals(200, result.getStatus());
   }


    /*
  *  call get  getBookCCCOrders  api    without parameter
  *
  * @throws Exception
  */
    @Test
    public void getBookCCCOrdersWithoutParameter() throws Exception {
        String bookBuildContentId="";

        ClientResponse result = getWebResponse(BASE_PATH + "/getCCCOrders")
                .queryParam("bookBuildContentId", bookBuildContentId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getBookCCCOrdersWithoutParameter, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(400, result.getStatus());
    }


 /*
  *  call get  getBookCCCOrders  api  sending invalid information in the parameter
  * @throws Exception
  */
    @Test
    public void getBookCCCOrdersInvalidParameter() throws Exception {
        String bookBuildContentId="#########";

        ClientResponse result = getWebResponse(BASE_PATH + "/getCCCOrders")
                .queryParam("bookBuildContentId", bookBuildContentId)
                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: getBookCCCOrdersInvalidParameter, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }


    /******** checkOrderDetailStatus *****/

     /*
    *  call get  checkOrderDetailStatus  api
    *
    * @throws Exception
    */
    @Test
    public void checkOrderDetailStatus() throws Exception {
        String order="63454056";

        ClientResponse result = getWebResponse(BASE_PATH + "/orders/details/"+order+"/status")

                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: checkOrderDetailStatus, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }


  /*
   *  call get  checkOrderDetailStatus  api sending invalid order details
   *
   * @throws Exception
   */
    @Test
    public void checkOrderDetailStatusSendingInvalidOrderStatus() throws Exception {
        String order="123456789";

        ClientResponse result = getWebResponse(BASE_PATH + "/orders/details/"+order+"/status")

                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: checkOrderDetailStatusSendingInvalidOrderStatus, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

    /*
   *  call get  checkOrderDetailStatus  api sending invalid characteres in  order details
   *
   * @throws Exception
   */
    @Test
    public void checkOrderDetailStatusSendingInvalidCharactersInOrderStatus() throws Exception {
        String order="@@@@@####";

        ClientResponse result = getWebResponse(BASE_PATH + "/orders/details/"+order+"/status")

                .header("Authorization", this.getAuthenticationHeader()).get(ClientResponse.class);

        LOG.info("Method: checkOrderDetailStatusSendingInvalidCharactersInOrderStatus, Results:");
        LOG.info(result.getEntity(String.class));
        Assert.assertEquals(200, result.getStatus());
    }

}
