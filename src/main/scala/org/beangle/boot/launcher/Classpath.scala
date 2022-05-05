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
import org.beangle.boot.dependency.AppResolver.{fetch, resolveArchive}
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings

import java.io.File
import java.util.jar.JarFile

object Classpath {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage:java org.beangle.boot.launcher.Classpath artifact_file [--remote=remote_url] [--local=local_base]")
      System.exit(-1)
    }
    val artifactURI = args(0)
    var local: String = null
    var remote = Repo.Remote.CentralURL
    args foreach { arg =>
      if arg.startsWith("--remote=") then
        remote = arg.substring("--remote=".length).trim
      else if arg.startsWith("--local=") then
        local = arg.substring("--local=".length).trim
    }
    val remoteRepo = new Repo.Remote("remote", remote, Layout.Maven2)
    val localRepo = new Repo.Local(local)

    fetch(artifactURI, remoteRepo, localRepo, false) match {
      case Some(a) =>
        val archives = resolveArchive(a)
        val paths = Collections.newBuffer[String]
        paths += a.getAbsolutePath
        archives foreach {
          case a: Artifact => paths += localRepo.file(a).getAbsolutePath
          case LocalFile(n) => paths += n
          case rf: RemoteFile => paths += rf.local(localRepo).getAbsolutePath
          case _ =>
        }
        val extra = System.getenv("classpath_extra")
        if (Strings.isNotEmpty(extra)) {
          paths.prepend(extra)
        }
        print(getMainClass(new JarFile(a)) + "@" + paths.mkString(File.pathSeparator))
        System.exit(0)
      case None => System.exit(-1)
    }
  }

  private def getMainClass(jarFile: JarFile): String = {
    val manifest = jarFile.getManifest
    var mainClass = ""
    if (null != manifest) {
      mainClass = manifest.getMainAttributes.getValue("Main-Class")
    }
    if (Strings.isBlank(mainClass)) mainClass = "cannot.find.mainclass"
    jarFile.close()
    mainClass
  }
}
