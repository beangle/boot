import org.beangle.parent.Dependencies._
import org.beangle.parent.Settings._

ThisBuild / organization := "org.beangle.boot"
ThisBuild / version := "0.0.27"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/beangle/boot"),
    "scm:git@github.com:beangle/boot.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id    = "chaostone",
    name  = "Tihua Duan",
    email = "duantihua@gmail.com",
    url   = url("http://github.com/duantihua")
  )
)

ThisBuild / description := "Beangle Boot ToolKit"
ThisBuild / homepage := Some(url("https://beangle.github.io/boot/index.html"))
val beangle_commons_core = "org.beangle.commons" %% "beangle-commons-core" % "5.2.9"
val beangle_commons_file = "org.beangle.commons" %% "beangle-commons-file" % "5.2.9"

lazy val root = (project in file("."))
  .settings(
    name := "beangle-boot",
    common,
    libraryDependencies ++=   Seq(scalatest,beangle_commons_core,beangle_commons_file,apache_commons_compress)
  )