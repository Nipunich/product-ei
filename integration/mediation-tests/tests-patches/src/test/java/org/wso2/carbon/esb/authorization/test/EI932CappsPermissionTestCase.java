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
import org.wso2.carbon.application.mgt.stub.ApplicationAdminExceptionException;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.admin.client.ApplicationAdminClient;
import org.wso2.carbon.integration.common.admin.client.CarbonAppUploaderClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import javax.activation.DataHandler;
import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;

/**
 * Test to verify if a carbon application can be 1) added 2) listed 3) deleted 4) browsed by a user with only the
 * specific permissions in "/permission/admin/manage/capps".
 */
public class EI932CappsPermissionTestCase extends ESBIntegrationTest {

    private AutomationContext esbContext;
    private String sessionCookie;
    private LoginLogoutClient loginLogoutClient;

    private final String ROLE_NAME = "CappsPermissionTestCaseRole";

    private final String USER_NAME = "CappsPermissionTestCaseUser";

    private final String CAPP_NAME = "capp-granular-permissions_1.0.0";

    private enum CappActions {
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
                ROOT_MANAGE_PERMISSION + "/capps/add", ROOT_MANAGE_PERMISSION + "/capps/list",
                ROOT_MANAGE_PERMISSION + "/capps", LOGIN_PERMISSION, ROOT_MANAGE_PERMISSION + "/mediation"
        };

        userManagement.addRole(ROLE_NAME, null, permissions);

        userManagement.addUser(USER_NAME, "password", new String[] { ROLE_NAME }, null);

        loginLogoutClient.logout();

    }

    /**
     * 1. Login as the newly created user above with granular permissions.
     * 2. Add a carbon application and verify if its successful by listing the carbon apps.
     * 3. Remove a carbon application and verify if its successfully removed by listing the carbon applications.
     *
     * @throws Exception if unexpected error occurs during test case.
     */
    @Test(groups = { "wso2.esb" },
          description = "Granular Permissions - Granular Permission for Capps add" + " and list")
    public void testListCarbonAppsWithoutRootManagePermission() throws Exception {

        sessionCookie = loginLogoutClient.login(USER_NAME, "password", "localhost");

        CarbonAppUploaderClient carbonAppUploader = new CarbonAppUploaderClient(
                esbContext.getContextUrls().getBackEndUrl(), sessionCookie);
        ApplicationAdminClient applicationAdminClient = new ApplicationAdminClient(
                esbContext.getContextUrls().getBackEndUrl(), sessionCookie);

        boolean isCarFileDeployed;
        boolean isCarFileDeleted;

        try {
            carbonAppUploader.uploadCarbonAppArtifact(CAPP_NAME + ".car", new DataHandler(
                    new URL("file:" + File.separator + File.separator + getESBResourceLocation() + File.separator
                            + "car" + File.separator + CAPP_NAME + ".car")));
        } catch (RemoteException e) {
            if (e.getMessage().contains("Access Denied.")) {
                Assert.fail("User " + USER_NAME + " with role " + ROLE_NAME + " is not allowed to upload "
                        + "carbon applications. " + e.getMessage());
            }
        }

        isCarFileDeployed = waitForActionCompletion(CappActions.ADD, applicationAdminClient);

        Assert.assertEquals(isCarFileDeployed, true, "Carbon application successfully uploaded and listed by user.");

        try {
            applicationAdminClient.deleteApplication(CAPP_NAME);
        } catch (RemoteException e) {
            if (e.getMessage().contains("Access Denied.")) {
                Assert.fail("User " + USER_NAME + " with role " + ROLE_NAME + " is not allowed to delete "
                        + "carbon applications. " + e.getMessage());
            }
        }

        isCarFileDeleted = waitForActionCompletion(CappActions.DELETE, applicationAdminClient);

        Assert.assertTrue(isCarFileDeleted, "Carbon application successfully deleted by user.");

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
        userManagementClient.deleteUser(USER_NAME);
        userManagementClient.deleteRole(ROLE_NAME);
        loginLogoutClient.logout();
    }

    /**
     * Wait until MAX_WAIT_FOR_CAPP_ACTION for an action (add/delete) on a carbon application to complete, while
     * listing the carbon applications every N seconds to check if action is complete.
     *
     * @param action                 Add or Delete a Carbon application.
     * @param applicationAdminClient Client stub for Application Admin service
     * @return true if 1) action is complete AND 2) if user was allowed to list carbon applications.
     */
    private boolean waitForActionCompletion(CappActions action, ApplicationAdminClient applicationAdminClient) {

        boolean isActionSuccessful = false;

        Calendar startTime = Calendar.getInstance();
        String[] applicationList = null;
        long time;

        long MAX_WAIT_FOR_CAPP_ACTION = 120000;
        log.info("waiting " + MAX_WAIT_FOR_CAPP_ACTION + " milliseconds for operation : " + action
                + " on carbon application.");

        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis()))
                < MAX_WAIT_FOR_CAPP_ACTION) {

            try {
                applicationList = applicationAdminClient.listAllApplications();

            } catch (ApplicationAdminExceptionException | RemoteException e) {
                if (e.getMessage().contains("Access Denied.")) {
                    Assert.fail("User " + USER_NAME + " with role " + ROLE_NAME
                            + " is not allowed to list carbon applications. " + e.getMessage());
                    break;
                }
            }

            switch (action) {
            case ADD:
                if (applicationList != null) {
                    if (ArrayUtils.contains(applicationList, CAPP_NAME)) {
                        isActionSuccessful = true;
                    }
                }
                break;
            case DELETE:
                if (applicationList != null) {
                    if (!ArrayUtils.contains(applicationList, CAPP_NAME)) {
                        isActionSuccessful = true;
                    }
                } else {
                    // empty list also indicates successful deletion.
                    isActionSuccessful = true;
                }
                break;
            default:
                log.error("Invalid action requested for Carbon application : " + action);
                return false;
            }

            if (!isActionSuccessful) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            } else {
                log.info("Operation : " + action + " performed on carbon application in " + time + " millseconds.");
                break;
            }
        }
        return isActionSuccessful;
    }
}
