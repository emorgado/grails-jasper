# Grails Jasper Plugin
[![Build Status](https://api.travis-ci.org/candrews/grails-jasper.png)](http://travis-ci.org/candrews/grails-jasper)

# Usage

For usage information, please see http://grails.org/plugin/jasper

# How to build this plugin

run 'grailsw package-plugin'

It will create a plugin archive.

# Releasing a new version of this plugin

1. In JasperGrailsPlugin.groovy, set the plugin version to a non-snapshot version number (ex 1.6.1)
2. run 'grailsw publish-plugin --stacktrace'
3. Update the plugin version in JasperGrailsPlugin.groovy to be a snapshot (ex "1.6.2-SNAPSHOT")

# RUn a plugin

./grailsw run-app -Dserver.port=8090


# Credentials

To easy the deploy I put the credentials on **sudo vim /etc/profile.d/archiva.sh**

# Deploy

grails publish-plugin --repository=velumsoft

Remember to change the plugin version on **GrailsJasperPlugin.groovy**