/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.esb.authorization.test;

import org.apache.commons.lang.ArrayUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.wso2.carbon.mediation.library.stub.upload.types.carbon.LibraryFileItem;
import org.wso2.esb.integration.common.clients.connector.MediationLibraryAdminServiceClient;
import org.wso2.esb.integration.common.clients.connector.MediationLibraryUploaderClient;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import javax.activation.DataHandler;
import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;

/**
 * Test to verify if a connector can be 1) added 2) listed 3) deleted 4) browsed by a user with only the
 * specific permissions in "/permission/admin/manage/connectors".
 * <p>
 * ** The connector/list and connector/add permissions are in effect only specified at the UI layer of the management
 * console. For the backend services utilized below, only the "mediation" permission is sufficient for connector
 * operations.
 */
public class EI932ConnectorsPermissionTestCase extends ESBIntegrationTest {

    private AutomationContext esbContext;
    private String sessionCookie;
    private LoginLogoutClient loginLogoutClient;

    private final String TEST_ROLE_NAME = "ConnectorPermissionTestRole";

    private final String TEMP_USER_NAME = "ConnectorPermissionTestUser";

    private final String CONNECTOR_QUALIFIED_NAME = "{org.wso2.carbon.connector}Sample";

    private enum ConnectorActions {
        ADD, DELETE
    }

    @BeforeClass(alwaysRun = true)
    public void addNonAdminUser() throws Exception {

        esbContext = new AutomationContext("ESB", TestUserMode.SUPER_TENANT_ADMIN);

        loginLogoutClient = new LoginLogoutClient(esbContext);

        sessionCookie = loginLogoutClient.login();

        UserManagementClient userManagement = new UserManagementClient(esbContext.getContextUrls().getBackEndUrl(),
                sessionCookie);

        String ROOT_MANAGE_PERMISSION = "/permission/admin/manage";
        String LOGIN_PERMISSION = "/permission/admin/login";
        String[] permissions = new String[] {
                ROOT_MANAGE_PERMISSION + "/connectors/add", ROOT_MANAGE_PERMISSION + "/connectors/list",
                ROOT_MANAGE_PERMISSION + "/connectors", LOGIN_PERMISSION, ROOT_MANAGE_PERMISSION + "/mediation"
        };

        userManagement.addRole(TEST_ROLE_NAME, null, permissions);

        userManagement.addUser(TEMP_USER_NAME, "password", new String[] { TEST_ROLE_NAME }, null);

        loginLogoutClient.logout();

    }

    /**
     * 1. Login as the newly created user above with granular permissions.
     * 2. Add a sample connector and verify if its successful by listing the connectors.
     * 3. Remove a connector and verify if its successfully removed by listing the connectors.
     *
     * @throws Exception if unexpected error occurs during test case.
     */
    @Test(groups = { "wso2.esb" },
          description = "Granular Permissions - Granular Permission for " + "Connectors add and list.")
    public void testListConnectorsWithoutRootManagePermission() throws Exception {

        sessionCookie = loginLogoutClient.login(TEMP_USER_NAME, "password", "localhost");

        MediationLibraryAdminServiceClient mediationLibraryAdminServiceClient = new MediationLibraryAdminServiceClient(
                esbContext.getContextUrls().getBackEndUrl(), sessionCookie);

        MediationLibraryUploaderClient mediationLibraryUploaderClient = new MediationLibraryUploaderClient(
                esbContext.getContextUrls().getBackEndUrl(), sessionCookie);

        LibraryFileItem uploadedFileItem = new LibraryFileItem();
        String CONNECTOR_FILE_NAME = "EI932-connector-1.0.0.zip";
        uploadedFileItem.setDataHandler(new DataHandler(
                new URL("file:" + File.separator + File.separator + getESBResourceLocation() + File.separator
                        + "connectors" + File.separator + CONNECTOR_FILE_NAME)));
        uploadedFileItem.setFileName(CONNECTOR_FILE_NAME);
        uploadedFileItem.setFileType("zip");

        LibraryFileItem[] uploadLibraryInfoList = new LibraryFileItem[] { uploadedFileItem };

        boolean isConnectorDeployed;
        boolean isConnectorDeleted;

        try {
            mediationLibraryUploaderClient.uploadConnector(uploadLibraryInfoList);
        } catch (RemoteException e) {
            if (e.getMessage().contains("Access Denied.")) {
                Assert.fail("User " + TEMP_USER_NAME + " with role " + TEST_ROLE_NAME
                        + " is not allowed to upload connectors. " + e.getMessage());
            }
        }

        isConnectorDeployed = waitForActionCompletion(ConnectorActions.ADD, mediationLibraryAdminServiceClient);

        Assert.assertEquals(isConnectorDeployed, true, "Connector successfully uploaded and listed by user.");

        try {
            mediationLibraryAdminServiceClient.deleteLibrary(CONNECTOR_QUALIFIED_NAME);
        } catch (RemoteException e) {
            if (e.getMessage().contains("Access Denied.")) {
                Assert.fail("User " + TEMP_USER_NAME + " with role " + TEST_ROLE_NAME
                        + " is not allowed to delete connectors." + e.getMessage());
            }
        }

        isConnectorDeleted = waitForActionCompletion(ConnectorActions.DELETE, mediationLibraryAdminServiceClient);

        Assert.assertTrue(isConnectorDeleted, "Connector successfully deleted by user.");

        loginLogoutClient.logout();
    }

    /**
     * Clean test case specific user + role as super admin.
     *
     * @throws Exception if error occurs while cleaning up test case artifacts.
     */
    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        sessionCookie = loginLogoutClient.login();
        UserManagementClient userManagementClient = new UserManagementClient(
                esbContext.getContextUrls().getBackEndUrl(), sessionCookie);
        userManagementClient.deleteUser(TEMP_USER_NAME);
        userManagementClient.deleteRole(TEST_ROLE_NAME);
        loginLogoutClient.logout();
    }

    /**
     * Wait until MAX_WAIT_FOR_CONNECTOR_ACTION for an action (add/delete) on a carbon application to complete, while
     * listing the carbon applications every N seconds to check if action is complete.
     *
     * @param action                             Add or Delete a Carbon application.
     * @param mediationLibraryAdminServiceClient Client stub for Connector Admin service
     * @return true if 1) action is complete AND 2) if user was allowed to list carbon applications.
     */
    private boolean waitForActionCompletion(ConnectorActions action,
            MediationLibraryAdminServiceClient mediationLibraryAdminServiceClient) {

        boolean isActionSuccessful = false;
        Calendar startTime = Calendar.getInstance();
        String[] connectorList = null;
        long time;

        long MAX_WAIT_FOR_CONNECTOR_ACTION = 120000;
        log.info("Waiting " + MAX_WAIT_FOR_CONNECTOR_ACTION + " milliseconds for operation : " + action + " on "
                + "connector.");

        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis()))
                < MAX_WAIT_FOR_CONNECTOR_ACTION) {
            try {
                connectorList = mediationLibraryAdminServiceClient.getAllLibraries();

            } catch (RemoteException e) {
                if (e.getMessage().contains("Access Denied.")) {
                    Assert.fail("User " + TEMP_USER_NAME + " with role " + TEST_ROLE_NAME + " is not allowed to list "
                            + "connectors. ", e);
                    break;
                }
            }

            switch (action) {
            case ADD:
                if ((connectorList != null) && ArrayUtils.contains(connectorList, CONNECTOR_QUALIFIED_NAME)) {
                    isActionSuccessful = true;
                }
                break;
            case DELETE:
                if ((connectorList != null) && !ArrayUtils.contains(connectorList, CONNECTOR_QUALIFIED_NAME)) {
                    isActionSuccessful = true;
                } else {
                    // empty list also indicates successful deletion.
                    isActionSuccessful = true;
                }
                break;
            default:
                log.error("Invalid action requested for Connector : " + action);
                return false;
            }

            if (!isActionSuccessful) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            } else {
                log.info("Operation : " + action + " performed on connector in " + time + " millseconds.");
                break;
            }

        }
        return isActionSuccessful;
    }
}
