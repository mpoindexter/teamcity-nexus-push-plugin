/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.github.mpoindexter.teamcity.nexuspushplugin.Constants;
import com.github.mpoindexter.teamcity.nexuspushplugin.Http;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.CredentialsBean;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.GlobalSettingsManager;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.ServerConfigBean;
import com.github.mpoindexter.teamcity.nexuspushplugin.nexus.SearchResponse;
import com.intellij.openapi.util.JDOMUtil;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.artifacts.ArtifactsGuard;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.ViewLogTab;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class NexusArtifactsReportTab extends ViewLogTab {
    private final ArtifactsGuard artifactGuard;
    private final GlobalSettingsManager globalSettings;

    public NexusArtifactsReportTab(@NotNull PagePlaces pagePlaces,
                                   @NotNull SBuildServer server,
                                   @NotNull PluginDescriptor pluginDescriptor,
                                   @NotNull ArtifactsGuard artifactGuard,
                                   @NotNull GlobalSettingsManager globalSettings) {
        super("Nexus Artifacts", "nexusArtifactsTab", pagePlaces, server);
        this.artifactGuard = artifactGuard;
        this.globalSettings = globalSettings;
        setIncludeUrl(pluginDescriptor.getPluginResourcesPath() + "nexusArtifactsReport.jsp");
    }

    @Override
    protected void fillModel(Map<String, Object> model, HttpServletRequest request, SBuild build) {
        List<ArtifactReportBean> artifacts = new ArrayList<>();
        File artifactsDir = build.getArtifactsDirectory();
        artifactGuard.lockReading(artifactsDir);
        try {
            File metadataFile = new File(artifactsDir, Constants.NEXUS_BUILD_METADATA_PATH);
            Document doc = JDOMUtil.loadDocument(metadataFile);
            Element root = doc.getRootElement();
            @SuppressWarnings("unchecked")
            List<Element> artifactElements = root.getContent(new ElementFilter("artifact"));
            for (Element artifactElement : artifactElements) {
                String path = artifactElement.getAttributeValue("path");
                String name = artifactElement.getAttributeValue("name");
                String sha1 = artifactElement.getAttributeValue("sha1");
                String serverId = artifactElement.getAttributeValue("serverId");
                String serverUrl = artifactElement.getAttributeValue("serverUrl");
                String repository = artifactElement.getAttributeValue("repository");

                ArtifactReportBean bean = new ArtifactReportBean();
                bean.setFilePath(path);
                bean.setFileName(name);
                bean.setSha1(sha1);
                bean.setServerId(serverId);
                bean.setServerUrl(serverUrl);
                bean.setRepository(repository);
                artifacts.add(bean);
                ServerConfigBean serverConfig = globalSettings.getServer(serverId);
                if (serverConfig != null) {
                    try {
                        CredentialsBean credentials = serverConfig.getCredentials();
                        HttpUrl url = HttpUrl.parse(serverConfig.getUrl()).newBuilder()
                            .addPathSegments("service/rest/beta/search")
                            .addQueryParameter("repository", repository)
                            .addQueryParameter("sha1", sha1)
                            .build();

                        Request nexusRequest = new Request.Builder()
                            .url(url)
                            .addHeader("Accept", "application/json")
                            .addHeader("Authorization", Credentials.basic(credentials.getUsername(), credentials.getPassword()))
                            .get()
                            .build();
                        Response response = Http.CLIENT.newCall(nexusRequest).execute();
                        if (response.isSuccessful()) {
                            SearchResponse searchResponse = SearchResponse.ADAPTER.fromJson(response.body().source());
                            bean.setComponents(searchResponse.getItems());
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        } catch (IOException | JDOMException e) {
            // ignore
        } finally {
            artifactGuard.unlockReading(artifactsDir);
        }
        model.put("artifacts", artifacts);
    }

    @Override
    protected boolean isAvailable(HttpServletRequest request, SBuild build) {
        File artifactsDir = build.getArtifactsDirectory();
        artifactGuard.lockReading(artifactsDir);
        try {
            File metadataFile = new File(artifactsDir, Constants.NEXUS_BUILD_METADATA_PATH);
            return metadataFile.exists();
        } finally {
            artifactGuard.unlockReading(artifactsDir);
        }
    }
}