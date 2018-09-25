/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.apimgt.api.dto;

/**
 * DTO object to represent client certificate.
 */
public class ClientCertificateDTO {
    private String alias;
    private String certificate;
    private String uniqueId;
    private String tierName;

    /**
     * To get the unique id of the certificate.
     *
     * @return unique identifier of the certificate
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * To set the unique id of the certificate.
     *
     * @param uniqueId Unique ID of the certificate.
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * To get the tier name which the certificate is subscribed to.
     * @return tier name.
     */
    public String getTierName() {
        return tierName;
    }

    /**
     * To set the subscription tier for the current certificate.
     *
     * @param tierName Name of the tier.
     */
    public void setTierName(String tierName) {
        this.tierName = tierName;
    }
    
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
}
