/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.esb.mediators.cache;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import java.io.File;
import java.rmi.RemoteException;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * This class adds the test case for cache mediator honouring the MaxSize specified.
 */
public class ESBJAVA5214TestCase extends ESBIntegrationTest {

    @BeforeClass(alwaysRun = true)
    public void deployArtifacts() throws Exception {
        super.init();
        loadESBConfigurationFromClasspath(
                File.separator + "artifacts" + File.separator + "ESB" + File.separator + "mediatorconfig"
                        + File.separator + "cache" + File.separator + "MaxSize.xml");

    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @Test(groups = {"wso2.esb"}, description = "Tests whether maxSize value evicts the older messages")
    public void testMaxSize() throws RemoteException, XPathExpressionException, InterruptedException,
            LogViewerLogViewerException {
        String apiName = "maxSize";
        OMElement response;

        response = axis2Client.sendSimpleStockQuoteRequest(getApiInvocationURL(apiName), "", "ABCD");
        String responseABCD1 = response.getFirstElement().toString();

        response = axis2Client.sendSimpleStockQuoteRequest(getApiInvocationURL(apiName), "", "ABC");
        String responseABC1 = response.getFirstElement().toString();

        response = axis2Client.sendSimpleStockQuoteRequest(getApiInvocationURL(apiName), "", "AB");
        String responseAB1 = response.getFirstElement().toString();

        response = axis2Client.sendSimpleStockQuoteRequest(getApiInvocationURL(apiName), "", "AB");
        String responseAB2 = response.getFirstElement().toString();

        assertEquals(responseAB1, responseAB2, "The response for AB has not been cached");

        //Allow for eviction
        LogViewerClient logViewerClient = new LogViewerClient(contextUrls.getBackEndUrl(), getSessionCookie());
        LogEvent[] logEvents;
        String logMessage;
        long startTime = System.currentTimeMillis();
        boolean evicted = false;
        while (!evicted && (System.currentTimeMillis() - startTime) < 30000) {
            logEvents = logViewerClient.getAllRemoteSystemLogs();
            for (LogEvent logEvent : logEvents) {
                logMessage = logEvent.getMessage();
                if (logMessage.contains("Evicted")) {
                    evicted = true;
                    break;
                }
            }
        }

        response = axis2Client.sendSimpleStockQuoteRequest(getApiInvocationURL(apiName), "", "ABC");
        String responseABC2 = response.getFirstElement().toString();

        assertEquals(responseABC1, responseABC2, "The response for ABC has not been cached");

        response = axis2Client.sendSimpleStockQuoteRequest(getApiInvocationURL(apiName), "", "ABCD");
        String responseABCD2 = response.getFirstElement().toString();

        assertNotEquals(responseABCD1, responseABCD2, "The response for ABCD has not been evicted");
    }

    @AfterClass(alwaysRun = true)
    public void close() throws Exception {
        super.cleanup();
    }
}
