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

import java.net.URL
import java.io.InputStreamReader
import java.io.LineNumberReader
import org.beangle.commons.collection.Collections
import java.io.File
import java.rmi.Remote

trait DependencyResolver {

  def resolve(resource: URL): Iterable[Artifact]
}

object BeangleResolver {

  val DependenciesFile = "META-INF/beangle/container.dependencies"

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage:java org.beangle.repo.artifact.BeangleResolver dependency_file remote_url local_base")
      return
    }
    val dependencyFile = new File(args(0))
    var remote = Repo.Remote.AliyunURL
    var local: String = null
    if (args.length > 1) remote = args(1)
    if (args.length > 2) local = args(2)
    val resolver = new BeangleResolver()
    val artifacts = resolver.resolve(dependencyFile.toURI().toURL())
    val remoteRepo = new Repo.Remote("remote", remote, Layout.Maven2)
    val localRepo = new Repo.Local(local)
    new ArtifactDownloader(remoteRepo, localRepo).download(artifacts)
  }
}

class BeangleResolver extends DependencyResolver {

  override def resolve(resource: URL): Iterable[Artifact] = {
    val artifacts = Collections.newBuffer[Artifact]
    if (null == resource) return Array.ofDim[Artifact](0)
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
