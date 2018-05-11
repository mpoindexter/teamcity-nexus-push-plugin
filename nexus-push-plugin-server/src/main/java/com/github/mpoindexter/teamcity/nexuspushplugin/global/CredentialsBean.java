/*
 * Copyright (c) 2018-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package com.github.mpoindexter.teamcity.nexuspushplugin.global;

import com.intellij.openapi.util.text.StringUtil;

import jetbrains.buildServer.serverSide.crypt.RSACipher;

public class CredentialsBean {

    private String username;
    private String password;

    public CredentialsBean() {
    }

    public CredentialsBean(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEncryptedPassword() {
        if (!StringUtil.isEmptyOrSpaces(password)) {
            return RSACipher.encryptDataForWeb(password);
        }
        return password;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        if (!StringUtil.isEmptyOrSpaces(password)) {
            encryptedPassword = RSACipher.decryptWebRequestData(encryptedPassword);
        }
        password = encryptedPassword;
    }

    public boolean isEmpty() {
        return StringUtil.isEmptyOrSpaces(username) || StringUtil.isEmptyOrSpaces(password);
    }
}
