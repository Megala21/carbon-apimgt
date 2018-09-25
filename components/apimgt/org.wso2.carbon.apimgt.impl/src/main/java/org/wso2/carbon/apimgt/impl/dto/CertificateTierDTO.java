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

package org.wso2.carbon.apimgt.impl.dto;

/**
 * DTO Object to represent subscription tier related with client certificate.
 */
public class CertificateTierDTO {
    private String tier;
    private int spikeArrestLimit;
    private String spikeArrestUnit;
    private boolean stopOnQuotaReach;

    /**
     * To get the subscription tier name.
     *
     * @return subscription tier.
     */
    public String getTier() {
        return tier;
    }

    /**
     * To set the subscription tier
     *
     * @param tier Relevant subscription tier.
     */
    public void setTier(String tier) {
        this.tier = tier;
    }

    /**
     * To get the spike arrest limit.
     *
     * @return spike arrest limit.
     */
    public int getSpikeArrestLimit() {
        return spikeArrestLimit;
    }

    /**
     * To set the spike arrest limit.
     *
     * @param spikeArrestLimit Relevant spike arrest limit.
     */
    public void setSpikeArrestLimit(int spikeArrestLimit) {
        this.spikeArrestLimit = spikeArrestLimit;
    }

    /**
     * To get the spike arrest unit.
     *
     * @return spike arrest unit.
     */
    public String getSpikeArrestUnit() {
        return spikeArrestUnit;
    }

    /**
     * To set the spike arrest unit.
     *
     * @param spikeArrestUnit Spike arrest unit.
     */
    public void setSpikeArrestUnit(String spikeArrestUnit) {
        this.spikeArrestUnit = spikeArrestUnit;
    }

    /**
     * To check whether to stop on quota reach.
     *
     * @return stopOnQuotaReach value.
     */
    public boolean isStopOnQuotaReach() {
        return stopOnQuotaReach;
    }

    /**
     * To set stop on quota reach.
     *
     * @param stopOnQuotaReach Relevant stopOnQuotaReach that need to be set.
     */
    public void setStopOnQuotaReach(boolean stopOnQuotaReach) {
        this.stopOnQuotaReach = stopOnQuotaReach;
    }
}
