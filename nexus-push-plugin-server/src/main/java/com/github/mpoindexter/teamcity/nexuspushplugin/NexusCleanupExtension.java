/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.github.mpoindexter.teamcity.nexuspushplugin.global.CredentialsBean;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.GlobalSettingsManager;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.ServerConfigBean;
import com.github.mpoindexter.teamcity.nexuspushplugin.nexus.SearchResponse;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.text.StringUtil;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.cleanup.BuildCleanupContext;
import jetbrains.buildServer.serverSide.cleanup.CleanupExtensionAdapter;
import jetbrains.buildServer.serverSide.cleanup.ErrorReporter;
import jetbrains.buildServer.util.positioning.PositionAware;
import jetbrains.buildServer.util.positioning.PositionConstraint;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class NexusCleanupExtension extends CleanupExtensionAdapter implements PositionAware {
    private static final Logger LOG = Loggers.CLEANUP;

    private final GlobalSettingsManager globalSettings;

    public NexusCleanupExtension(@NotNull GlobalSettingsManager globalSettings) {
        this.globalSettings = globalSettings;
    }
    
    @Override
    public String getOrderId() {
        return "nexuspush";
    }

    @Override
    public PositionConstraint getConstraint() {
        return PositionConstraint.first();
    }

    @Override
    public void cleanupBuildsData(BuildCleanupContext cleanupContext) throws Exception {
        for (SFinishedBuild build : cleanupContext.getBuilds()) {
            if (cleanupContext.getCleanupState().isInterrupted()) {
                return;
            }

            if (cleanupContext.getCleanupLevel().isCleanArtifacts()) {
                File metadataFile = new File(build.getArtifactsDirectory(), Constants.NEXUS_BUILD_METADATA_PATH);
                if (!metadataFile.exists()) {
                    LOG.info("Build " + build.getBuildId() + " has no Nexus metadata, skipping");
                    continue;
                }
                try {
                    Document doc = JDOMUtil.loadDocument(metadataFile);
                    Element root = doc.getRootElement();
                    @SuppressWarnings("unchecked")
                    List<Element> artifactElements = root.getContent(new ElementFilter("artifact"));
                    for (Element artifactElement : artifactElements) {
                        String sha1 = artifactElement.getAttributeValue("sha1");
                        if (StringUtil.isEmptyOrSpaces(sha1)) {
                            continue;
                        }
                        String serverId = artifactElement.getAttributeValue("serverId");
                        if (StringUtil.isEmptyOrSpaces(serverId)) {
                            continue;
                        }
                        String repository = artifactElement.getAttributeValue("repository");
                        if (StringUtil.isEmptyOrSpaces(repository)) {
                            continue;
                        }

                        String deleteArtifactOnCleanup = artifactElement.getAttributeValue("deleteArtifactOnCleanup");
                        if ("true".equals(deleteArtifactOnCleanup)) {
                            removeComponent(build.getBuildId(), serverId, repository, sha1, cleanupContext.getErrorReporter());
                        } else {
                            LOG.info("Artifact " + sha1 + " is not marked for deletion, skipping");
                        }
                    }
                } catch (IOException | JDOMException e) {
                    LOG.warn("Error reading nexus build data: " + e.getMessage() + " for build " + build.getBuildId());
                }
            }
        }
    }

    private void removeComponent(long buildId, String serverId, String repository, String sha1, ErrorReporter errorReporter) {
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
                    if (searchResponse.getItems() != null && searchResponse.getItems().size() == 1) {
                        String componentId = searchResponse.getItems().get(0).getId();
                        HttpUrl deleteUrl = HttpUrl.parse(serverConfig.getUrl()).newBuilder()
                            .addPathSegments("service/rest/beta/components")
                            .addPathSegment(componentId)
                            .build();
                        Request deleteRequest = new Request.Builder()
                            .url(deleteUrl)
                            .addHeader("Accept", "application/json")
                            .addHeader("Authorization", Credentials.basic(credentials.getUsername(), credentials.getPassword()))
                            .delete()
                            .build();
                        Response deleteResponse = Http.CLIENT.newCall(deleteRequest).execute();
                        if (!deleteResponse.isSuccessful()) {
                            errorReporter.buildCleanupError(buildId, "Cannot delete artifact on cleanup nexus artifact: " + response.body().string());
                        }
                    } else {
                        LOG.warn("Artifact did not have exactly one component associated, will not remove for build " + buildId);
                    }
                } else {
                    LOG.warn("Cannot find artifact on cleanup nexus artifact: " + response.body().string());
                    errorReporter.buildCleanupError(buildId, "Cannot find artifact on cleanup nexus artifact: " + response.body().string());
                }
            } catch (IOException e) {
                LOG.warn("IO Exception on cleanup nexus artifact: " + e.getMessage());
                errorReporter.buildCleanupError(buildId, "IO Exception on cleanup nexus artifact: " + e.getMessage());
            }
        }
    }
}