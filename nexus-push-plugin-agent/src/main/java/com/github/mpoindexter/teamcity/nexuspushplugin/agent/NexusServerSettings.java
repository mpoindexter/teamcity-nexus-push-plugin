/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.agent;

import java.util.Map;

import com.github.mpoindexter.teamcity.nexuspushplugin.Constants;

import jetbrains.buildServer.agent.AgentRunningBuild;

public class NexusServerSettings {
    public static NexusServerSettings getServerSettings(AgentRunningBuild build, String serverId) {
        if (serverId == null) {
            return null;
        }
        Map<String, String> params = build.getSharedConfigParameters();
        String url = params.get(Constants.AGENT_SERVER_URL_PARAM_PREFIX + serverId);
        String username = params.get(Constants.AGENT_SERVER_USERNAME_PARAM_PREFIX + serverId);
        String password = params.get(Constants.AGENT_SERVER_PASSWORD_PARAM_PREFIX + serverId);

        if (url == null || username == null || password == null) {
            return null;
        }

        return new NexusServerSettings(url, username, password);
    }

    private final String url;
    private final String username;
    private final String password;

    private NexusServerSettings(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}