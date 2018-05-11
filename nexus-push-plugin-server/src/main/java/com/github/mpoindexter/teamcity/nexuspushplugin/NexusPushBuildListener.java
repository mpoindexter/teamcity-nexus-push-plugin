/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.github.mpoindexter.teamcity.nexuspushplugin.feature.NexusPushFeature;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.CredentialsBean;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.GlobalSettingsManager;
import com.github.mpoindexter.teamcity.nexuspushplugin.global.ServerConfigBean;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.text.StringUtil;

import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.artifacts.ArtifactsGuard;
import jetbrains.buildServer.serverSide.buildLog.MessageAttrs;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import okio.HashingSink;
import okio.Okio;

public class NexusPushBuildListener extends BuildServerAdapter {
    private final GlobalSettingsManager globalSettings;
    private final ArtifactsGuard artifactsGuard;

    public NexusPushBuildListener(@NotNull final SBuildServer buildServer,
                                  @NotNull final GlobalSettingsManager globalSettings,
                                  @NotNull final ArtifactsGuard artifactsGuard) {
        this.globalSettings = globalSettings;
        this.artifactsGuard = artifactsGuard;
        buildServer.addListener(this);
    }

    @Override
    public void beforeBuildFinish(SRunningBuild build) {
        if (build.getBuildStatus() != null && build.getBuildStatus().isSuccessful()) {
            Document doc = new Document(new Element("artifacts"));
            Element root = doc.getRootElement();
            for (SBuildFeatureDescriptor feature : build.getBuildFeaturesOfType(NexusPushFeature.FEATURE_TYPE)) {
                Map<String, String> parameters = feature.getParameters();

                String serverId = parameters.get(Constants.NEXUS_SERVER_ID);
                if (serverId == null || globalSettings.getServer(serverId) == null) {
                    build.getBuildLog().message("Cannot push artifact to Nexus:  Invalid server", Status.WARNING, MessageAttrs.attrs());
                    continue;
                }

                String repositoryId = parameters.get(Constants.REPOSITORY_ID);
                if (StringUtil.isEmptyOrSpaces(repositoryId)) {
                    build.getBuildLog().message("Cannot push artifact to Nexus:  Invalid repository ID", Status.WARNING, MessageAttrs.attrs());
                    continue;
                }

                String artifactUploadSettingsSpec = parameters.get(Constants.ARTIFACT_UPLOAD_SETTINGS);
                if (StringUtil.isEmptyOrSpaces(artifactUploadSettingsSpec)) {
                    build.getBuildLog().message("Cannot push artifact to Nexus:  Invalid artifact upload settings", Status.WARNING, MessageAttrs.attrs());
                    continue;
                }

                ArtifactUploadSettings artifactUploadSettings = ArtifactUploadSettings.parse(artifactUploadSettingsSpec);
                if (artifactUploadSettings == null) {
                    build.getBuildLog().message("Cannot push artifact to Nexus:  Invalid artifact upload settings", Status.WARNING, MessageAttrs.attrs());
                    continue;
                }

                Map<String, Object> params = new HashMap<String,Object>();
                boolean hasError = false;
                for (ArtifactUploadSettings.UploadParameter param : artifactUploadSettings.getUploadParameters()) {
                    String result = param.addToRequestParameters(params, build);
                    if (result != null) {
                        build.getBuildLog().message("Cannot push artifact to Nexus:  " + result, Status.WARNING, MessageAttrs.attrs());
                        hasError = true;
                    }
                }

                if (hasError) {
                    continue;
                }

                File artifactsDir = build.getArtifactsDirectory();
                artifactsGuard.lockReading(artifactsDir);
                try {
                    String uploadMessage = "Uploading artifacts to Nexus with the following config:\n";
                    MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        uploadMessage += "\t" + entry.getKey() + "=";
                        if (entry.getValue() instanceof File) {
                            File f = (File)entry.getValue();
                            builder.addFormDataPart(entry.getKey(), f.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), f));
                            uploadMessage += "File <" + f.getName() + ">\n";
                        } else {
                            String value = entry.getValue() == null ? "" : entry.getValue().toString();
                            builder.addFormDataPart(entry.getKey(), value);
                            uploadMessage += entry.getValue() + "\n";
                        }
                    }

                    build.getBuildLog().message(uploadMessage, Status.NORMAL, MessageAttrs.attrs());

                    ServerConfigBean serverConfig = globalSettings.getServer(serverId);

                    try {
                        CredentialsBean credentials = serverConfig.getCredentials();
                        HttpUrl url = HttpUrl.parse(serverConfig.getUrl()).newBuilder()
                            .addPathSegments("service/rest/beta/components")
                            .addQueryParameter("repository", repositoryId)
                            .build();

                        Request request = new Request.Builder()
                            .url(url)
                            .addHeader("Authorization", Credentials.basic(credentials.getUsername(), credentials.getPassword()))
                            .post(builder.build())
                            .build();
                        Response response = Http.CLIENT.newCall(request).execute();
                        
                        if (!response.isSuccessful()) {
                            throw new IOException("Invalid status: " + response.code() + " " + response.body().string());
                        }
                    } catch (IOException e) {
                        build.getBuildLog().message("Cannot push artifact to Nexus:  Upload failed - " + e.getMessage(), Status.WARNING, MessageAttrs.attrs());
                        continue;
                    }

                    // TODO:  get metadata from Nexus upload when it's supported
                    for (ArtifactUploadSettings.UploadParameter uploadParam : artifactUploadSettings.getUploadParameters()) {
                        if (uploadParam instanceof ArtifactUploadSettings.FileUploadParameter) {
                            try {
                                String artifactPath = ((ArtifactUploadSettings.FileUploadParameter)uploadParam).artifactPath;
                                File artifactFile = new File(artifactsDir, artifactPath);
                                BufferedSource source = Okio.buffer(Okio.source(artifactFile));
                                HashingSink sink = HashingSink.sha1(Okio.blackhole());
                                source.readAll(sink);
                                String artifactHash = sink.hash().hex();
                                Element artifactElement = new Element("artifact");
                                artifactElement.setAttribute("path", artifactPath);
                                artifactElement.setAttribute("name", artifactFile.getName());
                                artifactElement.setAttribute("sha1", artifactHash);
                                artifactElement.setAttribute("serverId", serverId);
                                artifactElement.setAttribute("serverUrl", serverConfig.getUrl());
                                artifactElement.setAttribute("repository", repositoryId);
                                artifactElement.setAttribute("featureId", feature.getId());
                                root.addContent(artifactElement);
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                } finally {
                    artifactsGuard.unlockReading(artifactsDir);
                }
            }

            File artifactsDir = build.getArtifactsDirectory();
            artifactsGuard.lockWriting(artifactsDir);
            try {
                File output = new File(artifactsDir, Constants.NEXUS_BUILD_METADATA_PATH);
                JDOMUtil.writeDocument(doc, output, "\n");
            } catch (IOException e) {
                // Ignore
            } finally {
                artifactsGuard.unlockWriting(artifactsDir);
            }
        }
    }
}