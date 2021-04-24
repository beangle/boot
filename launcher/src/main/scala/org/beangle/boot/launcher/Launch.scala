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

import org.beangle.commons.lang.Strings
import org.beangle.repo.artifact.BeangleResolver.{fetch, process}
import org.beangle.repo.artifact.{Layout, Repo}

import java.util.jar.JarFile

object Launch {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage:java org.beangle.boot.launcher.Launch /path/to/jarorwar")
      return
    }
    var remoteUrl = System.getenv("M2_REPO")
    if (Strings.isEmpty(remoteUrl)) {
      remoteUrl = Repo.Remote.CentralURL
    }
    val remoteRepo = new Repo.Remote("remote", remoteUrl, Layout.Maven2)
    val localRepo = new Repo.Local()
    fetch(args(0), remoteRepo, localRepo) foreach { a =>
      process(a, remoteRepo, localRepo)
      run(getMainClass(new JarFile(a)), args)
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

  private def run(mainClassName: String, args: Array[String]): Unit = {
    val mainClass = Class.forName(mainClassName, false, Thread.currentThread.getContextClassLoader)
    val mainMethod = mainClass.getDeclaredMethod("main", classOf[Array[String]])
    mainMethod.setAccessible(true)
    mainMethod.invoke(null, Array[AnyRef](args))
  }

}
