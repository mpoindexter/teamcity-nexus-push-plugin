<%--
* Copyright (c) 2018-Present Michael Poindexter.
* 
* This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
* which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
--%>

<%@include file="/include.jsp" %>

<jsp:useBean id="globalSettings" type="com.github.mpoindexter.teamcity.nexuspushplugin.global.GlobalSettingsManager"
             scope="request"/>

<jsp:useBean id="hexEncodedPublicKey" type="java.lang.String"
             scope="request"/>

<jsp:useBean id="random" type="java.lang.String"
             scope="request"/>

<style type="text/css">
    .editObjectFormDialog label {
        display: inline-block;
        width: 14.5em;
    }
</style>

<bs:linkCSS>
    /css/admin/vcsSettings.css
</bs:linkCSS>
<bs:linkScript>
    /js/bs/testConnection.js
</bs:linkScript>
<c:url var="controllerUrl" value="/admin/nexus/nexusGlobalSettings.html"/>

<script type="text/javascript">
    var TestConnectionDialog = OO.extend(BS.AbstractModalDialog, {
        getContainer: function() {
            return $('testConnectionDialog');
        },
        showTestDialog: function(successful, connectionDetails) {
            if (successful) {
                $('testConnectionStatus').innerHTML = 'Connection successful!';
                $('testConnectionStatus').className = 'testConnectionSuccess';
            } else {
                $('testConnectionStatus').innerHTML = 'Connection failed!';
                $('testConnectionStatus').className = 'testConnectionFailed';
            }
            $('testConnectionDetails').innerHTML = connectionDetails;
            $('testConnectionDetails').style.height = '';
            $('testConnectionDetails').style.overflow = 'auto';
            this.showCentered();
        }
    });
    var ConfigTabDialog = OO.extend(BS.AbstractPasswordForm,
            OO.extend(BS.AbstractModalDialog, {
                getContainer: function() {
                    return $('editObjectFormDialog');
                },
                formElement: function() {
                    return $('editObjectForm');
                },
                savingIndicator: function() {
                    return $('saving_configTab');
                },
                showAddDialog: function() {
                    ConfigTabDialog.enable();
                    this.formElement().id.value = '';
                    this.formElement().url.value = '';
                    $('errorUrl').innerHTML = '';
                    this.formElement().username.value = '';
                    this.formElement().password.value = '';
                    this.formElement().editMode.value = 'add';
                    this.showCentered();
                },
                showEditDialog: function(id, url, username, password, randomPass, publicKey) {
                    ConfigTabDialog.enable();
                    this.formElement().id.value = id;
                    this.formElement().url.value = url;
                    $('errorUrl').innerHTML = '';
                    if (ConfigTabDialog.isValueNotBlank(username)) {
                        this.formElement().username.value = username;
                    }
                    if (ConfigTabDialog.isValueNotBlank(password)) {
                        ConfigTabDialog.setPasswordValue(this.formElement().password, password, randomPass, publicKey);
                    }
                    this.formElement().editMode.value = 'edit';
                    this.showCentered();
                },
                setPasswordValue: function(passwordField, encryptedPassword, randomPass, publicKey) {
                    var passwordValue = '';
                    if (ConfigTabDialog.isValueNotBlank(encryptedPassword)) {
                        passwordValue = randomPass;
                    }
                    passwordField.value = passwordValue;
                    passwordField.newValue = passwordValue;
                    passwordField.encryptedPassword = encryptedPassword;
                    passwordField.getEncryptedPassword = function(pubKey) {
                        if (this.value == randomPass) return encryptedPassword;
                        return BS.Encrypt.encryptData(this.value, pubKey != null ? pubKey : publicKey);
                    }
                    passwordField.maskPassword = function() {
                        this.value = passwordValue;
                    }
                },
                isValueNotBlank: function(valueToCheck) {
                    return (valueToCheck != null) && (valueToCheck.length != 0);
                },
                save: function() {
                    var that = this;
                    // will serialize form params, and submit form to form.action
                    // if XML with errors is returned, corresponding error listener methods will be called
                    BS.PasswordFormSaver.save(this, this.formElement().action, OO.extend(BS.ErrorsAwareListener, {
                        errorUrl : function(elem) {
                            $('errorUrl').innerHTML = elem.firstChild.nodeValue;
                        },
                        errorTimeout : function(elem) {
                            $('errorTimeout').innerHTML = elem.firstChild.nodeValue;
                        },
                        onSuccessfulSave: function() {
                            ConfigTabDialog.enable();
                            ConfigTabDialog.close();
                            $('objectsTable').refresh();
                        }
                    }), false);
                    return false;
                },
                testConnection: function() {
                    var that = this;
                    // will serialize form params, and submit form to form.action
                    // if XML with errors is returned, corresponding error listener methods will be called
                    BS.PasswordFormSaver.save(this, this.formElement().action + '?testConnection=true',
                            OO.extend(BS.ErrorsAwareListener, {
                                errorUrl : function(elem) {
                                    $('errorUrl').innerHTML = elem.firstChild.nodeValue;
                                },
                                errorConnection : function(elem) {
                                    TestConnectionDialog.showTestDialog(false, elem.firstChild.nodeValue);
                                },
                                onSuccessfulSave: function() {
                                    TestConnectionDialog.showTestDialog(true,
                                            'Connection was successfully established.');
                                }
                            }), false);
                    return false;
                },
                deleteObject: function(id) {
                    if (!confirm('Are you sure you wish to delete this configuration?')) return false;
                    BS.ajaxRequest('${controllerUrl}', {
                        parameters: 'deleteObject=' + id,
                        onComplete: function() {
                            $('objectsTable').refresh();
                        }
                    })
                }
            }));
</script>
<div>

    <bs:refreshable containerId="objectsTable" pageUrl="${pageUrl}">

        <table border="0" style="width: 80%;">
            <bs:messages key="objectUpdated"/>
            <bs:messages key="objectCreated"/>
            <bs:messages key="objectDeleted"/>
        </table>

        <l:tableWithHighlighting className="settings" highlightImmediately="true" style="width: 80%;">
            <tr>
                <th>Nexus Server URL</th>
                <th colspan="2">Actions</th>
            </tr>
            <c:forEach var="server" items="${globalSettings.allServers}">
                <c:set var="onclick">
                    ConfigTabDialog.showEditDialog('${server.id}', '${server.url}',
                    '${server.credentials.username}',
                    '${server.credentials.encryptedPassword}',
                    '${hexEncodedPublicKey}',
                    '${random}')
                </c:set>
                <tr>
                    <td class="highlight" onclick="${onclick}">
                        <c:out value="${server.url}"/>
                    </td>
                    <td class="edit highlight">
                        <a href="#" onclick="${onclick}; return false">edit</a>
                    </td>
                    <td class="edit">
                        <a href="#" onclick="ConfigTabDialog.deleteObject(${server.id}); return false">delete</a>
                    </td>
                </tr>
            </c:forEach>
        </l:tableWithHighlighting>
    </bs:refreshable>

    <p>
        <a class="btn" href="#" onclick="ConfigTabDialog.showAddDialog(); return false">
            <span class="addNew">Create new Nexus Server configuration</span>
        </a>
    </p>

    <bs:modalDialog formId="editObjectForm" title="Edit Nexus Server Configuration" action="${controllerUrl}"
                    saveCommand="ConfigTabDialog.save();" closeCommand="ConfigTabDialog.close();">

        <table border="0" style="width: 409px">
            <tr>
                <td>
                    <label for="url">Nexus Server URL:
                        <span class="mandatoryAsterix" title="Mandatory field">*</span>
                        <bs:helpIcon
                                iconTitle="Specify the root URL of your Nexus installation, for example: http://nexus.sonatype.com"/>
                    </label>
                </td>
                <td>
                    <forms:textField name="url" value=""/>
                </td>
            </tr>
            <tr>
                <td colspan="2">
                    <span class="error" id="errorUrl" style="margin-left: 0;"></span>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="username">Username:
                        <bs:helpIcon
                                iconTitle="User with permissions to deploy to the selected Nexus server."/>
                    </label>
                </td>
                <td>
                    <forms:textField name="username" value=""/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="password">Password:
                        <bs:helpIcon
                                iconTitle="Password of the user entered above."/>
                    </label>
                </td>
                <td>
                    <forms:passwordField name="password"/>
                </td>
            </tr>
        </table>
        <div class="saveButtonsBlock">
            <a href="#" onclick="ConfigTabDialog.close(); return false" class="btn cancel">Cancel</a>
            <input class="btn btn_primary submitButton" type="submit" name="editObject" value="Save">
            <input class="btn btn_primary submitButton" id="testConnectionButton" type="button" value="Test connection"
                   onclick="ConfigTabDialog.testConnection();">
            <input type="hidden" name="id" value="">
            <input type="hidden" name="editMode" value="">
            <input type="hidden" id="publicKey" name="publicKey"
                   value="<c:out value='${hexEncodedPublicKey}'/>"/>
            <forms:saving id="saving_configTab"/>
        </div>

        <bs:dialog dialogId="testConnectionDialog" dialogClass="vcsRootTestConnectionDialog" title="Test Connection"
                   closeCommand="BS.TestConnectionDialog.close(); ConfigTabDialog.enable();"
                   closeAttrs="showdiscardchangesmessage='false'">
            <div id="testConnectionStatus"></div>
            <div id="testConnectionDetails"></div>
        </bs:dialog>
    </bs:modalDialog>
</div>