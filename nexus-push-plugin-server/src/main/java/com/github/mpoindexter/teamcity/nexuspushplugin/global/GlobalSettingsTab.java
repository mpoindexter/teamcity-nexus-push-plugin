/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.global;

import jetbrains.buildServer.serverSide.auth.AuthUtil;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.SecurityContext;
import jetbrains.buildServer.serverSide.crypt.RSACipher;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PositionConstraint;
import jetbrains.buildServer.web.openapi.SimpleCustomTab;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class GlobalSettingsTab extends SimpleCustomTab {

    private final SecurityContext securityContext;
    private final GlobalSettingsManager globalSettings;

    public GlobalSettingsTab(final @NotNull WebControllerManager controllerManager,
                             final @NotNull SecurityContext securityContext,
                             final @NotNull GlobalSettingsManager globalSettings) {
        super(controllerManager, PlaceId.ADMIN_SERVER_CONFIGURATION_TAB, "nexus-push-plugin",
                "nexusGlobalSettings.jsp",
                "Nexus Servers");
        this.securityContext = securityContext;
        this.globalSettings = globalSettings;

        setPosition(PositionConstraint.after("serverConfigGeneral"));
        register();

        controllerManager.registerController("/admin/nexus/nexusGlobalSettings.html", new GlobalSettingsController(globalSettings));
    }

    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        super.fillModel(model, request);
        model.put("hexEncodedPublicKey", RSACipher.getHexEncodedPublicKey());
        model.put("random", String.valueOf(Math.random()));
        model.put("globalSettings", globalSettings);
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && userHasPermission();
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        return super.isAvailable(request) && userHasPermission();
    }

    private boolean userHasPermission() {
        return AuthUtil.hasGlobalPermission(securityContext.getAuthorityHolder(), Permission.CHANGE_SERVER_SETTINGS);
    }
}