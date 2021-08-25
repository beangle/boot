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

import org.beangle.boot.artifact._
import org.beangle.boot.dependency.AppResolver.{fetch, resolveArchive}
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings

import java.io.File
import java.util.jar.JarFile

object Classpath {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      System.exit(-1)
    }
    var local: String = null
    if (args.length > 1) local = args(1)
    val remoteRepo = new Repo.Remote("remote", Repo.Remote.CentralURL, Layout.Maven2)
    val localRepo = new Repo.Local(local)

    fetch(args(0), remoteRepo, localRepo, false) match {
      case Some(a) =>
        val archives = resolveArchive(a)
        val paths = Collections.newBuffer[String]
        paths += a.getAbsolutePath
        archives foreach {
          case a: Artifact => paths += localRepo.file(a).getAbsolutePath
          case LocalFile(n) => paths += n
          case rf: RemoteFile => paths += rf.local(localRepo).getAbsolutePath
          case d: Diff =>
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
