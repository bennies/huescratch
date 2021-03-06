# Play with the lights (Hue) using scratch.

## Using it

When it starts it will try to discover the bridge automatically and will request a userid from the bridge (You'll need to push that
large button on the bridge). So normally this would be fine:

```
java -Xmx64m -jar target/HueScratch-0.0.1.jar
```

You can however start with an application.properties file pointing to the bridge or add it on the commandline like this:

```
java -Xmx64m -jar target/HueScratch-0.0.1.jar --hue.bridge="http://192.168.20.114/"
```
It will try and connect to the bridge, on success will try and get a userid (require you to push the big button on the bridge)
, on success write both settings to an application.properties file so next time you run it you won't need to go through
this process.

## In scratch
You'll need the offline version of scratch. To load this module hit shift while clicking on file in the menu.
This will show you a new option "Import experimental HTTP extension", click this and point to the "huescratch.s2e file.
After this you can find the new hue blocks under the "More Blocks" section.

![scratch_hue_example](scratch_hue_example.png)

## Build

### How to create a jar
```
mvn package spring-boot:repackage
```
The jar can be found at: target/HueScratch-0.0.1.jar

### Prerequisite
Java 1.7+
Maven 3.2+
