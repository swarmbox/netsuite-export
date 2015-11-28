/*
 * Copyright (c) 2015
 * SwarmBox
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the {organization} nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.swarmbox.netsuite;

import com.netsuite.webservices.platform.core_2015_2.*;
import com.netsuite.webservices.platform.messages_2015_2.ApplicationInfo;
import com.netsuite.webservices.platform.messages_2015_2.SearchPreferences;
import com.netsuite.webservices.platform_2015_2.NetSuiteBindingStub;
import com.netsuite.webservices.platform_2015_2.NetSuitePortType;
import com.netsuite.webservices.platform_2015_2.NetSuiteService;
import com.netsuite.webservices.platform_2015_2.NetSuiteServiceLocator;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.rpc.ServiceException;
import javax.xml.soap.SOAPException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

public class Helpers {

    private static final Logger LOG = LoggerFactory.getLogger(Helpers.class);
    private static final XMLConfiguration CONFIGURATION;

    static {
        XMLConfiguration configuration;
        try {
            configuration = new XMLConfiguration("settings.xml");
        } catch (ConfigurationException e) {
            throw wrapCheckedException("Failed to get configuration", e);
        }
        CONFIGURATION = configuration;
    }

    public static RuntimeException wrapCheckedException(String m, Exception e) {
        Throwable cause = getRootCause(e);
        if (cause == null) cause = e;
        return new RuntimeException(m, cause);
    }

    static XMLConfiguration getConfiguration() {
        return CONFIGURATION;
    }

    static NetSuitePortType nsBuildAuthenticatedStub() throws ServiceException, RemoteException {

        /* Application Info */

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.setApplicationId(CONFIGURATION.getString("ns-application-id"));

        /* Passport */

        Passport passport = new Passport();
        passport.setAccount(CONFIGURATION.getString("ns-account-id"));
        passport.setEmail(CONFIGURATION.getString("ns-email"));
        passport.setPassword(CONFIGURATION.getString("ns-password"));
        String roleId = CONFIGURATION.getString("ns-role-id", null);
        if (roleId != null) {
            RecordRef role = new RecordRef();
            role.setInternalId(roleId);
            passport.setRole(role);
        } else {
            passport.setRole(null);
        }

        /* Search Preferences */

        SearchPreferences searchPreferences = new SearchPreferences();
        searchPreferences.setBodyFieldsOnly(false);
        searchPreferences.setPageSize(CONFIGURATION.getInt("ns-page-size"));

        /* SOAP Headers */

        SOAPHeaderElement applicationInfoHeader = new SOAPHeaderElement("urn:messages_2015_2.platform.webservices.netsuite.com", "applicationInfo");
        SOAPHeaderElement passportHeader = new SOAPHeaderElement("urn:messages_2015_2.platform.webservices.netsuite.com", "passport");
        SOAPHeaderElement searchPreferencesHeader = new SOAPHeaderElement("urn:messages_2015_2.platform.webservices.netsuite.com", "searchPreferences");
        try {
            applicationInfoHeader.setObjectValue(applicationInfo);
            passportHeader.setObjectValue(passport);
            searchPreferencesHeader.setObjectValue(searchPreferences);
        } catch (SOAPException e) {
            throw wrapCheckedException("Failed to create SOAP Header", e);
        }

        /* Binding Stub */

        NetSuiteService service = new NetSuiteServiceLocator();
        NetSuiteBindingStub stub = (NetSuiteBindingStub) service.getNetSuitePort();
        stub.clearHeaders();
        stub.setHeader(applicationInfoHeader);
        stub.setHeader(passportHeader);
        stub.setHeader(searchPreferencesHeader);
        stub.setMaintainSession(false);

        return stub;
    }

    static String nsGetStatusDetails(Status status) {
        return Optional.ofNullable(status.getStatusDetail())
                       .map(Arrays::stream)
                       .map(s -> s.map(StatusDetail::getMessage).collect(Collectors.joining("\n")))
                       .orElse("[No Status Details Available]");
    }

    static SearchResult nsSearch(NetSuitePortType port, SearchRecord search, int retry, Exception e) {
        if (retry <= -1) throw wrapCheckedException("Maximum retries reached", e);
        try {
            SearchResult result = port.search(search);
            if (!result.getStatus().isIsSuccess()) {
                LOG.error(nsGetStatusDetails(result.getStatus()));
            }
            return result;
        } catch (RemoteException remoteException) {
            return nsSearch(port, search, --retry, remoteException);
        }
    }

    static SearchResult nsSearchMoreWithId(NetSuitePortType port, String searchId, int pageIndex, int retry, Exception e) {
        if (retry <= -1) throw wrapCheckedException("Maximum retries reached", e);
        try {
            SearchResult result = port.searchMoreWithId(searchId, pageIndex);
            if (!result.getStatus().isIsSuccess()) {
                throw new RuntimeException("Search failed: "+ nsGetStatusDetails(result.getStatus()));
            }
            return result;
        } catch (RemoteException remoteException) {
            return nsSearchMoreWithId(port, searchId, pageIndex, --retry, remoteException);
        }
    }
}
