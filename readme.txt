
TeamCity plugin to push artifacts to Nexus.  Supports automatically cleaning up artifacts when the TeamCity build is cleaned up, and reports on the artifacts pushed to Nexus.

 1. Build
 Issue 'mvn package' command from the root project to build your plugin. Resulting package <artifactId>.zip will be placed in 'target' directory. 
 
 2. Install
 To install the plugin, put zip archive to 'plugins' dir under TeamCity data directory and restart the server.

 
 3. Config
 In TeamCity - (Administration/Nexus Servers) press "Create new Nexus Server configuration" and enter your Nexus server url, login. password

 4. Use
 In TeamСity - Оpen your Edit Configuration Settings in your project in section Build Features press (Add build feature) and select "Nexus artifact publisher"
 Repository ID - Set your Repository name in Nexus sever
 Artifact Upload Parameters - Set your artifact param:
 ----------------EXAMPLE------------------------
raw.directory=TC_plugin/
raw.asset1=@target/nexus-push-plugin.zip
raw.asset1.filename=nexus-push-plugin.zip
-------------------END--------------------------
 More config read: https://help.sonatype.com/repomanager3/rest-and-integration-api/components-api
