/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.mpoindexter.teamcity.nexuspushplugin.global.GlobalSettingsManager;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.ServerConfigBean;
import com.intellij.openapi.util.text.StringUtil;

import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;

public class NexusBuildStartContextProcessor implements BuildStartContextProcessor {
    private final GlobalSettingsManager globalSettings;

    public NexusBuildStartContextProcessor(@NotNull final GlobalSettingsManager globalSettings) {
        this.globalSettings = globalSettings;
    }

    @Override
    public void updateParameters(BuildStartContext context) {
        Set<String> serverIds = new HashSet<>();
        for (SBuildFeatureDescriptor feature : context.getBuild().getBuildFeaturesOfType(Constants.NEXUS_PUSH_FEATURE_TYPE)) {
            Map<String, String> parameters = feature.getParameters();

            String serverId = parameters.get(Constants.NEXUS_SERVER_ID);
            if (!StringUtil.isEmptyOrSpaces(serverId)) {
                serverIds.add(serverId);
            }
        }

        for (String serverId : serverIds) {
            ServerConfigBean config = globalSettings.getServer(serverId);
            if (config == null) {
                continue;
            }

            context.addSharedParameter(Constants.AGENT_SERVER_URL_PARAM_PREFIX + serverId, config.getUrl());
            context.addSharedParameter(Constants.AGENT_SERVER_USERNAME_PARAM_PREFIX + serverId, config.getCredentials().getUsername());
            context.addSharedParameter(Constants.AGENT_SERVER_PASSWORD_PARAM_PREFIX + serverId, config.getCredentials().getPassword());
        }
    }
}