/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.report;

import java.util.Collections;
import java.util.List;

import com.github.mpoindexter.teamcity.nexuspushplugin.nexus.ComponentBean;

public class ArtifactReportBean {
    private String filePath;
    private String fileName;
    private String sha1;
    private String serverId;
    private String serverUrl;
    private String repository;

    private List<ComponentBean> components = Collections.emptyList();

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the filePath
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @param filePath the filePath to set
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * @return the sha1
     */
    public String getSha1() {
        return sha1;
    }

    /**
     * @param sha1 the sha1 to set
     */
    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    /**
     * @return the serverId
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * @param serverId the serverId to set
     */
    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    /**
     * @return the serverUrl
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * @param serverUrl the serverUrl to set
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * @return the repository
     */
    public String getRepository() {
        return repository;
    }

    /**
     * @param repository the repository to set
     */
    public void setRepository(String repository) {
        this.repository = repository;
    }

    /**
     * @return the components
     */
    public List<ComponentBean> getComponents() {
        return components;
    }

    /**
     * @param components the components to set
     */
    public void setComponents(List<ComponentBean> components) {
        this.components = components;
    }
}