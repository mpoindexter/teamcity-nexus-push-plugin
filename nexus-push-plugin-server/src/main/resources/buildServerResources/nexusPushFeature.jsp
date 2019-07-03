<%--
* Copyright (c) 2018-Present Michael Poindexter.
* 
* This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
* which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
--%>

<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<l:settingsGroup title="Nexus Settings">
<tr>
  <th>Nexus Server:<l:star/></th>
  <td>
    <props:selectProperty name="nexusServerId">
      <c:forEach items="${allServers}" var="server">
        <props:option value="${server.id}">${server.url}</props:option>
      </c:forEach>
    </props:selectProperty>
    <span class="error" id="error_nexusServerId"></span>
  </td>
</tr>
<tr>
  <th>Repository ID:<l:star/></th>
  <td>
    <props:textProperty name="repositoryId" className="longField"/>
    <span class="error" id="error_repositoryId"></span>
    <span class="smallNote">Specify Nexus repository ID</span>
  </td>
</tr>
<tr>
  <th>Artifact Upload Parameters:<l:star/></th>
  <td>
    <props:multilineProperty name="uploadSettings" linkTitle="Artifact Upload Parameters" cols="49" rows="5" className="longField" />
    <span class="error" id="error_uploadSettings"></span>
    <span class="smallNote">
      For example for RAW:
      <br>
      raw.directory=TC_plugin/
      raw.asset1=@target/nexus-push-plugin.zip
      raw.asset1.filename=nexus-push-plugin.zip
      <br>
      <a href ="https://help.sonatype.com/repomanager3/rest-and-integration-api/components-api">More example in doc REST NEXUS</a>
    </span>
  </td>
</tr>
<tr>
  <th>Delete artifacts on cleanup:<l:star/></th>
  <td>
    <props:checkboxProperty name="deleteOnCleanup" uncheckedValue="false" />
    <span class="error" id="error_deleteOnCleanup"></span>
    <span class="smallNote">If checked artifacts will be deleted from Nexus when the corresponding build is removed from TeamCity</span>
  </td>
</tr>
<tr>
  <th>Fail build if publishing fails:<l:star/></th>
  <td>
    <props:checkboxProperty name="artifactUploadMandatory" uncheckedValue="false" />
    <span class="error" id="error_artifactUploadMandatory"></span>
    <span class="smallNote">If checked the build will be failed if artifacts cannot be uploaded to Nexus</span>
  </td>
</tr>
</l:settingsGroup>
