/*
 * Beangle, Agile Development Scaffold and Toolkits.
 *
 * Copyright © 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.boot.launcher

import org.beangle.boot.artifact.AppResolver.{fetch, resolveArtifact}
import org.beangle.boot.artifact.{Layout, Repo}

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
        val dependencies = resolveArtifact(a)
        val entries = a.getAbsolutePath :: dependencies.map(x => localRepo.file(x)).toList
        print(getMainClass(new JarFile(a)) + "@" + entries.mkString(File.pathSeparator))
        System.exit(0)
      case None => System.exit(-1)
    }
  }

  private def getMainClass(jarFile: JarFile): String = {
    val manifest = jarFile.getManifest
    var mainClass: String = null
    if (manifest != null) mainClass = manifest.getMainAttributes.getValue("Main-Class")
    if (mainClass == null) throw new IllegalStateException("No 'Main-Class' manifest entry specified in " + this)
    jarFile.close()
    mainClass
  }
}
