Beangle Boot toolkit

It provides a smart mechanism to bootstrap jar.Unlike other boot libraries which building flatjar,
beangle-boot just detects dependencies list file in jarfile!/META-INF/beangle/dependencies.

The dependencies file contains many lines,each line discribe a artifact gav(group:artifact:version) info,
that corresponding jar can be fetched by maven repo.

## Generate dependencies file

Put this configurate fragment in your project pom file.

    <plugin>
        <groupId>org.beangle.maven</groupId>
        <artifactId>beangle-maven-plugin</artifactId>
        <version>0.3.31</version>
        <executions>
          <execution>
            <id>generate</id>
            <phase>prepare-package</phase>
            <goals>
              <goal>dependencies</goal>
            </goals>
          </execution>
        </executions>
      </plugin>


A executable jar should contain Main-Class in /META-INF/MANIFEST.MF.The configuration
can be appended by jar plugin.

      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
          <archive>
            <manifest>
              <mainClass>org.packege.to.your.MainClass</mainClass>
            </manifest>
          </archive>
        </configuration>
      </plugin>

 After that,use `mvn package` will generate a package which contains the `dependencies` file.

### Launch your jar

    wget https://github.com/beangle/boot/tree/main/src/main/scripts/launch.sh
    chmod +x launch.sh
    ./launch.sh /path/to/your/project.jar

   To append additional library to object jar's classpath,using format:

    ./launch.sh -cp "/path/to/lib/*" /path/to/your/project.jar

   Specify properties using -Dxx=vv

    ./launch.sh -cp "/path/to/lib/*" -Dk1=v1 -Dk2=v2  /path/to/your/project.jar arg1 arg2

   The difference with java command is that `-jar` option is ommited.

