/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.mediators.iterator;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertTrue;


/**
 * https://wso2.org/jira/browse/ESBJAVA-4709
 * Aggregate mediator failed to handle empty JSON payload
 */
public class ESBJAVA4709AggregateEmptyJsonPayloadTestCase extends ESBIntegrationTest {
    private LogViewerClient logViewerClient;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        String artifactPath = "/artifacts/ESB/mediatorconfig/iterate/ESBJAVA4709-aggregateEmptyJsonPayloadTestProxy.xml";
        loadESBConfigurationFromClasspath(artifactPath);
        logViewerClient = new LogViewerClient(contextUrls.getBackEndUrl(), getSessionCookie());
        logViewerClient.clearLogs();
    }

    /**
     * At the proxy service, requests will be sent iteratively for the no content response backend, and received
     * responses will be aggregated
     *
     * @throws Exception
     */
    @Test(groups = "wso2.esb", description = "Test CorrelateOn in Aggregate mediator ")
    public void testAggregateEmptyJsonPayload() throws Exception {

        String inputPayload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "<soapenv:Header/>\n"
                + "<soapenv:Body>\n"
                + "<m0:getQuote xmlns:m0=\"http://services.samples\">\n"
                + " <m0:request>IBM\n"
                + " </m0:request>\n"
                + "   <m0:request>WSO2\n"
                + " </m0:request>\n"
                + "</m0:getQuote>\n"
                + "</soapenv:Body>\n"
                + "</soapenv:Envelope>";

        String expectedOutput = "<OverallResponse "
                + "xmlns=\"http://ws.apache.org/ns/synapse\"><jsonObject xmlns=\"\"/><jsonObject "
                + "xmlns=\"\"/></OverallResponse>";

        Map<String, String> requestHeader = new HashMap<>(3);
        requestHeader.put("Content-type", "text/xml");
        requestHeader.put("SOAPAction", "urn:mediate");
        requestHeader.put("Accept", "application/json");
        HttpRequestUtil.doPost(new URL(getProxyServiceURLHttp("aggregateEmptyJsonPayloadTestProxy")),
                inputPayload, requestHeader);

        LogEvent[] logs = logViewerClient.getAllRemoteSystemLogs();
        boolean logFound = false;
        for (LogEvent item : logs) {
            if (item.getPriority().equals("INFO")) {
                String message = item.getMessage();
                if (message.contains(expectedOutput)) {
                    logFound = true;
                    break;
                }
            }
        }
        assertTrue(logFound, "No content 204 responses are not properly aggregated at the aggregate mediator.");
    }

    @AfterClass(alwaysRun = true)
    public void stop() throws Exception {
        super.cleanup();
    }
}
