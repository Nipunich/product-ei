/*
*  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.esb.samples.test.endpoint;

import org.testng.Assert;
import org.awaitility.Awaitility;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.tcpmon.client.ConnectionData;
import org.wso2.carbon.automation.test.utils.tcpmon.client.TCPMonListener;
import org.wso2.esb.integration.common.clients.mediation.SynapseConfigAdminClient;
import org.wso2.esb.integration.common.utils.common.AvailabilityPollingUtils;
import org.wso2.carbon.esb.samples.test.util.ESBSampleIntegrationTest;
import org.wso2.esb.integration.common.utils.servers.axis2.SampleAxis2Server;

import java.util.concurrent.TimeUnit;

/**
 * Sample 61: Routing a Message to a Dynamic List of Recipients
 */
public class Sample61TestCase extends ESBSampleIntegrationTest {

	private SampleAxis2Server axis2Server1;
	private SampleAxis2Server axis2Server2;
	private SampleAxis2Server axis2Server3;

	private TCPMonListener listener1;
	private TCPMonListener listener2;
	private TCPMonListener listener3;

    private static final int MAX_WAIT_TIME = 30000;
    private static final int POLLING_INTERVAL = 10000;
    private static final String HOST = "localhost";

    private static final int LISTEN_PORT1 = 9100;
    private static final int LISTEN_PORT2 = 9200;
    private static final int LISTEN_PORT3 = 9300;

    private static final int TARGET_PORT1 = 9001;
    private static final int TARGET_PORT2 = 9002;
    private static final int TARGET_PORT3 = 9003;

	@BeforeClass(alwaysRun = true)
	public void setEnvironment() throws Exception {
		super.init();

        Awaitility.await()
                  .pollInterval(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                  .atMost(MAX_WAIT_TIME, TimeUnit.MILLISECONDS)
                  .until(AvailabilityPollingUtils.isPortClosed(HOST, LISTEN_PORT1));
        Awaitility.await()
                  .pollInterval(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                  .atMost(MAX_WAIT_TIME, TimeUnit.MILLISECONDS)
                  .until(AvailabilityPollingUtils.isPortClosed(HOST, LISTEN_PORT2));
        Awaitility.await()
                  .pollInterval(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                  .atMost(MAX_WAIT_TIME, TimeUnit.MILLISECONDS)
                  .until(AvailabilityPollingUtils.isPortClosed(HOST, LISTEN_PORT3));
		loadSampleESBConfiguration(61);

		axis2Server1 = new SampleAxis2Server("test_axis2_server_9001.xml");
		axis2Server2 = new SampleAxis2Server("test_axis2_server_9002.xml");
		axis2Server3 = new SampleAxis2Server("test_axis2_server_9003.xml");

		axis2Server1.deployService(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE);
		axis2Server2.deployService(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE_2);
		axis2Server3.deployService(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE_3);

		axis2Server1.start();
		axis2Server2.start();
		axis2Server3.start();

        listener1 = new TCPMonListener(LISTEN_PORT1, HOST, TARGET_PORT1);
        listener2 = new TCPMonListener(LISTEN_PORT2, HOST, TARGET_PORT2);
        listener3 = new TCPMonListener(LISTEN_PORT3, HOST, TARGET_PORT3);


        SynapseConfigAdminClient synapseConfigAdminClient =
				new SynapseConfigAdminClient(contextUrls.getBackEndUrl(), getSessionCookie());
		String config = synapseConfigAdminClient.getConfiguration();
        config = config.replace(String.valueOf(TARGET_PORT1), String.valueOf(LISTEN_PORT1)).
                replace(String.valueOf(TARGET_PORT2), String.valueOf(LISTEN_PORT2)).
                               replace(String.valueOf(TARGET_PORT3), String.valueOf(LISTEN_PORT3));
		synapseConfigAdminClient.updateConfiguration(config);

		listener1.start();
		listener2.start();
		listener3.start();

        Awaitility.await()
                  .pollInterval(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                  .atMost(MAX_WAIT_TIME, TimeUnit.MILLISECONDS)
                  .until(AvailabilityPollingUtils.isHostAvailable(HOST, LISTEN_PORT1));
        Awaitility.await()
                  .pollInterval(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                  .atMost(MAX_WAIT_TIME, TimeUnit.MILLISECONDS)
                  .until(AvailabilityPollingUtils.isHostAvailable(HOST, LISTEN_PORT2));
        Awaitility.await()
                  .pollInterval(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                  .atMost(MAX_WAIT_TIME, TimeUnit.MILLISECONDS)
                  .until(AvailabilityPollingUtils.isHostAvailable(HOST, LISTEN_PORT3));

	}

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
	@Test(groups = { "wso2.esb" }, description = "Routing a Message to a Dynamic List of Recipients")
	public void testRoutingMessagesToDynamicListOfRecipients() throws Exception {

		axis2Client.sendPlaceOrderRequest(getMainSequenceURL(), null, "WSO2");
		Thread.sleep(5000);

		boolean is9001Called = isAxisServiceCalled(listener1);
		boolean is9002Called = isAxisServiceCalled(listener2);
		boolean is9003Called = isAxisServiceCalled(listener3);

		Assert.assertTrue(is9001Called, "Service 9001 not called");
		Assert.assertTrue(is9002Called, "Service 9002 not called");
		Assert.assertTrue(is9003Called, "Service 9003 not called");

		listener1.clear();
		listener2.clear();
		listener3.clear();

		axis2Client.sendPlaceOrderRequest(getMainSequenceURL(), null, "WSO2");

        Awaitility.await()
                  .pollInterval(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                  .atMost(MAX_WAIT_TIME, TimeUnit.MILLISECONDS)
                  .until(AvailabilityPollingUtils.isAxisServiceInvoked(listener1));
        Awaitility.await()
                  .pollInterval(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                  .atMost(MAX_WAIT_TIME, TimeUnit.MILLISECONDS)
                  .until(AvailabilityPollingUtils.isAxisServiceInvoked(listener2));
        Awaitility.await()
                  .pollInterval(POLLING_INTERVAL, TimeUnit.MILLISECONDS)
                  .atMost(MAX_WAIT_TIME, TimeUnit.MILLISECONDS)
                  .until(AvailabilityPollingUtils.isAxisServiceInvoked(listener3));

		is9001Called = isAxisServiceCalled(listener1);
		is9002Called = isAxisServiceCalled(listener2);
		is9003Called = isAxisServiceCalled(listener3);

        Assert.assertTrue(is9001Called, "Service" + TARGET_PORT1 +  " not called");
        Assert.assertTrue(is9002Called, "Service" + TARGET_PORT2 + " not called");
        Assert.assertTrue(is9003Called, "Service" + TARGET_PORT3 + " not called");;
	}

	@AfterClass(alwaysRun = true)
	public void close() throws Exception {
		super.cleanup();

		if (axis2Server1.isStarted()) {
			axis2Server1.stop();
		}
		if (axis2Server2.isStarted()) {
			axis2Server2.stop();
		}
		if (axis2Server3.isStarted()) {
			axis2Server3.stop();
		}

		listener1.stop();
		listener2.stop();
		listener3.stop();

	}

	private boolean isAxisServiceCalled(TCPMonListener listener) throws Exception {
		for (ConnectionData connectionData : listener.getConnectionData().values()) {
			System.out.print(connectionData.getOutputText().toString());
			if (connectionData.getOutputText().toString().contains("HTTP/1.1 202 OK")) {
				return true;
			}
		}

		return false;
	}
}