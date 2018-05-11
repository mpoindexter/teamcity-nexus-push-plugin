/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.global;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.containers.hash.HashMap;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.crypt.EncryptUtil;

public class GlobalSettingsManager {
    private static final String CONFIG_FILE_NAME = "nexus-publisher-settings.xml";

    private final Map<String, ServerConfigBean> config;
    private final File configFile;

    public GlobalSettingsManager(@NotNull ServerPaths serverPaths) {
        this.config = new HashMap<String,ServerConfigBean>();
        configFile = new File(serverPaths.getConfigDir(), CONFIG_FILE_NAME);
        loadSettings();
    }

    public void updateServer(String id, String url, CredentialsBean credentials) {
        synchronized(config) {
            ServerConfigBean serverConfig = config.get(id);
            if (serverConfig != null) {
                serverConfig.setUrl(url);
                serverConfig.setCredentials(credentials);
            }
        }
    }

    public void addServer(String url, CredentialsBean credentials) {
        synchronized(config) {
            String id = UUID.randomUUID().toString();
            ServerConfigBean serverConfig = new ServerConfigBean();
            serverConfig.setId(id);
            serverConfig.setUrl(url);
            serverConfig.setCredentials(credentials);
            config.put(id, serverConfig);
        }
    }

    public void deleteServer(String id) {
        synchronized(config) {
            config.remove(id);
        }
    }

    public ServerConfigBean getServer(String id) {
        synchronized (config) {
            return config.get(id);
        }
    }

    public List<ServerConfigBean> getAllServers() {
        synchronized (this.config) {
            List<ServerConfigBean> configs = new ArrayList<ServerConfigBean>(config.values());
            List<ServerConfigBean> result = new ArrayList<ServerConfigBean>();
            for (ServerConfigBean server : configs) {
                result.add(server);
            }
            return result;
        }
    }

    public void persist() {
        Document doc = new Document(new Element("nexusConfig"));
        Element root = doc.getRootElement();
        synchronized (this.config) {
            List<ServerConfigBean> configs = new ArrayList<ServerConfigBean>(config.values());
            for (ServerConfigBean serverConfig : configs) {
                Element serverConfigElement = new Element("server");
                serverConfigElement.setAttribute("id", serverConfig.getId());
                serverConfigElement.setAttribute("url", serverConfig.getUrl());
                serverConfigElement.setAttribute("username", serverConfig.getCredentials().getUsername());
                serverConfigElement.setAttribute("password", EncryptUtil.scramble(serverConfig.getCredentials().getPassword()));
                root.addContent(serverConfigElement);
            }
        }
        try {
            JDOMUtil.writeDocument(doc, configFile, "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadSettings() {
        if (configFile.exists()) {
            try {
                Document doc = JDOMUtil.loadDocument(configFile);
                @SuppressWarnings("unchecked")
                List<Element> serverElements = doc.getRootElement().getContent(new ElementFilter("server"));
                for (Element serverElement : serverElements) {
                    ServerConfigBean serverConfig = new ServerConfigBean();
                    String id = serverElement.getAttributeValue("id");
                    String url = serverElement.getAttributeValue("url");
                    String username = serverElement.getAttributeValue("username");
                    String password = serverElement.getAttributeValue("password");
                    if (EncryptUtil.isScrambled(password)) {
                        password = EncryptUtil.unscramble(password);
                    }

                    serverConfig.setId(id);
                    serverConfig.setUrl(url);
                    serverConfig.setCredentials(new CredentialsBean(username, password));
                    config.put(id, serverConfig);
                }
            } catch (IOException | JDOMException e) {
                System.err.println("Cannot read nexus settings");
                e.printStackTrace();
            }
        }
    }
}