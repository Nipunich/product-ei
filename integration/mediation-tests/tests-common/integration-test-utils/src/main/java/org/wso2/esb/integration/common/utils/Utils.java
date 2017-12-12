/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.esb.integration.common.utils;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static OMElement getSimpleQuoteRequest(String symbol) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://services.samples", "ns");
        OMElement omGetQuote = fac.createOMElement("getSimpleQuote", omNs);
        OMElement value1 = fac.createOMElement("symbol", omNs);

        value1.addChild(fac.createOMText(omGetQuote, symbol));
        omGetQuote.addChild(value1);

        return omGetQuote;
    }

    public static OMElement getCustomQuoteRequest(String symbol) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace("http://services.samples", "ns");
        OMElement chkPrice = factory.createOMElement("CheckPriceRequest", ns);
        OMElement code = factory.createOMElement("Code", ns);
        chkPrice.addChild(code);
        code.setText(symbol);
        return chkPrice;
    }

    public static OMElement getStockQuoteRequest(String symbol) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://services.samples", "ns");
        OMElement method = fac.createOMElement("getQuote", omNs);
        OMElement value1 = fac.createOMElement("request", omNs);
        OMElement value2 = fac.createOMElement("symbol", omNs);

        value2.addChild(fac.createOMText(value1, symbol));
        value1.addChild(value2);
        method.addChild(value1);

        return method;
    }

	public static OMElement getIncorrectRequest(String stringValue) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace omNs = fac.createOMNamespace(
				"http://echo.services.core.carbon.wso2.org", "echo");
		OMElement method = fac.createOMElement("echoInt", omNs);
		OMElement value1 = fac.createOMElement("in", omNs);
		value1.setText(stringValue);
		method.addChild(value1);
		return method;
	}

    public static OMElement getCustomPayload(String symbol) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://services.samples", "ns");
        OMElement payload = fac.createOMElement("getQuote", omNs);
        OMElement request = fac.createOMElement("request", omNs);
        OMElement code = fac.createOMElement("Code", omNs);
        code.setText(symbol);

        request.addChild(code);
        payload.addChild(request);
        return payload;
    }

    /**
     * method to kill existing servers which are bind to the given port
     *
     * @param port
     */
    public static void shutdownFailsafe(int port) {
        try {
            System.out.println("Method to kill already existing servers in port " + port);
            Process p = Runtime.getRuntime().exec("lsof -Pi tcp:" + port);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            reader.readLine();
            line = reader.readLine();
            if (line != null) {
                line = line.trim();
                String processId = line.split(" +")[1];
                System.out.println("There is already a process using " + port + ", process id is - " + processId);
                if (processId != null) {
                    String killStr = "kill -9 " + processId;
                    System.out.println("kill string to kill the process - " + killStr);
                    Runtime.getRuntime().exec(killStr);

                    System.out.println(
                            "process " + processId + " killed successfully, which was running on port " + port);
                }
            } else {
                System.out.println("There are no existing processes running on port " + port);
            }
        } catch (Exception e) {
            System.out.println("Error killing the process which uses the port " + port);
        }
    }

    /**
     * Check for the existence of the given log message. The polling will happen in one second intervals.
     *
     * @param logViewerClient log viewer used for test
     * @param expected        expected log string
     * @param timeout         max time to do polling in seconds
     * @return true if the log is found with given timeout, false otherwise
     * @throws InterruptedException        if interrupted while sleeping
     * @throws RemoteException             due to a logviewer error
     * @throws LogViewerLogViewerException due to a logviewer error
     */
    public static boolean checkForLog(LogViewerClient logViewerClient, String expected, int timeout) throws
            InterruptedException, RemoteException, LogViewerLogViewerException {
        boolean logExists = false;
        for (int i = 0; i < timeout; i++) {
            TimeUnit.SECONDS.sleep(1);
            if (checkForLog(logViewerClient, expected)) {
                logExists = true;
                break;
            }
        }
        return logExists;
    }

    /**
     * Checks if a given message string appears in the log.
     *
     * @param logViewerClient log viewer client to be used to load the log
     * @param expected        the expected string in the log
     * @return
     * @throws RemoteException             if an error occurs in while loading the logs
     * @throws LogViewerLogViewerException if an error occurs in while loading the logs
     */
    private static boolean checkForLog(LogViewerClient logViewerClient, String expected)
            throws RemoteException, LogViewerLogViewerException {

        LogEvent[] systemLogs;
        systemLogs = logViewerClient.getAllRemoteSystemLogs();
        boolean matchFound = false;
        if (systemLogs != null) {
            for (LogEvent logEvent : systemLogs) {
                if (logEvent == null) {
                    continue;
                }
                if (logEvent.getMessage().contains(expected)) {
                    matchFound = true;
                    break;
                }
            }
        }
        return matchFound;
    }
}