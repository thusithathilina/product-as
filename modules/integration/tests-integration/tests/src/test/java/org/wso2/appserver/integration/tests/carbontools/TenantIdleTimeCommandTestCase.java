/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.appserver.integration.tests.carbontools;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.utils.ASIntegrationConstants;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.CarbonCommandToolsUtil;
import org.wso2.appserver.integration.tests.carbontools.test.servers.CarbonTestServerManager;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.integration.common.extensions.usermgt.UserPopulator;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;

import java.util.HashMap;

import static org.testng.Assert.assertTrue;

/**
 * This class to test tenant ide time startup property (-Dtenant.idle.time)
 */
public class TenantIdleTimeCommandTestCase extends ASIntegrationTest {

    private HashMap<String, String> serverPropertyMap = new HashMap<String, String>();
    private AutomationContext context;
    private AutomationContext contextUser;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        context = new AutomationContext(ASIntegrationConstants.AS_PRODUCT_GROUP,
                ASIntegrationConstants.AS_INSTANCE_0002,
                ContextXpathConstants.SUPER_TENANT,
                ContextXpathConstants.ADMIN);

        contextUser = new AutomationContext(ASIntegrationConstants.AS_PRODUCT_GROUP,
                ASIntegrationConstants.AS_INSTANCE_0002,
                ContextXpathConstants.SUPER_TENANT,
                "userKey1");
        // to start the server from a different port offset
        /*int portOffset = 1;*/
        int portOffset = Integer.parseInt(context.getInstance().getProperty("portOffset"));
        int idleTime = 1;
        serverPropertyMap.put("-DportOffset", Integer.toString(portOffset));
        serverPropertyMap.put("-Dtenant.idle.time", Integer.toString(idleTime));

        CarbonTestServerManager.start(serverPropertyMap);

        UserPopulator userPopulator = new UserPopulator(ASIntegrationConstants.AS_PRODUCT_GROUP,
                                                        ASIntegrationConstants.AS_INSTANCE_0002);
        userPopulator.populateUsers();
    }

    @Test(groups = "wso2.as", description = "Clean tenants after idle time")
    public void testTenantIdleTime() throws Exception {

       /* AutomationContext context =
                new AutomationContext(ASIntegrationConstants.AS_PRODUCT_GROUP,
                                      ASIntegrationConstants.AS_INSTANCE_0002,
                                      ContextXpathConstants.SUPER_TENANT,
                                      ContextXpathConstants.ADMIN);
*/
        LoginLogoutClient loginLogoutClient = new LoginLogoutClient(context);
        String sessionCookie = loginLogoutClient.login();

        /*AutomationContext contextUser =
                new AutomationContext(ASIntegrationConstants.AS_PRODUCT_GROUP,
                                      ASIntegrationConstants.AS_INSTANCE_0002,
                                      ContextXpathConstants.SUPER_TENANT,
                                      "userKey1");*/

        LoginLogoutClient loginLogoutClientUser = new LoginLogoutClient(contextUser);
        loginLogoutClientUser.login();
        loginLogoutClient.logout();

        boolean isStatingToCleanTenant = CarbonCommandToolsUtil.searchOnLogs(
                context.getContextUrls().getBackEndUrl(), new String[]{"Starting to clean tenant"},
                sessionCookie);

        assertTrue(isStatingToCleanTenant, "Not cleaned the tenant successfully");
    }

    @AfterClass(alwaysRun = true)
    public void serverShutDown() throws Exception {
        CarbonTestServerManager.stop();
    }
}
