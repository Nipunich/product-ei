package org.wso2.carbon.esb.mediators.validate;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.esb.integration.common.clients.registry.ResourceAdminServiceClient;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import javax.activation.DataHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Test for ei-705. Send valid and invalid messages as a mix and check if Validate mediator
 * works as expected.
 */
public class EI705ValidateJSONSchemaTestCase extends ESBIntegrationTest {

    private Map<String, String> httpHeaders = new HashMap();

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        //upload the Json schema to registry
        ResourceAdminServiceClient resourceAdminServiceClient = new ResourceAdminServiceClient(contextUrls
                .getBackEndUrl(), getSessionCookie());
        URL url = new URL("file:///" + getESBResourceLocation() + File.separator
                + "mediatorconfig" + File.separator + "validate" + File.separator + "largeJsonSchema.json");
        resourceAdminServiceClient.addResource("/_system/config/largeJsonSchema.json",
                "application/json", "JSON Schema", new DataHandler(url));
        loadESBConfigurationFromClasspath("/artifacts/ESB/mediatorconfig/validate/jsonSchemaValidateConfig.xml");
        httpHeaders.put("Content-Type", "application/json");

    }

    /**
     * Send a series of valid and invalid requests and see if
     * validation works in the expected way
     *
     * @throws Exception on a test exception
     */
    @Test(groups = "wso2.esb", description = "Validating the valid and invalid requests against the JSON Schema")
    public void validAndInvalidRequestTest() throws Exception {
        sendInvalidRequestAndAssert();
        sendInvalidRequestAndAssert();
        sendInvalidRequestAndAssert();
        sendInvalidRequestAndAssert();
        sendInvalidRequestAndAssert();
        sendInvalidRequestAndAssert();
        sendValidRequestAndAssert();
        sendValidRequestAndAssert();
    }

    /**
     * Send a valid request and check the response
     *
     * @throws IOException on a communication exception
     * @throws AutomationFrameworkException on a test framework exception
     */
    private void sendValidRequestAndAssert() throws IOException, AutomationFrameworkException {
        String goodRequest = readFile( getESBResourceLocation() + File.separator
                + "mediatorconfig" + File.separator + "validate"
                + File.separator + "validRequest.json");
        HttpResponse response = doPost(new URL(getApiInvocationURL("myjson"))
                , goodRequest, httpHeaders);
        Assert.assertTrue((response.getData().contains("success"))
                , "Validation must be success. Response: " + response.getData());
    }

    /**
     * Send an invalid request and check the response
     *
     * @throws IOException on a communication exception
     * @throws AutomationFrameworkException on a test framework exception
     */
    private void sendInvalidRequestAndAssert() throws IOException, AutomationFrameworkException {
        String badRequest = readFile( getESBResourceLocation() + File.separator
                + "mediatorconfig" + File.separator + "validate"
                + File.separator + "invalidRequest.json");
        HttpResponse response = doPost(new URL(getApiInvocationURL("myjson"))
                , badRequest, httpHeaders);
        Assert.assertTrue((response.getData().contains("fail"))
                , "Validation must be fail. Response: " + response.getData());
    }

    /**
     * Read a file as an string
     * @param path file path
     * @return content of the file as a String
     * @throws IOException on a read communication issue
     */
    private String readFile(String path) throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    /**
     * Util to do a HTTP post
     *
     * @param endpoint endpoint to send post
     * @param postBody body to send in the post
     * @param headers  headers those should be in POST request
     * @return HttpResponse
     * @throws AutomationFrameworkException on a test framework issue
     * @throws IOException                  on a connection issue
     */
    private static HttpResponse doPost(URL endpoint, String postBody, Map<String, String> headers)
            throws AutomationFrameworkException, IOException {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) endpoint.openConnection();
            try {
                urlConnection.setRequestMethod("POST");
            } catch (ProtocolException e) {
                throw new AutomationFrameworkException("Shouldn't happen: HttpURLConnection doesn't support POST?? "
                        + e.getMessage(), e);
            }
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            for (Map.Entry<String, String> e : headers.entrySet()) {
                urlConnection.setRequestProperty((String) e.getKey(), (String) e.getValue());
            }
            OutputStream out = urlConnection.getOutputStream();
            try {
                Writer writer = new OutputStreamWriter(out, "UTF-8");
                writer.write(postBody);
                writer.close();
            } catch (IOException e) {
                throw new AutomationFrameworkException("IOException while posting data " + e.getMessage(), e);
            } finally {
            }
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), Charset.defaultCharset
                        ()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            } catch (FileNotFoundException ignored) {
                //ignore
            }
            Iterator<String> itr = urlConnection.getHeaderFields().keySet().iterator();
            Object responseHeaders = new HashMap();
            String key;
            while (itr.hasNext()) {
                key = (String) itr.next();
                if (key != null) {
                    ((Map) responseHeaders).put(key, urlConnection.getHeaderField(key));
                }
            }
            return new HttpResponse(sb.toString(), urlConnection.getResponseCode(), (Map) responseHeaders);
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            rd = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream(), Charset.defaultCharset()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            return new HttpResponse(sb.toString(), urlConnection.getResponseCode());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
