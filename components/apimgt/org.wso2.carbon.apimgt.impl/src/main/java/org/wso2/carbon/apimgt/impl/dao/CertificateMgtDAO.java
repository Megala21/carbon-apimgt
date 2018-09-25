/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.impl.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.dto.CertificateMetadataDTO;
import org.wso2.carbon.apimgt.api.dto.ClientCertificateDTO;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.certificatemgt.exceptions.CertificateAliasExistsException;
import org.wso2.carbon.apimgt.impl.certificatemgt.exceptions.CertificateManagementException;
import org.wso2.carbon.apimgt.impl.dao.constants.SQLConstants;
import org.wso2.carbon.apimgt.impl.dto.CertificateTierDTO;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.utils.CertificateMgtUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles data base transactions related to the server certificate management.
 * Here the certificate information will be,
 * Alias
 * Server Endpoint
 * Tenant Id
 * The alias and server ep has one-to-one mapping, which there could be one certificate for a particular endpoint.
 * A tenant could have multiple alias-ep entries.
 */
public class CertificateMgtDAO {

    private static final String CERTIFICATE_TABLE_NAME = "AM_CERTIFICATE_METADATA";
    private static Log log = LogFactory.getLog(CertificateMgtDAO.class);
    private static CertificateMgtDAO certificateMgtDAO = null;
    private static boolean initialAutoCommit = false;
    private CertificateMgtUtils certificateMgtUtils;

    /**
     * Private constructor
     */
    private CertificateMgtDAO() {
        certificateMgtUtils = new CertificateMgtUtils();
    }

    /**
     * Returns an instance of CertificateMgtDao.
     */
    public static synchronized CertificateMgtDAO getInstance() {

        if (certificateMgtDAO == null) {
            certificateMgtDAO = new CertificateMgtDAO();
        }
        return certificateMgtDAO;
    }

    /**
     * Checks whether the certificate management table exists in the data base.
     *
     * @return : True if exists, false otherwise.
     */
    public boolean isTableExists() throws CertificateManagementException {

        boolean isExists = false;
        Connection connection = null;
        ResultSet resultSet = null;
        DatabaseMetaData databaseMetaData = null;

        try {
            connection = APIMgtDBUtil.getConnection();
            databaseMetaData = connection.getMetaData();

            resultSet = databaseMetaData.getTables(null, null,
                    CERTIFICATE_TABLE_NAME, null);
            if (resultSet.next()) {
                isExists = true;
            }
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error while retrieving database information. ", e);
            }
            handleException("Error retrieving Database information", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(null, connection, resultSet);
        }
        return isExists;
    }

    /**
     * To check whether new table which is used to store client certificates exist.
     *
     * @return true if the table exists, otherwise false.
     */
    public boolean isClientCertificateTableExists() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        boolean isExists = false;
        try {
            connection = APIMgtDBUtil.getConnection();
            preparedStatement = connection.prepareStatement(SQLConstants.ClientCertificateConstants.VERIFY_TABLE);
            isExists = preparedStatement.execute();
        } catch (SQLException e) {
            log.warn("Relevant table related with storing client certificates does not exist :" + e.getMessage());
        } finally {
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, null);
        }
        return isExists;
    }

    /**
     * Method to add a new client certificate to the database.
     *
     * @param certificate   : Client certificate that need to be added.
     * @param apiIdentifier : API which the client certificate is uploaded against.
     * @param alias         : Alias for the new certificate.
     * @param tenantId      : The Id of the tenant who uploaded the certificate.
     * @return : True if the information is added successfully, false otherwise.
     * @throws CertificateManagementException if existing entry is found for the given endpoint or alias.
     */
    public boolean addClientCertificate(String certificate, APIIdentifier apiIdentifier, String alias,
            String tierName, int tenantId) throws CertificateManagementException {

        boolean result = false;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String addCertQuery = SQLConstants.ClientCertificateConstants.INSERT_CERTIFICATE;

        try {
            connection = APIMgtDBUtil.getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            int apiId = ApiMgtDAO.getInstance().getAPIID(apiIdentifier, connection);
            preparedStatement = connection.prepareStatement(addCertQuery);
            preparedStatement.setBlob(1, getInputStream(certificate));
            preparedStatement.setInt(2, tenantId);
            preparedStatement.setString(3, alias + "_" + tenantId);
            preparedStatement.setInt(4, apiId);
            preparedStatement.setString(5, certificateMgtUtils.getUniqueIdentifierOfCertificate(certificate));
            preparedStatement.setString(6, tierName);
            result = preparedStatement.executeUpdate() == 1;
            connection.commit();
        } catch (SQLException e) {
            handleConnectionRollBack(connection);
            if (log.isDebugEnabled()) {
                log.debug("Error occurred while adding client certificate details to database for the API "
                        + apiIdentifier.toString(), e);
            }
            handleException("Error while persisting client certificate for the API " + apiIdentifier.toString(), e);
        } catch (APIManagementException e) {
            handleException("Error getting API details of the API " + apiIdentifier.toString() + " when storing "
                    + "client certificate", e);
        } finally {
            APIMgtDBUtil.setAutoCommit(connection, initialAutoCommit);
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, null);
        }
        return result;
    }

    /**
     * To get the input stream from string.
     *
     * @param value Relevant string that need to be converted to input stream.
     * @return input stream.
     */
    private InputStream getInputStream(String value) {
        byte[] cert = value.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(cert);
    }

    /**
     * This method handles the connection roll back.
     *
     * @param connection Relevant database connection that need to be rolled back.
     */
    private void handleConnectionRollBack(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            } else {
                log.warn("Could not perform rollback since the connection is null.");
            }
        } catch (SQLException e1) {
            log.error("Error while rolling back the transaction.", e1);
        }
    }

    /**
     * Method to add a new certificate to the database.
     *
     * @param alias    : Alias for the new certificate.
     * @param endpoint : The endpoint/ server url which the certificate will be mapped to.
     * @param tenantId : The Id of the tenant who uploaded the certificate.
     * @return : True if the information is added successfully, false otherwise.
     * @throws CertificateManagementException if existing entry is found for the given endpoint or alias.
     */
    public boolean addCertificate(String alias, String endpoint, int tenantId) throws CertificateManagementException,
            CertificateAliasExistsException {

        boolean result = false;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String addCertQuery = SQLConstants.CertificateConstants.INSERT_CERTIFICATE;

        //Check whether any certificate is uploaded for the same alias or endpoint by another user/ tenant.
        CertificateMetadataDTO existingCertificate = getCertificate(alias);

        if (existingCertificate != null) {
            if (log.isDebugEnabled()) {
                log.debug("A certificate for the endpoint " + endpoint + " has already added with alias " +
                        existingCertificate.getAlias());
            }
            String message = "Alias or Endpoint exists in the database!";
            if (existingCertificate.getAlias().equals(alias)) {
                throw new CertificateAliasExistsException(message);
            }
        }

        try {
            connection = APIMgtDBUtil.getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(addCertQuery);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.setString(2, endpoint);
            preparedStatement.setString(3, alias);
            result = preparedStatement.executeUpdate() == 1;
            connection.commit();
        } catch (SQLException e) {
            handleConnectionRollBack(connection);
            if (log.isDebugEnabled()) {
                log.debug("Error occurred while adding certificate metadata to database.", e);
            }
            handleException("Error while persisting certificate metadata.", e);
        } finally {
            APIMgtDBUtil.setAutoCommit(connection, initialAutoCommit);
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, null);
        }
        return result;
    }

    /**
     * Method to retrieve certificate metadata from db for specific alias or endpoint.
     * From alias and endpoint, only one parameter is required. This will be used to query all the certificates
     * without a limitation for tenant.
     * Addresses : If some tenant is trying to add a certificate with the same alias, proper error should be shown in
     * the UI.
     *
     * @param alias : Alias for the certificate. (Optional)
     * @return : A CertificateMetadataDTO object if the certificate is retrieved successfully, null otherwise.
     */
    private CertificateMetadataDTO getCertificate(String alias) throws CertificateManagementException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        CertificateMetadataDTO certificateMetadataDTO = null;
        String getCertQuery = SQLConstants.CertificateConstants.GET_CERTIFICATE_ALL_TENANTS;

        try {
            connection = APIMgtDBUtil.getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            connection.commit();
            preparedStatement = connection.prepareStatement(getCertQuery);
            preparedStatement.setString(1, alias);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                certificateMetadataDTO = new CertificateMetadataDTO();
                certificateMetadataDTO.setAlias(resultSet.getString("ALIAS"));
                certificateMetadataDTO.setEndpoint(resultSet.getString("END_POINT"));
            }
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error while retrieving certificate metadata for alias " + alias);
            }
            handleException("Error while retrieving certificate metadata for alias " + alias, e);
        } finally {
            APIMgtDBUtil.setAutoCommit(connection, initialAutoCommit);
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }
        return certificateMetadataDTO;
    }

    /**
     * Method to retrieve certificate metadata from db for specific tenant which matches alias or endpoint.
     * From alias and endpoint, only one parameter is required.
     *
     * @param tenantId : The id of the tenant which the certificate belongs to.
     * @param alias    : Alias for the certificate. (Optional)
     * @param endpoint : The endpoint/ server url which the certificate is mapped to. (Optional)
     * @return : A CertificateMetadataDTO object if the certificate is retrieved successfully, null otherwise.
     */
    public List<CertificateMetadataDTO> getCertificates(String alias, String endpoint, int tenantId)
            throws CertificateManagementException {

        Connection connection = null;
        String getCertQuery = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        CertificateMetadataDTO certificateMetadataDTO;
        List<CertificateMetadataDTO> certificateMetadataList = new ArrayList<>();

        if (StringUtils.isNotEmpty(alias) || StringUtils.isNotEmpty(endpoint)) {
            if (log.isDebugEnabled()) {
                log.debug("The alias and endpoint are not empty. Invoking the search query with parameters " +
                        "alias = " + alias + " endpoint = " + endpoint);
            }
            getCertQuery = SQLConstants.CertificateConstants.GET_CERTIFICATE_TENANT;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("The alias and endpoint are empty. Invoking the get all certificates for tenant " +
                        tenantId);
            }
            getCertQuery = SQLConstants.CertificateConstants.GET_CERTIFICATES;
        }

        try {
            connection = APIMgtDBUtil.getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            connection.commit();
            preparedStatement = connection.prepareStatement(getCertQuery);
            preparedStatement.setInt(1, tenantId);

            if (StringUtils.isNotEmpty(alias) || StringUtils.isNotEmpty(endpoint)) {
                preparedStatement.setString(2, alias);
                preparedStatement.setString(3, endpoint);
            }
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                certificateMetadataDTO = new CertificateMetadataDTO();
                certificateMetadataDTO.setAlias(resultSet.getString("ALIAS"));
                certificateMetadataDTO.setEndpoint(resultSet.getString("END_POINT"));
                certificateMetadataList.add(certificateMetadataDTO);
            }
        } catch (SQLException e) {
            handleException("Error while retrieving certificate metadata.", e);
        } finally {
            APIMgtDBUtil.setAutoCommit(connection, initialAutoCommit);
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }
        return certificateMetadataList;
    }

    /**
     * To remove the entries of updated certificates from gateway.
     *
     * @param apiIdentifier Identifier of the API.
     * @param tenantId      ID of the tenant.
     * @throws CertificateManagementException Certificate Management Exception.
     */
    public void updateRemovedCertificatesFromGateways(APIIdentifier apiIdentifier, int tenantId)
            throws CertificateManagementException {
        Connection connection = null;
        String getCertQuery = SQLConstants.ClientCertificateConstants.DELETE_CERTIFICATES_FOR_API;
        PreparedStatement preparedStatement = null;

        try {
            connection = APIMgtDBUtil.getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            int apiId = ApiMgtDAO.getInstance().getAPIID(apiIdentifier, connection);
            preparedStatement = connection.prepareStatement(getCertQuery);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.setInt(2, apiId);
            preparedStatement.setBoolean(3, true);
            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            handleConnectionRollBack(connection);
            handleException(
                    "SQL exception while updating removed certificates from gateway for the api " + apiIdentifier
                            .toString(), e);
        } catch (APIManagementException e) {
            handleConnectionRollBack(connection);
            handleException("API management exception while updating removed certificates from gateway for the api "
                    + apiIdentifier.toString(), e);
        } finally {
            APIMgtDBUtil.setAutoCommit(connection, initialAutoCommit);
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, null);
        }
    }

    /**
     * To get the set of client certificates tht are removed from publisher.
     *
     * @param apiIdentifier Identifier of the API.
     * @param tenantId      ID of the tenant.
     * @return set of client certificates.
     * @throws CertificateManagementException Certificate Management Exception.
     */
    public List<ClientCertificateDTO> getDeletedClientCertificates(APIIdentifier apiIdentifier, int tenantId)
            throws CertificateManagementException {

        Connection connection = null;
        String getCertQuery = SQLConstants.ClientCertificateConstants.GET_CERTIFICATES_FOR_API;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<ClientCertificateDTO> clientCertificateDTOList = new ArrayList<>();

        try {
            connection = APIMgtDBUtil.getConnection();
            int apiId = ApiMgtDAO.getInstance().getAPIID(apiIdentifier, connection);
            preparedStatement = connection.prepareStatement(getCertQuery);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.setInt(2, apiId);
            preparedStatement.setBoolean(3, true);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Blob blob = resultSet.getBlob("CERTIFICATE");
                ClientCertificateDTO clientCertificateDTO = new ClientCertificateDTO();
                clientCertificateDTO.setAlias(resultSet.getString("ALIAS"));
                clientCertificateDTO.setCertificate(new String(blob.getBytes(1L, (int) blob.length())));
                clientCertificateDTOList.add(clientCertificateDTO);
            }
        } catch (SQLException e) {
            handleException(
                    "SQL exception while retrieving deleted client certificate details for the API " + apiIdentifier
                            .toString(), e);
        } catch (APIManagementException e) {
            handleException(
                    "API Management exception while retrieving deleted client certificate details " + "for the API "
                            + apiIdentifier.toString(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }
        return clientCertificateDTOList;
    }

    /**
     * To get set of active client certificates for the API.
     *
     * @param apiIdentifier API Identifier.
     * @param tenantId      ID of the tenant.
     * @return list of active client certificates.
     * @throws CertificateManagementException Certificate Management Exception.
     */
    public List<ClientCertificateDTO> getClientCertificates(APIIdentifier apiIdentifier, int tenantId)
            throws CertificateManagementException {

        Connection connection = null;
        String getCertQuery = SQLConstants.ClientCertificateConstants.GET_CERTIFICATES_FOR_API;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<ClientCertificateDTO> clientCertificateDTOList = new ArrayList<>();

        try {
            connection = APIMgtDBUtil.getConnection();
            int apiId = ApiMgtDAO.getInstance().getAPIID(apiIdentifier, connection);
            preparedStatement = connection.prepareStatement(getCertQuery);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.setInt(2, apiId);
            preparedStatement.setBoolean(3, false);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Blob blob = resultSet.getBlob("CERTIFICATE");
                ClientCertificateDTO clientCertificateDTO = new ClientCertificateDTO();
                String alias = resultSet.getString("ALIAS");
                clientCertificateDTO.setAlias(resultSet.getString("ALIAS").substring(0, alias.lastIndexOf("_")));
                clientCertificateDTO.setCertificate(new String(blob.getBytes(1L, (int) blob.length())));
                clientCertificateDTO.setUniqueId(resultSet.getString("UNIQUE_ID"));
                clientCertificateDTO.setTierName(resultSet.getString("TIER_NAME"));
                clientCertificateDTOList.add(clientCertificateDTO);
            }
        } catch (SQLException e) {
            handleException(
                    "SQL exception while retrieving active client certificate metadata for the API, " + apiIdentifier
                            .toString(), e);
        } catch (APIManagementException e) {
            handleException("API Management exception while retrieving active client certificate metadata for the API, "
                    + apiIdentifier.toString(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }
        return clientCertificateDTOList;
    }

    /**
     * Method to delete a certificate from the database.
     *
     * @param alias    : Alias for the certificate.
     * @param endpoint : The endpoint/ server url which the certificate is mapped to.
     * @param tenantId : The Id of the tenant who owns the certificate.
     * @return : true if certificate deletion is successful, false otherwise.
     */
    public boolean deleteCertificate(String alias, String endpoint, int tenantId)
            throws CertificateManagementException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        boolean result = false;
        String deleteCertQuery = SQLConstants.CertificateConstants.DELETE_CERTIFICATES;

        try {
            connection = APIMgtDBUtil.getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(deleteCertQuery);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.setString(2, alias);
            result = preparedStatement.executeUpdate() == 1;
            connection.commit();
            connection.setAutoCommit(initialAutoCommit);
        } catch (SQLException e) {
            handleConnectionRollBack(connection);
            handleException("Error while deleting certificate metadata. ", e);
        } finally {
            APIMgtDBUtil.closeStatement(preparedStatement);
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, null);
        }
        return result;
    }

    /**
     * Method to delete client certificate from the database.
     *
     * @param apiIdentifier : Identifier of the API.
     * @param alias         : Alias for the certificate.
     * @param tenantId      : The Id of the tenant who owns the certificate.
     * @return : true if certificate deletion is successful, false otherwise.
     */
    public boolean deleteClientCertificate(APIIdentifier apiIdentifier, String alias, int tenantId)
            throws CertificateManagementException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        boolean result = false;
        String deleteCertQuery = SQLConstants.ClientCertificateConstants.PRE_DELETE_CERTIFICATES;

        try {
            connection = APIMgtDBUtil.getConnection();
            initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            int apiId = ApiMgtDAO.getInstance().getAPIID(apiIdentifier, connection);
            /* If an entry exists already with "Removed" true, remove that particular entry and update the current
             entry with removed true */
            preparedStatement = connection.prepareStatement(deleteCertQuery);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.setInt(2, apiId);
            preparedStatement.setBoolean(3, true);
            preparedStatement.setString(4, alias + "_" + tenantId);
            preparedStatement.executeUpdate();

            deleteCertQuery = SQLConstants.ClientCertificateConstants.DELETE_CERTIFICATES;
            preparedStatement = connection.prepareStatement(deleteCertQuery);
            preparedStatement.setBoolean(1, true);
            preparedStatement.setInt(2, tenantId);
            preparedStatement.setString(3, alias + "_" + tenantId);
            preparedStatement.setInt(4, apiId);
            result = preparedStatement.executeUpdate() == 1;
            connection.commit();
        } catch (SQLException e) {
            handleConnectionRollBack(connection);
            handleException("Database exception while deleting client certificate metadata for the api " + apiIdentifier
                    .toString(), e);
        } catch (APIManagementException e) {
            handleConnectionRollBack(connection);
            handleException(
                    "API Management exception while trying deleting certificate metadata with the alias " + alias
                            + " for the API " + apiIdentifier.toString(), e);
        } finally {
            APIMgtDBUtil.setAutoCommit(connection, initialAutoCommit);
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, null);
        }
        return result;
    }

    /**
     * To get the subscription tier information for the relevant certificate
     *
     * @param apiIdentifier         Relevant API Identifier.
     * @param certificateIdentifier Identifier of the certificate
     * @return subscription tier information of the certificate.
     * @throws CertificateManagementException Certificate Manegement Exception.
     */
    public CertificateTierDTO getCertificateSubscriptionTierInformation(APIIdentifier apiIdentifier,
            String certificateIdentifier) throws CertificateManagementException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String deleteCertQuery = SQLConstants.ClientCertificateConstants.SELECT_CERTIFICATE_TIER;

        if (APIUtil.isAdvanceThrottlingEnabled()) {
            deleteCertQuery = SQLConstants.ClientCertificateConstants.SELECT_CERTIFICATE_TIER_ADVANCED;
        }
        CertificateTierDTO certificateTierDTO = new CertificateTierDTO();

        try {
            connection = APIMgtDBUtil.getConnection();
            int apiId = ApiMgtDAO.getInstance().getAPIID(apiIdentifier, connection);
            preparedStatement = connection.prepareStatement(deleteCertQuery);
            preparedStatement.setInt(1, apiId);
            preparedStatement.setString(2, certificateIdentifier);
            resultSet = preparedStatement.executeQuery();

            boolean isResultExist = resultSet.next();
            if (isResultExist) {
                certificateTierDTO.setTier(resultSet.getString("TIER_NAME"));
                if (APIUtil.isAdvanceThrottlingEnabled()) {
                    certificateTierDTO.setSpikeArrestLimit(resultSet.getInt("RATE_LIMIT_COUNT"));
                    certificateTierDTO.setSpikeArrestUnit(resultSet.getString("RATE_LIMIT_TIME_UNIT"));
                    certificateTierDTO.setStopOnQuotaReach(resultSet.getBoolean("STOP_ON_QUOTA_REACH"));
                }
            }
        } catch (SQLException e) {
            handleException("Error while getting certificate level tier details for " + apiIdentifier.toString(), e);
        } catch (APIManagementException e) {
            handleException("API Management exception while getting certificate level tier details for " + apiIdentifier
                    .toString(), e);
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }
        return certificateTierDTO;
    }

    /**
     * Retrieve the number of total certificates which a tenant has uploaded.
     *
     * @param tenantId : The id of the tenant.
     * @return : The total certificate count of the tenant.
     * @throws CertificateManagementException :
     */
    public int getCertificateCount(int tenantId) throws CertificateManagementException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String certificateCountQuery = SQLConstants.CertificateConstants.CERTIFICATE_COUNT_QUERY;
        int count = 0;
        ResultSet resultSet = null;

        if (log.isDebugEnabled()) {
            log.debug("Get the certificate count for tenantId" + tenantId);
        }

        try {
            connection = APIMgtDBUtil.getConnection();
            preparedStatement = connection.prepareStatement(certificateCountQuery);
            preparedStatement.setInt(1, tenantId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                count = resultSet.getInt("count");
            }
        } catch (SQLException e) {
            handleException("Error while retrieving the certificate count for tenantId " + tenantId + ".", e);
        } finally {
            APIMgtDBUtil.closeStatement(preparedStatement);
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }
        return count;
    }

    /**
     * To check whether alias with the given value exist already.
     *
     * @param alias Relevant alias.
     * @return true if the alias exist, false if not.
     * @throws CertificateManagementException Certificate Management Exception.
     */
    public boolean checkWhetherAliasExist(String alias) throws CertificateManagementException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String selectCertificateForAlias = SQLConstants.ClientCertificateConstants.SELECT_CERTIFICATE_FOR_ALIAS;
        boolean isExist = false;
        ResultSet resultSet = null;

        try {
            connection = APIMgtDBUtil.getConnection();
            preparedStatement = connection.prepareStatement(selectCertificateForAlias);
            preparedStatement.setString(1, alias);
            preparedStatement.setBoolean(2, false);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                isExist = true;
            }
            if (!isExist) {
                selectCertificateForAlias = SQLConstants.CertificateConstants.SELECT_CERTIFICATE_FOR_ALIAS;
                preparedStatement = connection.prepareStatement(selectCertificateForAlias);
                preparedStatement.setString(1, alias);
                resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    isExist = true;
                }
            }
        } catch (SQLException e) {
            handleException("Error while retrieving the certificates for alias " + alias + ".", e);
        } finally {
            APIMgtDBUtil.closeStatement(preparedStatement);
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }
        return isExist;
    }

    /**
     * Method to handle the SQL Exception.
     *
     * @param message : Error message.
     * @param e       : Throwable cause.
     * @throws CertificateManagementException :
     */
    private void handleException(String message, Throwable e) throws CertificateManagementException {

        throw new CertificateManagementException(message, e);
    }
}
