/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.parameters.ValueResolver;
import jetbrains.buildServer.serverSide.SBuild;

public class ArtifactUploadSettings {

    public static ArtifactUploadSettings parse(String settings) {
        String[] lines = settings.split("\\r?\\n");
        List<UploadParameter> uploadParameters = new ArrayList<UploadParameter>();
        for (String line : lines) {
            if (line.indexOf('=') == -1) {
                return null;
            }

            String[] parts = line.split("=", 2);
            String key = parts[0];
            String valueSpec = parts[1];

            if (valueSpec.startsWith("@")) {
                uploadParameters.add(new FileUploadParameter(key, valueSpec.substring(1)));
            } else {
                uploadParameters.add(new SimpleUploadParameter(key, valueSpec));
            }
        }

        return new ArtifactUploadSettings(uploadParameters);
    }

    private final List<UploadParameter> uploadParameters;

    private ArtifactUploadSettings(List<UploadParameter> uploadParameters) {
        this.uploadParameters = uploadParameters;
    }

    public List<UploadParameter> getUploadParameters() {
        return uploadParameters;
    }

    public static interface UploadParameter {
        public String addToRequestParameters(Map<String, Object> params, SBuild build);
    }

    private static class SimpleUploadParameter implements UploadParameter {
        private final String key;
        private final String value;

        public SimpleUploadParameter(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String addToRequestParameters(Map<String, Object> params, SBuild build) {
            ValueResolver resolver = build.getValueResolver();
            String value = resolver.resolve(this.value).getResult();
            params.put(key, value);
            return null;
        }
    }

    static class FileUploadParameter implements UploadParameter {
        private final String key;
        final String artifactPath;
        public FileUploadParameter(String key, String artifactPath) {
            this.key = key;
            this.artifactPath = artifactPath;
        }

        @Override
        public String addToRequestParameters(Map<String, Object> params, SBuild build) {
            File artifactsDir = build.getArtifactsDirectory();
            File artifact = new File(artifactsDir, this.artifactPath);
            if (!artifact.exists()) {
                return "Cannot find artifact " + artifactPath;
            }
            params.put(key, artifact);
            return null;
        }
    }
}