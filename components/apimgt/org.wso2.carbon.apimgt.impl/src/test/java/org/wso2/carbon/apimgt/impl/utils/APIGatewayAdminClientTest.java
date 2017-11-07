/*
 *
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.apimgt.impl.utils;

import static org.mockito.Mockito.times;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.gateway.dto.stub.APIData;
import org.wso2.carbon.apimgt.gateway.stub.APIGatewayAdminStub;
import org.wso2.carbon.apimgt.impl.dto.Environment;
import org.wso2.carbon.apimgt.impl.template.APITemplateBuilder;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ APIGatewayAdminClient.class })
public class APIGatewayAdminClientTest {

    private APIGatewayAdminStub apiGatewayAdminStub;
    private Environment environment;
    private AuthenticationAdminStub authAdminStub;
    private final String USERNAME = "username";
    private final String PASSWORD = "password";
    private final String ENV_NAME = "test-environment";
    private final String SERVER_URL = "https://localhost.com";

    @Before
    public void setup() throws Exception {
        environment = new Environment();
        environment.setName(ENV_NAME);
        environment.setPassword(PASSWORD);
        environment.setUserName(USERNAME);
        environment.setServerURL(SERVER_URL);
        apiGatewayAdminStub = Mockito.mock(APIGatewayAdminStub.class);

        Options options = new Options();
        ServiceContext serviceContext = new ServiceContext();
        OperationContext operationContext = Mockito.mock(OperationContext.class);
        serviceContext.setProperty(HTTPConstants.COOKIE_STRING, "");
        ServiceClient serviceClient = Mockito.mock(ServiceClient.class);
        authAdminStub = Mockito.mock(AuthenticationAdminStub.class);
        Mockito.doReturn(true).when(authAdminStub).login(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.when(authAdminStub._getServiceClient()).thenReturn(serviceClient);
        Mockito.when(serviceClient.getLastOperationContext()).thenReturn(operationContext);
        Mockito.when(operationContext.getServiceContext()).thenReturn(serviceContext);
        Mockito.when(apiGatewayAdminStub._getServiceClient()).thenReturn(serviceClient);
        Mockito.when(serviceClient.getOptions()).thenReturn(options);
        PowerMockito.whenNew(AuthenticationAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString()).thenReturn(authAdminStub);
    }

    @Test
    public void testAPIGatewayAdminClient() throws Exception {
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        Assert.assertNotNull(client);


    }

    @Test(expected = AxisFault.class)
    public void testAPIGatewayAdminClientException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString()).thenThrow(AxisFault.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
    }

    @Test(expected = AxisFault.class)
    public void testAPIGatewayAdminClientServerAttributeNullException()
            throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        environment.setServerURL(null);
        environment.setUserName(null);
        environment.setPassword(null);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
    }

    @Test(expected = AxisFault.class)
    public void testAPIGatewayAdminClientLoginException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(authAdminStub.login(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(LoginAuthenticationExceptionException.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
    }

    @Test(expected = AxisFault.class)
    public void testAPIGatewayAdminClientRemoteException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(authAdminStub.login(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(RemoteException.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
    }

    @Test(expected = AxisFault.class)
    public void testAPIGatewayAdminClientMalformedUrlException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        environment.setServerURL("malformed-url");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
    }

    @Test(expected = APIManagementException.class)
    public void testSetSecureVaultPropertyException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.doEncryption(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).
                thenThrow(RemoteException.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        API api = new API(identifier);
        client.setSecureVaultProperty(api, null);
    }

    @Test
    public void testSetSecureVaultProperty() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.doEncryption(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).
                thenReturn("");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        API api = new API(identifier);
        client.setSecureVaultProperty(api, null);
        Mockito.verify(apiGatewayAdminStub, times(1))
                .doEncryption(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testAddApi() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .addApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(apiGatewayAdminStub
                .addApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenReturn(true);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForTemplate(environment)).thenReturn(Mockito.anyString());
        client.addApi(apiTemplateBuilder, "", identifier);
        client.addApi(apiTemplateBuilder, null, identifier);
        client.addApi(apiTemplateBuilder, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, identifier);
        Mockito.verify(apiGatewayAdminStub, times(3))
                .addApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        client.addApi(apiTemplateBuilder, "tenant", identifier);

        Mockito.verify(apiGatewayAdminStub, times(1))
                .addApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testAddApiException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .addApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub
                .addApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenThrow(Exception.class);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForTemplate(environment)).thenReturn(Mockito.anyString());
        client.addApi(apiTemplateBuilder, "", identifier);
        client.addApi(apiTemplateBuilder, "tenant", identifier);
    }


    @Test
    public void testAddPrototypeScripImpl() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .addApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(apiGatewayAdminStub
                .addApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenReturn(true);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForPrototypeScriptAPI(environment))
                .thenReturn(Mockito.anyString());
        client.addPrototypeApiScriptImpl(apiTemplateBuilder, "", identifier);
        client.addPrototypeApiScriptImpl(apiTemplateBuilder, null, identifier);
        client.addPrototypeApiScriptImpl(apiTemplateBuilder, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, identifier);
        Mockito.verify(apiGatewayAdminStub, times(3))
                .addApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString());
        client.addPrototypeApiScriptImpl(apiTemplateBuilder, "tenant", identifier);
        Mockito.verify(apiGatewayAdminStub, times(1))
                .addApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testAddPrototypeScripImplException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .addApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub
                .addApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenThrow(Exception.class);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForPrototypeScriptAPI(environment))
                .thenReturn(Mockito.anyString());
        client.addPrototypeApiScriptImpl(apiTemplateBuilder, "", identifier);
        client.addPrototypeApiScriptImpl(apiTemplateBuilder, "tenant", identifier);
    }

    @Test
    public void testUpdateApi() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .updateApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(apiGatewayAdminStub
                .updateApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenReturn(true);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        API api = new API(identifier);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForTemplate(environment)).thenReturn(Mockito.anyString());
        client.updateApi(apiTemplateBuilder, "", identifier);
        client.updateApi(apiTemplateBuilder, null, identifier);
        client.updateApi(apiTemplateBuilder, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, identifier);
        client.updateApi(apiTemplateBuilder, "tenant", identifier);
        Mockito.verify(apiGatewayAdminStub, times(3))
                .updateApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(apiGatewayAdminStub, times(1))
                .updateApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testUpdateApiException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .updateApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub
                .updateApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenThrow(Exception.class);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForTemplate(environment)).thenReturn(Mockito.anyString());
        client.updateApi(apiTemplateBuilder, "", identifier);
        client.updateApi(apiTemplateBuilder, "tenant", identifier);
    }

    @Test
    public void testAddDefaultApi() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .addDefaultAPI(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(apiGatewayAdminStub
                .addDefaultAPIForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        API api = new API(identifier);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForDefaultAPITemplate(Mockito.anyString()))
                .thenReturn(Mockito.anyString());
        client.addDefaultAPI(apiTemplateBuilder, "", "1.0.0", identifier);
        client.addDefaultAPI(apiTemplateBuilder, null, "1.0.0", identifier);
        client.addDefaultAPI(apiTemplateBuilder, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, "1.0.0", identifier);
        Mockito.verify(apiGatewayAdminStub, times(3))
                .addDefaultAPI(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        client.addDefaultAPI(apiTemplateBuilder, "tenant", "1.0.0", identifier);
        Mockito.verify(apiGatewayAdminStub, times(1))
                .addDefaultAPIForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testAddDefaultApiException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .addDefaultAPI(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub
                .addDefaultAPIForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString())).thenThrow(Exception.class);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForDefaultAPITemplate(Mockito.anyString()))
                .thenReturn(Mockito.anyString());
        client.addDefaultAPI(apiTemplateBuilder, "", "1.0.0", identifier);
        client.addDefaultAPI(apiTemplateBuilder, "tenant", "1.0.0", identifier);
    }

    @Test
    public void testUpdateDefaultApi() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .updateDefaultApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(apiGatewayAdminStub
                .updateDefaultApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        API api = new API(identifier);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForDefaultAPITemplate(Mockito.anyString()))
                .thenReturn(Mockito.anyString());
        client.updateDefaultApi(apiTemplateBuilder, "", "1.0.0", identifier);
        client.updateDefaultApi(apiTemplateBuilder, null, "1.0.0", identifier);
        client.updateDefaultApi(apiTemplateBuilder, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, "1.0.0", identifier);
        Mockito.verify(apiGatewayAdminStub, times(3))
                .updateDefaultApi((Mockito.anyString()), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        client.updateDefaultApi(apiTemplateBuilder, "tenant", "1.0.0", identifier);

        Mockito.verify(apiGatewayAdminStub, times(1))
                .updateDefaultApiForTenant((Mockito.anyString()), (Mockito.anyString()), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testUpdateDefaultApiException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .updateDefaultApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub
                .updateDefaultApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString())).thenThrow(Exception.class);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForDefaultAPITemplate(Mockito.anyString()))
                .thenReturn(Mockito.anyString());
        client.updateDefaultApi(apiTemplateBuilder, "", "1.0.0", identifier);
        client.updateDefaultApi(apiTemplateBuilder, "tenant", "1.0.0", identifier);
    }

    @Test
    public void testUpdateApiForInlineScript() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .updateApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(apiGatewayAdminStub
                .updateApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenReturn(true);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        API api = new API(identifier);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForPrototypeScriptAPI(environment))
                .thenReturn(Mockito.anyString());
        client.updateApiForInlineScript(apiTemplateBuilder, "", identifier);
        client.updateApiForInlineScript(apiTemplateBuilder, null, identifier);
        client.updateApiForInlineScript(apiTemplateBuilder, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, identifier);
        Mockito.verify(apiGatewayAdminStub, times(3))
                .updateApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        client.updateApiForInlineScript(apiTemplateBuilder, "tenant", identifier);
        Mockito.verify(apiGatewayAdminStub, times(1))
                .updateApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testUpdateApiForInlineScriptException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub
                .updateApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub
                .updateApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenThrow(Exception.class);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APITemplateBuilder apiTemplateBuilder = Mockito.mock(APITemplateBuilder.class);
        Mockito.when(apiTemplateBuilder.getConfigStringForPrototypeScriptAPI(environment))
                .thenReturn(Mockito.anyString());
        client.updateApiForInlineScript(apiTemplateBuilder, "", identifier);
        client.updateApiForInlineScript(apiTemplateBuilder, "tenant", identifier);
    }

    @Test
    public void testDeleteApi() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.deleteApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(apiGatewayAdminStub
                .deleteApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.deleteApi("", identifier);
        client.deleteApi(null, identifier);
        client.deleteApi(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, identifier);
        Mockito.verify(apiGatewayAdminStub, times(3))
                .deleteApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        client.deleteApi("tenant", identifier);
        Mockito.verify(apiGatewayAdminStub, times(1))
                .deleteApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testDeleteApiException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.deleteApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub
                .deleteApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.deleteApi("", identifier);
        client.deleteApi("tenant", identifier);
    }

    @Test
    public void testDeleteDefaultApi() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(
                apiGatewayAdminStub.deleteDefaultApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(apiGatewayAdminStub
                .deleteDefaultApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenReturn(true);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.deleteDefaultApi("", identifier);
        client.deleteDefaultApi(null, identifier);
        client.deleteDefaultApi(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, identifier);
        Mockito.verify(apiGatewayAdminStub, times(3))
                .deleteDefaultApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        client.deleteDefaultApi("tenant", identifier);
        Mockito.verify(apiGatewayAdminStub, times(1))
                .deleteDefaultApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testDeleteDefaultApiException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(
                apiGatewayAdminStub.deleteDefaultApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub
                .deleteDefaultApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenThrow(Exception.class);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.deleteDefaultApi("", identifier);
        client.deleteDefaultApi("tenant", identifier);
    }

    @Test
    public void testAddSequence() throws Exception {
        OMElement sequence = Mockito.mock(OMElement.class);
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.addSequence(Mockito.anyString())).thenReturn(true);
        Mockito.when(apiGatewayAdminStub.
                addSequenceForTenant(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.doNothing().when(sequence).serializeAndConsume(Mockito.any(StringWriter.class));
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.addSequence(sequence, "");
        client.addSequence(sequence, null);
        client.addSequence(sequence, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        Mockito.verify(apiGatewayAdminStub, times(3)).addSequence(Mockito.anyString());
        client.addSequence(sequence, "tenant");
        Mockito.verify(apiGatewayAdminStub, times(1)).addSequenceForTenant(Mockito.anyString(), Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testAddSequenceException() throws Exception {
        OMElement sequence = Mockito.mock(OMElement.class);
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.addSequence(Mockito.anyString())).thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub.addSequenceForTenant(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.doNothing().when(sequence).serializeAndConsume(Mockito.any(StringWriter.class));
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.addSequence(sequence, "");
        client.addSequence(sequence, "tenant");
    }

    @Test
    public void testDeleteSequence() throws Exception {
        OMElement sequence = Mockito.mock(OMElement.class);
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.deleteSequence(Mockito.anyString())).thenReturn(true);
        Mockito.when(apiGatewayAdminStub.
                deleteSequenceForTenant(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.deleteSequence(sequence.getLocalName(), "");
        client.deleteSequence(sequence.getLocalName(), null);
        client.deleteSequence(sequence.getLocalName(), MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        Mockito.verify(apiGatewayAdminStub, times(3)).deleteSequence(Mockito.anyString());
        client.deleteSequence(sequence.getLocalName(), "tenant");
        Mockito.verify(apiGatewayAdminStub, times(1)).deleteSequenceForTenant(Mockito.anyString(), Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testDeleteSequenceException() throws Exception {
        OMElement sequence = Mockito.mock(OMElement.class);
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.deleteSequence(Mockito.anyString())).thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub.deleteSequenceForTenant(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.deleteSequence(sequence.getLocalName(), "");
        client.deleteSequence(sequence.getLocalName(), "tenant");
    }

    @Test
    public void testDeployPolicy() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.deployPolicy(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.deployPolicy("sample-policy", "sample-policy-file");
        Mockito.verify(apiGatewayAdminStub, times(1)).deployPolicy(Mockito.anyString(), Mockito.anyString());

    }

    @Test(expected = AxisFault.class)
    public void testDeployPolicyException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.deployPolicy(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(RemoteException.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.deployPolicy("sample-policy", "sample-policy-file");

    }

    @Test
    public void testUndeployPolicy() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.undeployPolicy(Mockito.any(String[].class))).thenReturn(true);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        String fileNames[] = { "file1", "file2", "file3" };
        client.undeployPolicy(fileNames);
        Mockito.verify(apiGatewayAdminStub, times(1)).undeployPolicy(Mockito.any(String[].class));
    }

    @Test(expected = AxisFault.class)
    public void testUndeployPolicyException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.undeployPolicy(Mockito.any(String[].class))).thenThrow(RemoteException.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        String fileNames[] = {"file1","file2","file3"};
        client.undeployPolicy(fileNames);
    }

    @Test
    public void testGetApi() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.getApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new APIData());
        Mockito.when(apiGatewayAdminStub
                .getApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new APIData());
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        client.getApi("", identifier);
        client.getApi(null, identifier);
        client.getApi(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, identifier);
        Mockito.verify(apiGatewayAdminStub, times(3))
                .getApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        client.getApi("tenant", identifier);
        Mockito.verify(apiGatewayAdminStub, times(1))
                .getApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test(expected = AxisFault.class)
    public void testGetApiException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.getApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub
                .getApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        client.getApi("", identifier);
        client.getApi(null, identifier);
        client.getApi(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, identifier);
        client.getApi("tenant", identifier);
    }

    @Test
    public void testGetDefaultApi() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.getDefaultApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new APIData());
        Mockito.when(apiGatewayAdminStub
                .getDefaultApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenReturn(new APIData());
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        client.getDefaultApi("", identifier);
        client.getDefaultApi(null, identifier);
        client.getDefaultApi(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, identifier);
        Mockito.verify(apiGatewayAdminStub, times(3))
                .getDefaultApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        client.getDefaultApi("tenant", identifier);
        Mockito.verify(apiGatewayAdminStub, times(1))
                .getDefaultApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test(expected = AxisFault.class)
    public void testGetDefaultApiException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.getDefaultApi(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(Exception.class);
        Mockito.when(apiGatewayAdminStub
                .getDefaultApiForTenant(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString())).thenThrow(Exception.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        APIIdentifier identifier = new APIIdentifier("P1_API1_v1.0.0");
        client.getDefaultApi("", identifier);
        client.getDefaultApi(null, identifier);
        client.getDefaultApi(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, identifier);
        client.getDefaultApi("tenant", identifier);
    }

    @Test
    public void testGetSequence() throws Exception {
        OMElement omElement = Mockito.mock(OMElement.class);
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.getSequence(Mockito.anyString())).thenReturn(omElement);
        Mockito.when(apiGatewayAdminStub.getSequenceForTenant(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(omElement);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.getSequence("sample-sequence", "");
        client.getSequence("sample-sequence", null);
        client.getSequence("sample-sequence", MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        client.getSequence("sample-sequence", "tenant");
        Mockito.verify(apiGatewayAdminStub, times(3)).getSequence(Mockito.anyString());
        Mockito.verify(apiGatewayAdminStub, times(1)).getSequenceForTenant(Mockito.anyString(), Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testGetSequenceException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.getSequence(Mockito.anyString())).thenThrow(RemoteException.class);
        Mockito.when(apiGatewayAdminStub.getSequenceForTenant(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(RemoteException.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.getSequence("sample-sequence", MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        client.getSequence("sample-sequence", "tenant");
    }

    @Test
    public void testIsExistingSequence() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.doReturn(true).when(apiGatewayAdminStub).isExistingSequence(Mockito.anyString());
        Mockito.doReturn(true).when(apiGatewayAdminStub)
                .isExistingSequenceForTenant(Mockito.anyString(), Mockito.anyString());
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.isExistingSequence("sample-sequence", "");
        client.isExistingSequence("sample-sequence", null);
        client.isExistingSequence("sample-sequence", MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        client.isExistingSequence("sample-sequence", "tenant");
        Mockito.verify(apiGatewayAdminStub, times(3)).isExistingSequence(Mockito.anyString());
        Mockito.verify(apiGatewayAdminStub, times(1))
                .isExistingSequenceForTenant(Mockito.anyString(), Mockito.anyString());
    }

    @Test (expected = AxisFault.class)
    public void testIsExistingSequenceException() throws Exception {
        PowerMockito.whenNew(APIGatewayAdminStub.class)
                .withArguments(Mockito.any(ConfigurationContext.class), Mockito.anyString())
                .thenReturn(apiGatewayAdminStub);
        Mockito.when(apiGatewayAdminStub.isExistingSequence(Mockito.anyString())).thenThrow(RemoteException.class);
        Mockito.when(apiGatewayAdminStub.isExistingSequenceForTenant(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(RemoteException.class);
        APIGatewayAdminClient client = new APIGatewayAdminClient(null, environment);
        client.isExistingSequence("sample-sequence", "");
        client.isExistingSequence("sample-sequence", null);
        client.isExistingSequence("sample-sequence", MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        client.isExistingSequence("sample-sequence", "tenant");
    }

}