Beangle Boot toolkit

It provides a smart mechanism to bootstrap jar.Unlike other boot libraries which building flatjar,
beangle-boot just detects dependencies list in jarfile!/META-INF/beangle/dependencies.

The dependencies file contains many lines,each line discribe a artifact gav(group:artifact:version) info,
that corresponding jar can be fetched by maven repo.

## Generate dependencies file

Put this config fragment in your project plugin.sbt file.

<<<<<<< HEAD
    addSbtPlugin("org.beangle.build" % "sbt-beangle-build" % "0.0.12")
=======
    addSbtPlugin("org.beangle.build" % "sbt-beangle-build" % "0.0.14")
>>>>>>> develop

A executable jar should contain Main-Class in /META-INF/MANIFEST.MF.The configuration
can be appended by jar plugin.

    Compile / packageBin / packageOptions +=
    Package.ManifestAttributes(java.util.jar.Attributes.Name.MAIN_CLASS -> "org.your.main.class.name")

    Compile / compile := (Compile / compile).dependsOn(BootPlugin.generateDependenciesTask).value

 After that,use `sbt package` will generate a package which contains the `dependencies` file.

### Launch your jar

    wget https://github.com/beangle/boot/tree/main/src/main/scripts/launch.sh
    chmod +x launch.sh
    ./launch.sh /path/to/your/project.jar

   To append additional library to object jar's classpath,using format:

    ./launch.sh -cp "/path/to/lib/*" /path/to/your/project.jar

   Specify properties using -Dxx=vv

    ./launch.sh -cp "/path/to/lib/*" -Dk1=v1 -Dk2=v2  /path/to/your/project.jar arg1 arg2

   The difference with java command is that `-jar` option is ommited.

