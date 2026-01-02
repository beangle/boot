Beangle Boot Toolkit

## 功能和原理
他是个支持瘦jar包的启动工具。首先他会解析Jar包中的依赖文件(/META-INF/beangle/dependencies)，并通过CLASSPATH暴露出来，从而支持进行快速启动。
这个依赖文件包含很多行，每行描述一个外部依赖，形式采用gav(group:artifact:version)格式，例如：

    com.zaxxer:HikariCP:7.0.2
    org.slf4j:slf4j-api:2.0.17
    ch.qos.logback:logback-classic:1.5.20
    ch.qos.logback:logback-core:1.5.20

这些外部依赖，确保可以从Maven仓库（或者指定仓库）上下载。Boot Toolkit会在用户目录下（~/.m2/repository）检测相应文件是否存在，
必要时会下载jar文件和对应的sha1摘要文件。所有外部依赖下载后，他会将这些依赖的路径通过CLASSPATH环境变量暴露出来，然后调用Jar中的Main-Class
启动Java进程。

## 一、Maven工程中生成依赖文件

如果使用Maven构建项目，可以只通过插件在打包的时候生成这个依赖文件，省去手工编辑。

    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.4.2</version>
        <configuration>
          <archive>
            <manifest>
              <mainClass>com.yourcompany.project.MainClass</mainClass>
            </manifest>
          </archive>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.beangle.maven</groupId>
        <artifactId>beangle-maven-plugin</artifactId>
        <version>0.3.32</version>
        <executions>
          <execution>
            <id>generate</id>
            <phase>compile</phase>
            <goals>
              <goal>dependencies</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>

在工程的pom.xml中的plugins中添加这两个插件，并配置工程的入口类`com.yourcompany.project.MainClass`，这样在打包时就可以生成依赖文件。

## 二、Sbt工程中生成依赖文件

将如下代码片段放在工程的project/plugin.sbt文件中。

    addSbtPlugin("org.beangle.build" % "sbt-beangle-build" % "0.0.15")

然后在build.sbt文件中配置入口类，并激活BootPlugin。

    Compile / packageBin / packageOptions +=
    Package.ManifestAttributes(java.util.jar.Attributes.Name.MAIN_CLASS -> "org.your.main.class.name")

    Compile / compile := (Compile / compile).dependsOn(BootPlugin.generateDependenciesTask).value

之后执行`sbt package` 将会产生`dependencies`文件。

### 三、启动jar包

使用该工具启动jar的简便办法，执行如下命令：

    curl -o launch.sh https://raw.githubusercontent.com/beangle/boot/refs/heads/main/src/main/scripts/launch.sh
    chmod +x launch.sh
    ./launch.sh /path/to/your/project.jar

如果需要添加额外的依赖库，可以使用-cp参数，也可以添加其他JVM参数（-Dk1=v1 -Dk2=v2),类似如下：

    ./launch.sh -cp "/path/to/lib/*" /path/to/your/project.jar
    ./launch.sh -cp "/path/to/lib/*" -Dk1=v1 -Dk2=v2  /path/to/your/project.jar arg1 arg2

这个工具还支持直接使用gav形式的描述，直接运行maven仓库中的jar包，例如

    ./launch.sh org.beangle.sqlplus:beangle-sqlplus:0.0.46 db.xml

### 四、启动war

  如果项目是个部署在tomcat中的war包，没有Main-Class的属性，日常运行的时候是直接放在容器中，不是可执行的jar包。
那么可以使用工具包中的sas.sh脚本，直接运行该包。

    # 下载可执行文件
    curl -o sas.sh https://raw.githubusercontent.com/beangle/boot/refs/heads/main/src/main/scripts/sas.sh
    chmod +x sas.sh

    # 直接执行这个war
    ./sas.sh org.beangle.otk:beangle-otk-ws:0.0.22 --path=/tools
    # 或者指定端口
    ./sas.sh /path/to/beangle-otk-ws-0.0.22.jar --path=/tools --port=8083 --engine=undertow

### 五、其他用法

如果想在启动过程中，进行灵活定制，可以使用工程的resolve.sh脚本，它可以解析出jar对应的Main-Class和CLASSPATH两个环境变量，
这样我们可以插入到自己的脚本中。例如，定制一个自己的start.sh：

    #!/bin/bash

    #下载resolve.sh
    curl -o resolve.sh https://raw.githubusercontent.com/beangle/boot/refs/heads/main/src/main/scripts/resolve.sh
    chmod +x resolve.sh

    # 同shell中直接执行resolve.sh，他会暴露MAIN-CLASS and CLASSPATH
    source ./resolve.sh /path/to/myproject.jar
    # do some log rolling
    # do more custome things.

    # 最后执行Java
    java -Xmx1G $MAIN_CLASS


