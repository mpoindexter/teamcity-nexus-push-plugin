/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin;

public class Constants {
    public final static String NEXUS_SERVER_ID = "nexusServerId";
    public final static String REPOSITORY_ID = "repositoryId";
    public final static String ARTIFACT_UPLOAD_SETTINGS = "uploadSettings";
    public final static String DELETE_ARTIFACT_ON_CLEANUP = "deleteOnCleanup";
    public final static String ARTIFACT_UPLOAD_MANDATORY = "artifactUploadMandatory";

    public final static String NEXUS_BUILD_METADATA_FILE = "nexus-metadata.xml";
    public final static String NEXUS_BUILD_METADATA_PATH = ".teamcity/" + NEXUS_BUILD_METADATA_FILE;

    public static final String NEXUS_PUSH_FEATURE_TYPE = "com.github.mpoindexter.teamcity.nexuspushplugin";

    public final static String AGENT_SERVER_URL_PARAM_PREFIX = "secure:nexuspush.serverUrl.";
    public final static String AGENT_SERVER_USERNAME_PARAM_PREFIX = "secure:nexuspush.serverUsername.";
    public final static String AGENT_SERVER_PASSWORD_PARAM_PREFIX = "secure:nexuspush.serverPassword.";
}