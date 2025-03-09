# Introduction
This repository houses all of our Minecraft plugins.

## Local hosting
In order to test plugins, they must be added to a Maven profile. You can find the available profiles in the `pom.xml`. To mount all of the plugins of a specific profile on your local dev server you can run:

```
mvn install -P <profile name>
```

The plugins should be automatically added to your dev server. You can use VSCode's run configuration by pressing F5 to start the server, or navigate to the `/_server` directory and run the `start.bat` file manually.

## Updating Paper
To update Paper, simply navigate put an updated `paper.jar` at the root directory. This jar will be copied to the dev server when running `mvn install`.