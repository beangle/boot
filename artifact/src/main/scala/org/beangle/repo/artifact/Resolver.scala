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

import org.beangle.commons.collection.Collections

import java.io.{File, InputStreamReader, LineNumberReader}
import java.net.URL

trait DependencyResolver {
  def resolve(resource: URL): Iterable[Artifact]
}

/**
 * 可以解析一个war，一个文本文件或者一个war解压后的文件夹，或者一个gav字符串
 */
object BeangleResolver extends DependencyResolver {

  val DependenciesFile = "META-INF/beangle/container.dependencies"

  private val WarDependenciesFile = "/WEB-INF/classes/" + DependenciesFile

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage:java org.beangle.repo.artifact.BeangleResolver dependency_file remote_url local_base")
      return
    }
    var remote = Repo.Remote.CentralURL
    var local: String = null
    if (args.length > 1) remote = args(1)
    if (args.length > 2) local = args(2)

    val remoteRepo = new Repo.Remote("remote", remote, Layout.Maven2)
    val localRepo = new Repo.Local(local)
    fetch(args(0), remoteRepo, localRepo) foreach { a =>
      process(a, remoteRepo, localRepo)
    }
  }

  /**
   * 可以解析一个war|jar，一个文本文件或者一个war解压后的文件夹
   */
  def resolveArtifact(artifact: File): Iterable[Artifact] = {
    val file = artifact.getAbsolutePath
    if (!artifact.exists) {
      println(s"Cannot find $file")
      return List.empty
    }

    var url: URL = null
    if (file.endsWith(".war") || file.endsWith(".jar")) {
      val nestedUrl = new URL("jar:file:" + artifact.getAbsolutePath + "!" + WarDependenciesFile)
      try {
        nestedUrl.openConnection.connect()
        url = nestedUrl
      } catch {
        case _: Throwable =>
      }
    } else if (artifact.isDirectory) {
      val nestedFile = new File(file + WarDependenciesFile)
      if (nestedFile.isFile) {
        url = nestedFile.toURI.toURL
      }
    } else {
      url = artifact.toURI.toURL
    }
    if (null == url) {
      List.empty
    } else {
      resolve(url)
    }
  }

  def fetch(file: String, remoteRepo: Repo.Remote, localRepo: Repo.Local): Option[File] = {
    if (file.contains(":") && !file.contains("/") && !file.contains("\\")) {
      val war = Artifact(file).packaging("war")
      new ArtifactDownloader(remoteRepo, localRepo).download(List(war))
      if (!localRepo.exists(war)) {
        println("Download error:" + file)
        None
      } else {
        Some(new File(localRepo.file(war).getAbsolutePath))
      }
    } else {
      val localFile = new File(file)
      if (localFile.exists) {
        Some(localFile)
      } else {
        println(s"Cannot find $file")
        None
      }
    }
  }

  def process(file: File, remoteRepo: Repo.Remote, localRepo: Repo.Local): Boolean = {
    val artifacts = resolveArtifact(file)
    new ArtifactDownloader(remoteRepo, localRepo).download(artifacts)
    val missing = artifacts filter (!localRepo.exists(_))
    if (missing.nonEmpty) {
      println("Download error :" + missing)
      false
    } else {
      true
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
      } while (line != null)
      lr.close()
    } catch {
      case e: Exception => e.printStackTrace()
    }
    artifacts
  }
}
