/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.global;

import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.crypt.RSACipher;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

import org.jdom.Element;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.mpoindexter.teamcity.nexuspushplugin.Http;
import com.intellij.openapi.util.text.StringUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class GlobalSettingsController extends BaseFormXmlController {

    private GlobalSettingsManager settingsManager;

    public GlobalSettingsController(final GlobalSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @Override
    protected ModelAndView doGet(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response, Element xmlResponse) {
        boolean isEditMode = isEditMode(request);
        boolean isAddMode = isAddMode(request);

        if (isEditMode || isAddMode) {
            ActionErrors errors = validate(request);
            if (errors.hasErrors()) {
                errors.serialize(xmlResponse);
                return;
            }
        }

        if (isTestConnectionRequest(request)) {
            ActionErrors errors = testConnection(request);
            if (errors.hasErrors()) {
                errors.serialize(xmlResponse);
            }
            return;
        }

        if (isEditMode) {
            String id = request.getParameter("id");
            String url = request.getParameter("url");
            settingsManager.updateServer(id, url, getCredentialsFromRequest(request));
            settingsManager.persist();
            getOrCreateMessages(request).addMessage("objectUpdated", "Nexus server configuration was updated.");
        }

        if (isAddMode) {
            String url = request.getParameter("url");
            settingsManager.addServer(url, getCredentialsFromRequest(request));
            settingsManager.persist();
            getOrCreateMessages(request).addMessage("objectCreated", "Nexus server configuration was created.");
        }

        if (isDeleteMode(request)) {
            String id = request.getParameter("deleteObject");
            settingsManager.deleteServer(id);
            settingsManager.persist();
            getOrCreateMessages(request).addMessage("objectDeleted", "Nexus server configuration was deleted.");
        }
    }

    private ActionErrors validate(final HttpServletRequest request) {
        String url = request.getParameter("url");

        ActionErrors errors = new ActionErrors();
        if (StringUtil.isEmptyOrSpaces(url)) {
            errors.addError("errorUrl", "Please specify the URL of a Nexus server.");
        } else {
            try {
                new URL(url);
            } catch (MalformedURLException mue) {
                errors.addError("errorUrl", "Nexus server URL invalid.");
            }
        }
        return errors;
    }

    private ActionErrors testConnection(final HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();

        CredentialsBean credentials = getCredentialsFromRequest(request);

        String url = request.getParameter("url");

        try {
            HttpUrl requestUrl = HttpUrl.parse(url).newBuilder()
                                    .addPathSegments("service/rest/beta/read-only")
                                    .build();
            Request httpRequest = new Request.Builder()
                .url(requestUrl)
                .addHeader("Authorization", Credentials.basic(credentials.getUsername(), credentials.getPassword()))
                .build();
            Response response = Http.CLIENT.newCall(httpRequest).execute();
            
            if (!response.isSuccessful()) {
                errors.addError("errorConnection", "Invalid response status: " + response.code());
            }
        } catch (IOException e) {
            handleConnectionException(errors, url, e);
        }
        return errors;
    }

    private CredentialsBean getCredentialsFromRequest(HttpServletRequest request) {
        String username = request.getParameter("username");
        String encryptedPassword = request.getParameter("encryptedPassword");
        String password = RSACipher.decryptWebRequestData(encryptedPassword);
        CredentialsBean credentialsBean = new CredentialsBean(username, password);
        return credentialsBean;
    }

    private boolean isDeleteMode(final HttpServletRequest req) {
        return req.getParameter("deleteObject") != null;
    }

    private boolean isEditMode(final HttpServletRequest req) {
        return "edit".equals(req.getParameter("editMode"));
    }

    private boolean isAddMode(final HttpServletRequest req) {
        return "add".equals(req.getParameter("editMode"));
    }

    private boolean isTestConnectionRequest(final HttpServletRequest req) {
        String testConnectionParamValue = req.getParameter("testConnection");
        return !StringUtil.isEmptyOrSpaces(testConnectionParamValue) && Boolean.valueOf(testConnectionParamValue);
    }

    private void handleConnectionException(ActionErrors errors, String url, Exception e) {
        Throwable throwable = e.getCause();
        String errorMessage;
        if (throwable != null) {
            errorMessage = e.getMessage() + " (" + throwable.getClass().getCanonicalName() + ")";
        } else {
            errorMessage = e.getClass().getCanonicalName() + ": " + e.getMessage();
        }
        errors.addError("errorConnection", errorMessage);
        Loggers.SERVER.error("Error while testing the connection to Nexus server " + url, e);
    }
}