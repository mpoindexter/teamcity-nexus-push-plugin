/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin;

import java.util.ArrayList;
import java.util.List;

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
                uploadParameters.add(new UploadParameter(key, valueSpec.substring(1), true));
            } else {
                uploadParameters.add(new UploadParameter(key, valueSpec, false));
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

    public static class UploadParameter {
        public final boolean isFile;
        public final String key;
        public final String value;

        public UploadParameter(String key, String value, boolean isFile) {
            this.isFile = isFile;
            this.key = key;
            this.value = value;
        }
    }
}