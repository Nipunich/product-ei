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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * This class adds tests to verify if multi array JSON payloads before being cached and after being cached are
 * consistent. Checks if numbers set as String are converted to primitive after the response is cached by
 * the cache mediator.
 */
public class ESBJAVA5221TestCase extends ESBIntegrationTest {

    private LogViewerClient logViewer;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        logViewer = new LogViewerClient(contextUrls.getBackEndUrl(), getSessionCookie());
        loadESBConfigurationFromClasspath("artifacts" + File.separator + "ESB"
                + File.separator + "mediatorconfig" + File.separator + "cache" + File.separator
                + "JSONPayloadCacheAPI.xml");
    }

    @Test(groups = { "wso2.esb" }, description = "Test case to verify cache mediator behavior when payload is JSON.")
    public void testCachedResponseWithJsonPayload() throws Exception {
        //Call API first time and get the response
        HttpResponse response = HttpRequestUtil.sendGetRequest(
                getApiInvocationURL("cacheTest/api/json"),
                null);
        Assert.assertNotNull(response, "Response is null");
        String firstResponse = response.getData();

        //Call API second time and get the cached response
        response = HttpRequestUtil.sendGetRequest(
                getApiInvocationURL("cacheTest/api/json"),
                null);
        Assert.assertNotNull(response, "Response is null");
        String cachedResponse = response.getData();

        Assert.assertEquals(firstResponse, cachedResponse, "First response should match the cached response.");

        //Added to ensure that the carbon log is updated with the required entries
        TimeUnit.SECONDS.sleep(5);

        //Verify cache is hit by reading the logs
        LogEvent[] getLogsInfo = logViewer.getAllRemoteSystemLogs();
        boolean isResponseCached = false;
        for (LogEvent event : getLogsInfo) {
            if (event.getMessage().contains("********Cache is Hit*******")) {
                isResponseCached = true;
                break;
            }
        }

        Assert.assertTrue(isResponseCached, "Cache should be hit when calling the API for the second time.");
    }
}
