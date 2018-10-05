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
package org.beangle.repo.artifact

import java.io.{ File, InputStreamReader, LineNumberReader }
import java.net.URL

import org.beangle.commons.collection.Collections

trait DependencyResolver {
  def resolve(resource: URL): Iterable[Artifact]
}

object BeangleResolver extends DependencyResolver {

  val DependenciesFile = "META-INF/beangle/container.dependencies"

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage:java org.beangle.repo.artifact.BeangleResolver dependency_file remote_url local_base")
      return
    }
    val dependencyFile = new File(args(0))
    if (!dependencyFile.exists) {
      println(s"Cannot find ${args(0)}")
      return
    }
    val url: URL =
      if (args(0).endsWith(".war")) {
        val nestedUrl = new URL("jar:file:" + dependencyFile.getAbsolutePath + "!/WEB-INF/classes/META-INF/beangle/container.dependencies")
        try {
          nestedUrl.openConnection.connect()
          nestedUrl
        } catch {
          case e: Throwable =>
            println("Resolving aborted,cannot find META-INF/beangle/container.dependencies.")
            return
        }
      } else {
        dependencyFile.toURI().toURL()
      }
    var remote = Repo.Remote.AliyunURL
    var local: String = null
    if (args.length > 1) remote = args(1)
    if (args.length > 2) local = args(2)
    val artifacts = resolve(url)
    val remoteRepo = new Repo.Remote("remote", remote, Layout.Maven2)
    val localRepo = new Repo.Local(local)
    new ArtifactDownloader(remoteRepo, localRepo).download(artifacts)

    val missing = artifacts filter (!localRepo.exists(_))
    if (!missing.isEmpty) {
      println("Download error :" + missing)
    }
  }

  override def resolve(resource: URL): Iterable[Artifact] = {
    val artifacts = Collections.newBuffer[Artifact]
    if (null == resource) return List.empty
    try {
      val reader = new InputStreamReader(resource.openStream())
      val lr = new LineNumberReader(reader)
      var line: String = null
      do {
        line = lr.readLine()
        if (line != null && !line.isEmpty) {
          val infos = line.split(":")
          artifacts += new Artifact(infos(0), infos(1), infos(2))
        }
      } while (line != null);
      lr.close()
    } catch {
      case e: Exception => e.printStackTrace()
    }
    artifacts
  }
}
