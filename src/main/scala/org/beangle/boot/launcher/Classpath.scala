/*
 * Copyright (C) 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.beangle.boot.launcher

import org.beangle.boot.artifact.*
import org.beangle.boot.artifact.Repo as MavenRepo
import org.beangle.boot.dependency.AppResolver.resolveArchive
import org.beangle.commons.collection.Collections
import org.beangle.commons.io.Dirs
import org.beangle.commons.lang.Strings

import java.io.File
import java.util.jar.JarFile

object Classpath {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage:java org.beangle.boot.launcher.Classpath artifact_file [--local=local_base]")
      System.exit(-1)
    }
    val artifactPath = args(0)
    var local: String = null
    args foreach { arg =>
      if arg.startsWith("--local=") then
        local = arg.substring("--local=".length).trim
    }
    val artifact = new File(artifactPath)
    if !artifact.exists() then
      Console.err.println(s"Cannot find $artifactPath")
      System.exit(-1)

    val localRepo = new MavenRepo.Local(local)
    val archives = resolveArchive(artifact)
    val missing = Collections.newBuffer[Artifact]
    archives foreach {
      case a: Artifact => if !localRepo.exists(a) then missing += a
      case _ =>
    }
    if (missing.nonEmpty) {
      Console.err.println("Missing: " + missing.mkString(","))
      System.exit(-1)
    }

    val paths = Collections.newBuffer[String]
    //如果是jar包,则把该包放到classpath中
    if (artifact.isFile && artifact.getName.endsWith(".jar")) {
      paths += artifact.getAbsolutePath
    } else if (artifact.isDirectory) {
      //添加war包的依赖
      val warClasses = artifact.getAbsolutePath + "/WEB-INF/classes"
      if (new File(warClasses).exists()) {
        paths += warClasses.replace('/', File.separatorChar)
      }
      val warLib = artifact.getAbsolutePath + "/WEB-INF/lib"
      if (new File(warLib).exists()) {
        Dirs.on(warLib).ls() foreach { lib =>
          if (lib.endsWith(".jar")) {
            paths += (warLib + "/" + lib).replace('/', File.separatorChar)
          }
        }
      }
    }
    archives foreach {
      case a: Artifact => paths += localRepo.file(a).getAbsolutePath
      case LocalFile(n) => paths += n
      case rf: RemoteFile => paths += rf.local(localRepo).getAbsolutePath
      case _ =>
    }
    var extra = System.getenv("classpath_extra")
    if Strings.isEmpty(extra) then extra = System.getenv("CLASSPATH_EXTRA")
    if Strings.isNotEmpty(extra) then paths.prepend(extra)

    if (artifact.isDirectory) {
      print("none@" + paths.mkString(File.pathSeparator))
    } else {
      print(getMainClass(new JarFile(artifact)) + "@" + paths.mkString(File.pathSeparator))
    }
    System.exit(0)
  }

  private def getMainClass(jarFile: JarFile): String = {
    val manifest = jarFile.getManifest
    var mainClass = ""
    if (null != manifest) {
      mainClass = manifest.getMainAttributes.getValue("Main-Class")
    }
    if (Strings.isBlank(mainClass)) mainClass = "none"
    jarFile.close()
    mainClass
  }
}
