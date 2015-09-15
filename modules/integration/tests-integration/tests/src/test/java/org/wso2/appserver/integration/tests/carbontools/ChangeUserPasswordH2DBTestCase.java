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

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.integration.common.bean.DataSourceBean;
import org.wso2.appserver.integration.common.exception.CarbonToolsIntegrationTestException;
import org.wso2.appserver.integration.common.utils.ASIntegrationConstants;
import org.wso2.appserver.integration.common.utils.ASIntegrationTest;
import org.wso2.appserver.integration.common.utils.CarbonCommandToolsUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.engine.frameworkutils.enums.OperatingSystems;
import org.wso2.carbon.automation.extensions.servers.carbonserver.TestServerManager;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.integration.common.extensions.exceptions.AutomationExtensionException;
import org.wso2.carbon.integration.common.extensions.usermgt.UserPopulator;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertTrue;

/**
 * This class is to test change H2DB user password using chpasswd.sh/chpasswd.bat
 */
public class ChangeUserPasswordH2DBTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(ChangeUserPasswordH2DBTestCase.class);
    private AutomationContext automationContext;
    private AuthenticatorClient authenticatorClient;
    private static final char[] userNewPassword = {'t', 'e', 's', 't', 'u', '1', '2', '3', 'U', 'D', '9'};
    private DataSourceBean dataSourceBean;
    private static final String userName = "testu1";
    private int portOffset = 1;
    private TestServerManager testServerManager;

    @BeforeClass(alwaysRun = true)
    public void init() throws XPathExpressionException, AxisFault {
        automationContext =
                new AutomationContext(ASIntegrationConstants.AS_PRODUCT_GROUP,
                                      ASIntegrationConstants.AS_INSTANCE_0002,
                                      ContextXpathConstants.SUPER_TENANT,
                                      ContextXpathConstants.SUPER_ADMIN);

        authenticatorClient = new AuthenticatorClient(automationContext.getContextUrls().getBackEndUrl());

        dataSourceBean = CarbonCommandToolsUtil.getDataSourceInformation("default");

        portOffset = Integer.parseInt(automationContext.getInstance().getProperty("portOffset"));
    }

    @Test(groups = "wso2.as", description = "H2DB Password changing script run test")
    public void testScriptRunChangeUserPasswordH2DB() throws Exception {
        AutomationContext autoCtx = new AutomationContext();
        testServerManager = new TestServerManager(autoCtx, portOffset) {
            public void configureServer() throws AutomationFrameworkException {
                try {
                    testServerManager.startServer();
                    UserPopulator userPopulator = new UserPopulator(ASIntegrationConstants.AS_PRODUCT_GROUP,
                                                                    ASIntegrationConstants.AS_INSTANCE_0002);
                    userPopulator.populateUsers();
                    testServerManager.stopServer();
                    carbonHome = testServerManager.getCarbonHome();
                    String commandDirectory = carbonHome + File.separator + "bin";
                    String[] cmdArray;

                    if ((CarbonCommandToolsUtil.getCurrentOperatingSystem().contains(
                            OperatingSystems.WINDOWS.name().toLowerCase()))) {

                        cmdArray =
                                new String[]{
                                        "cmd.exe", "/c", "chpasswd.bat",
                                        "--db-url", "jdbc:h2:" + carbonHome + dataSourceBean.getUrl(),
                                        "--db-driver", dataSourceBean.getDriverClassName(), "--db-username",
                                        dataSourceBean.getUserName(), "--db-password",
                                        String.valueOf(dataSourceBean.getPassWord()), "--username",
                                        userName, "--new-password", String.valueOf(userNewPassword)};
                    } else {

                        cmdArray =
                                new String[]{
                                        "sh", "chpasswd.sh", "--db-url",
                                        "jdbc:h2:" + carbonHome + dataSourceBean.getUrl(), "--db-driver",
                                        "org.h2.Driver", "--db-username", "wso2carbon",
                                        "--db-password", String.valueOf(dataSourceBean.getPassWord()),
                                        "--username", userName, "--new-password", String.valueOf(userNewPassword)};
                    }

                    boolean scriptRunStatus =
                            CarbonCommandToolsUtil.isScriptRunSuccessfully(commandDirectory, cmdArray,
                                                                           "Password updated successfully");
                    log.info("Script running status : " + scriptRunStatus);
                    assertTrue(scriptRunStatus, "Script executed unsuccessfully");

                } catch (IOException e) {
                    throw new AutomationFrameworkException("Error when starting the carbon server",e);
                } catch (CarbonToolsIntegrationTestException e) {
                    throw new AutomationFrameworkException("Error when running the chpasswd script",e);
                } catch (XPathExpressionException e) {
                    throw new AutomationFrameworkException("Error when starting the carbon server",e);
                } catch (AutomationExtensionException e) {
                    throw new AutomationFrameworkException("Error when populating users",e);
                }
            }
        };
        testServerManager.startServer();
    }

    @Test(groups = "wso2.as", description = "H2DB password change test",
            dependsOnMethods = {"testScriptRunChangeUserPasswordH2DB"})
    public void testChangeUserPasswordH2DB() throws Exception {

        String loginStatusString = authenticatorClient.login
                (userName, String.valueOf(userNewPassword), automationContext.getInstance().getHosts().get("default"));
        assertTrue(loginStatusString.contains("JSESSIONID"), "Unsuccessful login");

    }

    @AfterClass(alwaysRun = true)
    public void serverShutDown() throws CarbonToolsIntegrationTestException {
        CarbonCommandToolsUtil.serverShutdown(portOffset, automationContext);
    }


}

