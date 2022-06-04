/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.agent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.mpoindexter.teamcity.nexuspushplugin.ArtifactUploadSettings;
import com.github.mpoindexter.teamcity.nexuspushplugin.Constants;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;

import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.pathMatcher.AntPatternFileCollector;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import okio.HashingSink;
import okio.Okio;

public class NexusAgentListener extends AgentLifeCycleAdapter {
    private static final Logger LOG = Loggers.AGENT;

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .readTimeout(1, TimeUnit.MINUTES)
        .connectTimeout(1, TimeUnit.MINUTES)
        .build();

    private final ArtifactsWatcher artifactWatcher;

    public NexusAgentListener(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                              @NotNull final ArtifactsWatcher artifactWatcher) {
        this.artifactWatcher = artifactWatcher;
        agentDispatcher.addListener(this);
        LOG.info("Nexus agent listener started");
    }

    @Override
    public void beforeBuildFinish(AgentRunningBuild build, BuildFinishedStatus buildStatus) {
        if (buildStatus.isFailed()) {
            LOG.info("Nexus agent skipping failed build");
            return;
        }

        for (AgentBuildFeature f : build.getBuildFeatures()) {
            LOG.info("Nexus agent found feature: " + f.getType());
        }

        if (build.getBuildFeaturesOfType(Constants.NEXUS_PUSH_FEATURE_TYPE).isEmpty()) {
            LOG.info("Nexus agent skipping build, no features active");
            return;
        }

        build.getBuildLogger().activityStarted("Uploading Nexus artifacts", "", DefaultMessagesInfo.BLOCK_TYPE_BUILD_STEP);
        try {
            uploadArtifacts(build);
        } finally {
            build.getBuildLogger().activityFinished("Uploading Nexus artifacts", DefaultMessagesInfo.BLOCK_TYPE_BUILD_STEP);
        }
    }

    private void uploadArtifacts(AgentRunningBuild build) {
        BuildProgressLogger buildLog = build.getBuildLogger();
        Document doc = new Document(new Element("artifacts"));
        Element root = doc.getRootElement();
        for (AgentBuildFeature feature : build.getBuildFeaturesOfType(Constants.NEXUS_PUSH_FEATURE_TYPE)) {
            Map<String, String> parameters = feature.getParameters();

            boolean artifactUploadMandatory = "true".equals(parameters.get(Constants.ARTIFACT_UPLOAD_MANDATORY));

            String serverId = parameters.get(Constants.NEXUS_SERVER_ID);
            NexusServerSettings serverSettings = NexusServerSettings.getServerSettings(build, serverId);
            if (serverSettings == null) {
                errorOrFail("Cannot push artifact to Nexus:  Invalid server", buildLog, artifactUploadMandatory);
                continue;
            }

            String repositoryId = parameters.get(Constants.REPOSITORY_ID);
            if (StringUtil.isEmptyOrSpaces(repositoryId)) {
                errorOrFail("Cannot push artifact to Nexus:  Invalid repository ID", buildLog, artifactUploadMandatory);
                continue;
            }

            String artifactUploadSettingsSpec = parameters.get(Constants.ARTIFACT_UPLOAD_SETTINGS);
            if (StringUtil.isEmptyOrSpaces(artifactUploadSettingsSpec)) {
                errorOrFail("Cannot push artifact to Nexus:  Invalid artifact upload settings", buildLog, artifactUploadMandatory);
                continue;
            }

            ArtifactUploadSettings artifactUploadSettings = ArtifactUploadSettings.parse(artifactUploadSettingsSpec);
            if (artifactUploadSettings == null) {
                errorOrFail("Cannot push artifact to Nexus:  Invalid artifact upload settings", buildLog, artifactUploadMandatory);
                continue;
            }

            String deleteArtifactOnCleanup = parameters.get(Constants.DELETE_ARTIFACT_ON_CLEANUP);

            File workingDirectory = build.getCheckoutDirectory();
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            boolean hasError = false;
            List<Pair<String, File>> resolvedArtifacts = new ArrayList<>();
            String uploadMessage = "Uploading artifacts to Nexus with the following config:\n";
            for (ArtifactUploadSettings.UploadParameter param : artifactUploadSettings.getUploadParameters()) {
                uploadMessage += "\t" + param.key + "=";
                if (param.isFile) {
                    List<File> matchedFiles = AntPatternFileCollector.scanDir(workingDirectory, new String[] { param.value }, new AntPatternFileCollector.ScanOption[]{});
                    if (matchedFiles.size() == 1) {
                        File artifact = matchedFiles.get(0);
                        builder.addFormDataPart(param.key, artifact.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), artifact));
                        resolvedArtifacts.add(Pair.create(param.value, artifact));
                        uploadMessage += "File <" + artifact.getName() + ">\n";
                    } else if (matchedFiles.size() > 1) {
                        errorOrFail("Cannot push artifact to Nexus:  Pattern " + param.value + " matched multiple files", buildLog, artifactUploadMandatory);
                        hasError = true;
                    } else {
                        errorOrFail("Cannot push artifact to Nexus:  Pattern " + param.value + " did not match any files", buildLog, artifactUploadMandatory);
                        hasError = true;
                    }
                } else {
                    String value = param.value == null ? "" : param.value;
                    builder.addFormDataPart(param.key, value);
                    uploadMessage += value + "\n";
                }
            }

            if (hasError) {
                continue;
            }

            build.getBuildLogger().message(uploadMessage);

            try {
                HttpUrl url = HttpUrl.parse(serverSettings.getUrl()).newBuilder()
                    .addPathSegments("service/rest/v1/components")
                    .addQueryParameter("repository", repositoryId)
                    .build();

                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(serverSettings.getUsername(), serverSettings.getPassword()))
                    .post(builder.build())
                    .build();
                Response response = CLIENT.newCall(request).execute();
                
                if (!response.isSuccessful()) {
                    throw new IOException("Invalid status: " + response.code() + " " + response.body().string());
                }
            } catch (IOException e) {
                errorOrFail("Cannot push artifact to Nexus:  Upload failed - " + e.getMessage(), buildLog, artifactUploadMandatory);
                continue;
            }

            // TODO:  get metadata from Nexus upload when it's supported
            for (Pair<String, File> artifact : resolvedArtifacts) {
                try {
                    File artifactFile = artifact.second;
                    BufferedSource source = Okio.buffer(Okio.source(artifactFile));
                    HashingSink sink = HashingSink.sha1(Okio.blackhole());
                    source.readAll(sink);
                    String artifactHash = sink.hash().hex();
                    Element artifactElement = new Element("artifact");
                    artifactElement.setAttribute("path", artifact.first);
                    artifactElement.setAttribute("name", artifactFile.getName());
                    artifactElement.setAttribute("sha1", artifactHash);
                    artifactElement.setAttribute("serverId", serverId);
                    artifactElement.setAttribute("serverUrl", serverSettings.getUrl());
                    artifactElement.setAttribute("repository", repositoryId);
                    if (deleteArtifactOnCleanup != null) {
                        artifactElement.setAttribute("deleteArtifactOnCleanup", deleteArtifactOnCleanup);
                    }
                    root.addContent(artifactElement);
                } catch (IOException e) {
                    LOG.error("Cannot save nexus artifact metadata", e);
                }
            }
        }

        File tempDir = build.getBuildTempDirectory();
        try {
            File output = new File(tempDir, Constants.NEXUS_BUILD_METADATA_FILE);
            JDOMUtil.writeDocument(doc, output, "\n");
            artifactWatcher.addNewArtifactsPath(output.getAbsolutePath() + "=>.teamcity");
        } catch (IOException e) {
            LOG.error("Cannot save nexus artifact metadata", e);
        }
    }

    private void errorOrFail(String message, BuildProgressLogger buildLog, boolean fail) {
        LOG.error(message);
        buildLog.error(message);
        if (fail) {
            String id = Constants.NEXUS_PUSH_FEATURE_TYPE + ".uploadFailed";
            buildLog.logBuildProblem(BuildProblemData.createBuildProblem(id, Constants.NEXUS_PUSH_FEATURE_TYPE, message));
        }
    }
}