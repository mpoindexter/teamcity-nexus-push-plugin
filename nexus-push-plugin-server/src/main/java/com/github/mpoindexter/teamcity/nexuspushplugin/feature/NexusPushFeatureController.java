/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.feature;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.mpoindexter.teamcity.nexuspushplugin.global.GlobalSettingsManager;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.ServerConfigBean;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;

public class NexusPushFeatureController extends BaseController {
    private final PluginDescriptor descriptor;
    private final GlobalSettingsManager globalSettingsManager;
    private final String myUrl;
  
    public NexusPushFeatureController(@NotNull WebControllerManager controllerManager,
                                      @NotNull PluginDescriptor descriptor,
                                      @NotNull GlobalSettingsManager globalSettingsManager) {
        myUrl = descriptor.getPluginResourcesPath("nexusPushFeature.html");
        controllerManager.registerController(myUrl, this);
        this.descriptor = descriptor;
        this.globalSettingsManager = globalSettingsManager;
    }

    public String getUrl() {
        return myUrl;
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        List<ServerConfigBean> allServers = globalSettingsManager.getAllServers();
        ModelAndView mv = new ModelAndView(descriptor.getPluginResourcesPath("nexusPushFeature.jsp"));
        mv.addObject("allServers", allServers);
        return mv;
    }
}
