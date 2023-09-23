# Overview

Before we create a new version we have to consider following changes:

Change all SNAPSHOT version in files
* CHANGELOG.md
* gradle.properties
* Change version in the 'src/main/izpack/install.xml' to the current release version
* Change version in the about or info dialog to the current release version (/src/main/resources/ui/messages.properties)
* Change default value for the version in the about or info dialog in the class 
io.github.astrapi69.mystic.crypt.DesktopMenu in callback method onNewInfoDialog#InfoDialog#newInfoPanel

Then you can follow the instructions from the [wiki izpack section](https://github.com/astrapi69/mystic-crypt-ui/wiki/How-to-create-izpack-installer-with-gradle)
