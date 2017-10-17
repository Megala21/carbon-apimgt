/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.wso2.carbon.apimgt.keymgt.service.thrift;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.api.dto.ConditionDTO;
import org.wso2.carbon.apimgt.api.dto.ConditionGroupDTO;
import org.wso2.carbon.apimgt.api.model.AccessTokenInfo;
import org.wso2.carbon.apimgt.api.model.KeyManager;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.factory.KeyManagerHolder;
import org.wso2.carbon.apimgt.impl.generated.thrift.APIKeyValidationInfoDTO;
import org.wso2.carbon.apimgt.impl.generated.thrift.APIKeyValidationService;
import org.wso2.carbon.apimgt.impl.generated.thrift.APIManagementException;
import org.wso2.carbon.apimgt.impl.generated.thrift.URITemplate;
import org.wso2.carbon.apimgt.keymgt.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.keymgt.util.APIKeyMgtUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.thrift.authentication.ThriftAuthenticatorService;
import org.wso2.carbon.metrics.manager.MetricManager;
import org.wso2.carbon.metrics.manager.MetricService;
import org.wso2.carbon.metrics.manager.Timer;
import org.wso2.carbon.utils.ThriftSession;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.base.CarbonBaseConstants.CARBON_HOME;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ThriftAuthenticatorService.class, ServiceReferenceHolder.class, ApiMgtDAO.class,
        PrivilegedCarbonContext.class, org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.class,
        Timer.class, MetricManager.class, APIKeyMgtUtil.class, KeyManagerHolder.class })
public class APIKeyValidationServiceImplTest {
    private ServiceReferenceHolder serviceReferenceHolder;
    private ApiMgtDAO apiMgtDAO = Mockito.mock(ApiMgtDAO.class);
    private APIManagerConfiguration apiManagerConfiguration;
    private APIManagerConfigurationService apiManagerConfigurationService;
    private APIKeyValidationServiceImpl apiKeyValidationServiceImpl;
    private final String TENANT_DOMAIN = "carbon.super";
    private final int TENANT_ID = -1234;
    private final String CONTEXT = "context";
    private final String VERSION = "1.0.0";
    private final String ACCESS_TOKEN = "1z2x3c4v5b6b7n8m9";
    private final String SESSION_ID = "a1b2c3d4e5f6g7";
    private final String REQUIRED_AUTHENTICATION_LEVEL = "level";
    private final String ALLOWED_DOMAINS = "wso2.com";
    private final String MATCHING_RESOURCE = "/*";
    private final String HTTP_VERB = "GET";

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ApiMgtDAO.class);
        serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        apiManagerConfigurationService = Mockito.mock(APIManagerConfigurationService.class);

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_KEY_MANGER_VALIDATIONHANDLER_CLASS_NAME))
                .thenReturn("org.wso2.carbon.apimgt.keymgt.handlers.DefaultKeyValidationHandler");
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_FORCE_CI_COMPARISIONS))
                .thenReturn("false");
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(ApiMgtDAO.getInstance()).thenReturn(apiMgtDAO);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        apiKeyValidationServiceImpl = new APIKeyValidationServiceImpl();
    }

    @Test
    public void testValidateKeyTest() throws Exception {

        APIKeyValidationInfoDTO dto;

        try {
            dto = apiKeyValidationServiceImpl
                    .validateKey(CONTEXT, VERSION, ACCESS_TOKEN, SESSION_ID, REQUIRED_AUTHENTICATION_LEVEL,
                            ALLOWED_DOMAINS, MATCHING_RESOURCE, HTTP_VERB);
            Assert.fail("APIKeyMgtException should be expected");
        } catch (org.wso2.carbon.apimgt.impl.generated.thrift.APIKeyMgtException e) {
            //Exception ignored
        }

        try {
            ThriftAuthenticatorService thriftAuthenticatorService = Mockito.mock(ThriftAuthenticatorService.class);
            APIKeyValidationServiceImpl.init(thriftAuthenticatorService);
            dto = apiKeyValidationServiceImpl
                    .validateKey(CONTEXT, VERSION, ACCESS_TOKEN, SESSION_ID, REQUIRED_AUTHENTICATION_LEVEL,
                            ALLOWED_DOMAINS, MATCHING_RESOURCE, HTTP_VERB);
            Assert.fail("APIKeyMgtException should be expected");
        } catch (org.wso2.carbon.apimgt.impl.generated.thrift.APIKeyMgtException e) {
            //Exception ignored
        }

        ThriftAuthenticatorService thriftAuthenticatorService = Mockito.mock(ThriftAuthenticatorService.class);
        Mockito.when(thriftAuthenticatorService.isAuthenticated(SESSION_ID)).thenReturn(true);
        ThriftSession currentSession = new ThriftSession();
        Mockito.when(thriftAuthenticatorService.getSessionInfo(SESSION_ID)).thenReturn(currentSession);
        APIKeyValidationServiceImpl.init(thriftAuthenticatorService);

        try {
            dto = apiKeyValidationServiceImpl
                    .validateKey(CONTEXT, VERSION, ACCESS_TOKEN, SESSION_ID, REQUIRED_AUTHENTICATION_LEVEL,
                            ALLOWED_DOMAINS, MATCHING_RESOURCE, HTTP_VERB);
            Assert.fail("APIKeyMgtException should be expected");
        } catch (org.wso2.carbon.apimgt.impl.generated.thrift.APIKeyMgtException e) {
            //Exception ignored
        }

        System.setProperty(CARBON_HOME, "");
        PrivilegedCarbonContext privilegedCarbonContext = Mockito.mock(PrivilegedCarbonContext.class);
        PowerMockito.mockStatic(PrivilegedCarbonContext.class);
        PowerMockito.when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);
        PowerMockito.when(privilegedCarbonContext.getTenantDomain()).thenReturn(TENANT_DOMAIN);
        currentSession.setAttribute(MultitenantConstants.TENANT_ID, TENANT_ID);

        org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder serviceReferenceHolder1 = Mockito
                .mock(org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.class);
        PowerMockito.mockStatic(org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.class);
        MetricService metricService = Mockito.mock(MetricService.class);
        Timer timer = Mockito.mock(Timer.class);
        Mockito.when(org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.getInstance())
                .thenReturn(serviceReferenceHolder1);
        Mockito.when(serviceReferenceHolder1.getMetricService()).thenReturn(metricService);

        Timer.Context timerContext = Mockito.mock(Timer.Context.class);
        Mockito.when(timer.start()).thenReturn(timerContext);

        Mockito.when(metricService.timer(org.wso2.carbon.metrics.manager.Level.INFO, MetricManager
                .name(APIConstants.METRICS_PREFIX, APIKeyValidationService.class.getSimpleName(), "VALIDATE_MAIN")))
                .thenReturn(timer);
        Mockito.when(metricService.timer(org.wso2.carbon.metrics.manager.Level.INFO, MetricManager
                .name(APIConstants.METRICS_PREFIX, APIKeyValidationService.class.getSimpleName(), "VALIDATE_TOKEN")))
                .thenReturn(timer);
        Mockito.when(metricService.timer(org.wso2.carbon.metrics.manager.Level.INFO, MetricManager
                .name(APIConstants.METRICS_PREFIX, APIKeyValidationService.class.getSimpleName(),
                        "VALIDATE_SUBSCRIPTION"))).thenReturn(timer);
        Mockito.when(metricService.timer(org.wso2.carbon.metrics.manager.Level.INFO, MetricManager
                .name(APIConstants.METRICS_PREFIX, APIKeyValidationService.class.getSimpleName(), "VALIDATE_SCOPES")))
                .thenReturn(timer);
        Mockito.when(metricService.timer(org.wso2.carbon.metrics.manager.Level.INFO, MetricManager
                .name(APIConstants.METRICS_PREFIX, APIKeyValidationService.class.getSimpleName(), "GENERATE_JWT")))
                .thenReturn(timer);

        PowerMockito.mockStatic(APIKeyMgtUtil.class);
        PowerMockito.mockStatic(KeyManagerHolder.class);

        KeyManager keyManager = Mockito.mock(KeyManager.class);
        PowerMockito.when(KeyManagerHolder.getKeyManagerInstance()).thenReturn(keyManager);
        AccessTokenInfo tokenInfo = new AccessTokenInfo();
        PowerMockito.when(keyManager.getTokenMetaData(ACCESS_TOKEN)).thenReturn(tokenInfo);

        dto = apiKeyValidationServiceImpl
                .validateKey(CONTEXT, VERSION, ACCESS_TOKEN, SESSION_ID, REQUIRED_AUTHENTICATION_LEVEL, ALLOWED_DOMAINS,
                        MATCHING_RESOURCE, HTTP_VERB);
        Assert.assertNotNull("APIKeyValidationInfoDTO should not be null", dto);
    }

    @Test
    public void testGetAllURITemplatesTest() throws Exception {
        List<URITemplate> uriTemplateList;

        org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder serviceReferenceHolder1 = Mockito
                .mock(org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.class);
        PowerMockito.mockStatic(org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.class);
        MetricService metricService = Mockito.mock(MetricService.class);
        Timer timer = Mockito.mock(Timer.class);
        Mockito.when(org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.getInstance())
                .thenReturn(serviceReferenceHolder1);
        Mockito.when(serviceReferenceHolder1.getMetricService()).thenReturn(metricService);

        Timer.Context timerContext = Mockito.mock(Timer.Context.class);
        Mockito.when(timer.start()).thenReturn(timerContext);

        Mockito.when(metricService.timer(org.wso2.carbon.metrics.manager.Level.INFO, MetricManager
                .name(APIConstants.METRICS_PREFIX, APIKeyValidationService.class.getSimpleName(), "GET_URI_TEMPLATE")))
                .thenReturn(timer);
        ArrayList<org.wso2.carbon.apimgt.api.model.URITemplate> uriTemplates1 = new ArrayList<org.wso2.carbon.apimgt.api.model.URITemplate>();
        uriTemplates1.add(new org.wso2.carbon.apimgt.api.model.URITemplate());

        PowerMockito.when(ApiMgtDAO.getInstance().getAllURITemplates(CONTEXT, VERSION)).thenReturn(uriTemplates1);
        uriTemplateList = apiKeyValidationServiceImpl.getAllURITemplates(CONTEXT, VERSION, SESSION_ID);
        Assert.assertEquals(1, uriTemplateList.size());

        org.wso2.carbon.apimgt.api.model.URITemplate uriTemplate2 = new org.wso2.carbon.apimgt.api.model.URITemplate();
        ConditionGroupDTO[] conditionGroups = new ConditionGroupDTO[1];
        conditionGroups[0] = new ConditionGroupDTO();
        conditionGroups[0].setConditionGroupId("12345");
        ConditionDTO[] conditionDTOS = new ConditionDTO[1];
        conditionDTOS[0] = new ConditionDTO();
        conditionGroups[0].setConditions(conditionDTOS);

        uriTemplate2.setConditionGroups(conditionGroups);
        uriTemplates1.add(uriTemplate2);
        PowerMockito.when(ApiMgtDAO.getInstance().getAllURITemplates(CONTEXT, VERSION)).thenReturn(uriTemplates1);
        uriTemplateList = apiKeyValidationServiceImpl.getAllURITemplates(CONTEXT, VERSION, SESSION_ID);
        Assert.assertEquals(2, uriTemplateList.size());

        PowerMockito.when(ApiMgtDAO.getInstance().getAllURITemplates(CONTEXT, VERSION)).thenThrow(
                new org.wso2.carbon.apimgt.api.APIManagementException("Error while fetching all URL Templates"));
        try {
            apiKeyValidationServiceImpl.getAllURITemplates(CONTEXT, VERSION, SESSION_ID);
            Assert.fail("APIManagementException should be expected");
        } catch (APIManagementException e) {
            //Exception ignored
        }
        APIKeyValidationServiceImpl.init(null);
        try {
            apiKeyValidationServiceImpl.getAllURITemplates(CONTEXT, VERSION, SESSION_ID);
            Assert.fail("APIKeyMgtException should be expected");
        } catch (org.wso2.carbon.apimgt.impl.generated.thrift.APIKeyMgtException e) {
            //Exception ignored
        }

        ThriftAuthenticatorService thriftAuthenticatorService = Mockito.mock(ThriftAuthenticatorService.class);
        Mockito.when(thriftAuthenticatorService.isAuthenticated(SESSION_ID)).thenReturn(false);
        APIKeyValidationServiceImpl.init(thriftAuthenticatorService);
        try {
            apiKeyValidationServiceImpl.getAllURITemplates(CONTEXT, VERSION, SESSION_ID);
            Assert.fail("APIKeyMgtException should be expected");
        } catch (org.wso2.carbon.apimgt.impl.generated.thrift.APIKeyMgtException e) {
            //Exception ignored
        }
    }

    @Test
    public void testValidateKeyForHandshake() throws Exception {
        ThriftAuthenticatorService thriftAuthenticatorService = Mockito.mock(ThriftAuthenticatorService.class);
        Mockito.when(thriftAuthenticatorService.isAuthenticated(SESSION_ID)).thenReturn(false);
        APIKeyValidationServiceImpl.init(thriftAuthenticatorService);

        try {
            apiKeyValidationServiceImpl.validateKeyforHandshake(CONTEXT, VERSION, ACCESS_TOKEN, SESSION_ID);
            Assert.fail("APIKeyMgtException should be expected");
        } catch (org.wso2.carbon.apimgt.impl.generated.thrift.APIKeyMgtException e) {
            //Exception ignored
        }

        APIKeyValidationServiceImpl.init(null);
        try {
            apiKeyValidationServiceImpl.validateKeyforHandshake(CONTEXT, VERSION, ACCESS_TOKEN, SESSION_ID);
            Assert.fail("APIKeyMgtException should be expected");
        } catch (org.wso2.carbon.apimgt.impl.generated.thrift.APIKeyMgtException e) {
            //Exception ignored
        }

        thriftAuthenticatorService = Mockito.mock(ThriftAuthenticatorService.class);
        Mockito.when(thriftAuthenticatorService.isAuthenticated(SESSION_ID)).thenReturn(true);
        APIKeyValidationServiceImpl.init(thriftAuthenticatorService);

        try {
            apiKeyValidationServiceImpl.validateKeyforHandshake(CONTEXT, VERSION, ACCESS_TOKEN, SESSION_ID);
            Assert.fail("APIKeyMgtException should be expected");
        } catch (org.wso2.carbon.apimgt.impl.generated.thrift.APIKeyMgtException e) {
            //Exception ignored
        }

        System.setProperty(CARBON_HOME, "");
        PrivilegedCarbonContext privilegedCarbonContext = Mockito.mock(PrivilegedCarbonContext.class);
        PowerMockito.mockStatic(PrivilegedCarbonContext.class);
        PowerMockito.when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);
        PowerMockito.when(privilegedCarbonContext.getTenantDomain()).thenReturn(TENANT_DOMAIN);

        thriftAuthenticatorService = Mockito.mock(ThriftAuthenticatorService.class);
        Mockito.when(thriftAuthenticatorService.isAuthenticated(SESSION_ID)).thenReturn(true);
        ThriftSession currentSession = new ThriftSession();
        currentSession.setAttribute(MultitenantConstants.TENANT_ID, TENANT_ID);
        Mockito.when(thriftAuthenticatorService.getSessionInfo(SESSION_ID)).thenReturn(currentSession);
        APIKeyValidationServiceImpl.init(thriftAuthenticatorService);

        PowerMockito.mockStatic(APIKeyMgtUtil.class);
        PowerMockito.mockStatic(KeyManagerHolder.class);

        KeyManager keyManager = Mockito.mock(KeyManager.class);
        PowerMockito.when(KeyManagerHolder.getKeyManagerInstance()).thenReturn(keyManager);
        AccessTokenInfo tokenInfo = new AccessTokenInfo();
        PowerMockito.when(keyManager.getTokenMetaData(ACCESS_TOKEN)).thenReturn(tokenInfo);

        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = apiKeyValidationServiceImpl
                .validateKeyforHandshake(CONTEXT, VERSION, ACCESS_TOKEN, SESSION_ID);
        Assert.assertNotNull(apiKeyValidationInfoDTO);
    }
}
