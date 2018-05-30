/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.esb.mediators.call;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.base.CarbonBaseUtils;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.common.FileManager;

import java.io.File;
import java.net.URL;

import static org.testng.Assert.assertTrue;

/**
 * This test class verify the functionality of the PerAPI logs when the call mediator used in the sequence flow.
 * Ref: https://wso2.org/jira/browse/ESBJAVA-5216
 */
public class ESBJAVA5216CallMediatorPerAPILogTest extends ESBIntegrationTest {

    private ServerConfigurationManager serverConfigurationManager;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        String sourceLog4j = getESBResourceLocation() + File.separator + "mediatorconfig" + File.separator + "call"
                + File.separator + "config" + File.separator + "log4j.properties";
        String targetLog4j = CarbonBaseUtils.getCarbonHome() + File.separator
                + "conf" + File.separator + "log4j.properties";
        String artifactLocation = File.separator + "artifacts" + File.separator + "ESB" + File.separator
                + "mediatorconfig" + File.separator + "call" + File.separator + "ESBJAVA5216CallMediatorPerAPILogTest"
                + ".xml";
        init();
        serverConfigurationManager = new ServerConfigurationManager(context);
        File sourceLog4jFile = new File(sourceLog4j);
        File targetLog4jFile = new File(targetLog4j);
        serverConfigurationManager.applyConfiguration(sourceLog4jFile, targetLog4jFile, true, true);
        init();
        loadESBConfigurationFromClasspath(artifactLocation);
    }

    @Test(groups = "wso2.esb", description = "Test PerAPI log print after call mediator")
    public void perAPILogPrintTest() throws Exception {
        String logFileLocation = CarbonBaseUtils.getCarbonHome() + File.separator + "repository" + File.separator
                + "logs" + File.separator + "api" + File.separator + "test" + File.separator + "LogPrint.log";
        HttpResponse httpResponse = HttpRequestUtil.doPost(new URL(getApiInvocationURL("logprint")), "");
        assertTrue(httpResponse.getResponseCode() == 200, "Request sending failed");
        assertTrue(httpResponse.getData() != null, "Expected response haven't received");

        String fileContent = FileManager.readFile(logFileLocation);
        log.info("Log file content: " + fileContent);
        assertTrue(fileContent.contains("After"), "Log entry after the call mediator haven't been printed");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
        if (serverConfigurationManager != null) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
        serverConfigurationManager = null;
    }
}
