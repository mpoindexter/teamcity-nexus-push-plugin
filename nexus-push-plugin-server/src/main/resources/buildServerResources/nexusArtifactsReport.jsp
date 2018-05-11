<%--
* Copyright (c) 2018-Present Michael Poindexter.
* 
* This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
* which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="artifacts" type="java.util.List" scope="request"/>

<style>
    .summary-label {
        font-weight: bold;
    }

    .component {
        margin-top: 10px;
        border: 1px solid lightgrey;
        border-radius: 5px;
        padding: 5px;
    }
</style>

<div>
    <c:forEach var="artifact" items="${artifacts}">

        <div class="artifact">
            <h1 class="artifact-name">${artifact.fileName}</h1>

            <div class="artifact-summary">
                <table>
                    <tr>
                        <td class="summary-label">Server URL</td>
                        <td class="summary-value"><a href="${artifact.serverUrl}">${artifact.serverUrl}</a></td>
                    </tr>
                    <tr>
                        <td class="summary-label">Repository</td>
                        <td class="summary-value">${artifact.repository}</td>
                    </tr>
                    <tr>
                        <td class="summary-label">File Path</td>
                        <td class="summary-value">${artifact.filePath}</td>
                    </tr>

                    <tr>
                        <td class="summary-label">SHA 1</td>
                        <td class="summary-value">${artifact.sha1}</td>
                    </tr>
                </table>
            </div>

            <div class="artifact-components">
                <h2>Components</h2>
                <c:forEach var="component" items="${artifact.components}">
                    <div class="component">
                        <div class="component-summary">
                            <table>
                                <tr>
                                    <td class="summary-label">Name</td>
                                    <td class="summary-value">${component.name}</a></td>
                                </tr>
                                <tr>
                                    <td class="summary-label">Group</td>
                                    <td class="summary-value">${component.group}</td>
                                </tr>
                                <tr>
                                    <td class="summary-label">Version</td>
                                    <td class="summary-value">${component.version}</td>
                                </tr>
                                <tr>
                                    <td class="summary-label">Format</td>
                                    <td class="summary-value">${component.format}</td>
                                </tr>
                            </table>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>
    </c:forEach>
</div>