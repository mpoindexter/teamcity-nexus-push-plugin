
TeamCity plugin to push artifacts to Nexus.  Supports automatically cleaning up artifacts when the TeamCity build is cleaned up, and reports on the artifacts pushed to Nexus.

 1. Build
 Issue 'mvn package' command from the root project to build your plugin. Resulting package <artifactId>.zip will be placed in 'target' directory. 
 
 2. Install
 To install the plugin, put zip archive to 'plugins' dir under TeamCity data directory and restart the server.

 
