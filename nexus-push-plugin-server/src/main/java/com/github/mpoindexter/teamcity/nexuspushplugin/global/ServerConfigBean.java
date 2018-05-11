/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.global;

public class ServerConfigBean {
    private String id;
    private String url;
    private CredentialsBean credentials;

    public String getId() {
        return id;
    }

    public CredentialsBean getCredentials() {
        return credentials;
    }

    public String getUrl() {
        return url;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCredentials(CredentialsBean credentials) {
        this.credentials = credentials;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}