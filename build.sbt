import org.beangle.parent.Dependencies.*
import org.beangle.parent.Settings.*

ThisBuild / organization := "org.beangle.boot"
ThisBuild / version := "0.1.9"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/beangle/boot"),
    "scm:git@github.com:beangle/boot.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = url("http://github.com/duantihua")
  )
)
val beangle_common_ver = "5.6.11"
ThisBuild / description := "Beangle Boot ToolKit"
ThisBuild / homepage := Some(url("https://beangle.github.io/boot/index.html"))
val beangle_commons_core = "org.beangle.commons" %% "beangle-commons-core" % beangle_common_ver
val beangle_commons_file = "org.beangle.commons" %% "beangle-commons-file" % beangle_common_ver

lazy val root = (project in file("."))
  .settings(
    name := "beangle-boot",
    common,
    libraryDependencies ++= Seq(beangle_commons_core, beangle_commons_file, apache_commons_compress),
    libraryDependencies ++= Seq(logback_classic % "test", logback_core % "test", scalatest)
  )
