import org.beangle.parent.Dependencies.*
import org.beangle.parent.Settings.*

organization := "org.beangle.boot"
version := "0.1.29"

scmInfo := Some(
  ScmInfo(
    uri("https://github.com/beangle/boot"),
    "scm:git@github.com:beangle/boot.git"
  )
)

developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = uri("http://github.com/duantihua")
  )
)
description := "Beangle Boot ToolKit"
homepage := Some(uri("https://beangle.github.io/boot/index.html"))
val beangle_commons = "org.beangle.commons" % "beangle-commons" % "6.3.2"

lazy val root = (project in file("."))
  .settings(
    name := "beangle-boot",
    common,
    libraryDependencies ++= Seq(beangle_commons, apache_commons_compress),
    libraryDependencies ++= Seq(scalatest)
  )
