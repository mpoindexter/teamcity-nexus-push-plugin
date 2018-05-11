/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.github.mpoindexter.teamcity.nexuspushplugin.ArtifactUploadSettings;
import com.github.mpoindexter.teamcity.nexuspushplugin.Constants;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.GlobalSettingsManager;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.ServerConfigBean;
import com.intellij.openapi.util.text.StringUtil;

import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;

public class NexusPushFeature extends BuildFeature {
    public static final String FEATURE_TYPE = "com.github.mpoindexter.teamcity.nexuspushplugin";

    private final NexusPushFeatureController controller;
    private final GlobalSettingsManager globalSettings;

    public NexusPushFeature(@NotNull final NexusPushFeatureController controller,
                            @NotNull final GlobalSettingsManager globalSettings) {
        this.controller = controller;
        this.globalSettings = globalSettings;
    }

    @Override
    @NotNull
    public String getType() {
        return FEATURE_TYPE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Nexus artifact publisher";
    }

    @Override
    public String getEditParametersUrl() {
        return controller.getUrl();
    }

    @Override
    public boolean isMultipleFeaturesPerBuildTypeAllowed() {
        return true;
    }

    @Override
    public String describeParameters(Map<String, String> params) {
        String serverId = params.get(Constants.NEXUS_SERVER_ID);
        if (serverId == null) {
            return "";
        }

        ServerConfigBean serverConfig = globalSettings.getServer(serverId);
        if (serverConfig == null) {
            return "";
        }
        return "Push artifact to " + serverConfig.getUrl();
    }

    @Override
    public PropertiesProcessor getParametersProcessor() {
        return new PropertiesProcessor() {
            public Collection<InvalidProperty> process(Map<String, String> params) {
                List<InvalidProperty> errors = new ArrayList<InvalidProperty>();
                String serverId = params.get(Constants.NEXUS_SERVER_ID);
                if (StringUtil.isEmptyOrSpaces(serverId) || globalSettings.getServer(serverId) == null) {
                    errors.add(new InvalidProperty(Constants.NEXUS_SERVER_ID, "Specify a Nexus server to push to"));
                }

                String repositoryId = params.get(Constants.REPOSITORY_ID);
                if (StringUtil.isEmptyOrSpaces(repositoryId)) {
                    errors.add(new InvalidProperty(Constants.REPOSITORY_ID, "Specify a repository to push to"));
                }

                String artifactUploadSettings = params.get(Constants.ARTIFACT_UPLOAD_SETTINGS);
                if (StringUtil.isEmptyOrSpaces(artifactUploadSettings)) {
                    errors.add(new InvalidProperty(Constants.ARTIFACT_UPLOAD_SETTINGS, "Specify artifact upload settings"));
                }

                if (ArtifactUploadSettings.parse(artifactUploadSettings) == null) {
                    errors.add(new InvalidProperty(Constants.ARTIFACT_UPLOAD_SETTINGS, "Invalid artifact upload settings"));
                }

                String deleteOnCleanup = params.get(Constants.DELETE_ARTIFACT_ON_CLEANUP);
                if (StringUtil.isEmptyOrSpaces(deleteOnCleanup)) {
                    errors.add(new InvalidProperty(Constants.DELETE_ARTIFACT_ON_CLEANUP, "Specify whether to delete artifact on cleanup"));
                }
                
                return errors;
            }
        };
    }

    @Override
    public boolean isRequiresAgent() {
        return false;
    }
}