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

package org.beangle.boot.dependency

import org.beangle.boot.artifact.*
import org.beangle.boot.downloader.{DefaultDownloader, Detector}
import org.beangle.commons.collection.Collections

import java.io.File
import java.net.URL

/**
 * 可以解析一个war，一个文本文件或者一个war解压后的文件夹，或者一个gav字符串
 */
object AppResolver {
  private val JarDependenciesFile = "/META-INF/beangle/dependencies"
  private val WarDependenciesFile = "/WEB-INF/classes" + JarDependenciesFile

  private val OldWarDependenciesFile = "/WEB-INF/classes/META-INF/beangle/container.dependencies"

  def isApp(file: String): Boolean = {
    file.endsWith(".war") || file.endsWith(".jar")
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage:java org.beangle.boot.dependency.AppResolver artifact_file remote_url local_base")
      return
    }
    var remote = Repo.Remote.CentralURL
    var local: String = null
    if (args.length > 1) remote = args(1)
    if (args.length > 2) local = args(2)

    val remoteRepo = new Repo.Remote("remote", remote, Layout.Maven2)
    val localRepo = new Repo.Local(local)
    val dest = fetch(args(0), remoteRepo, localRepo)
    var missingSize = 0
    dest foreach { a =>
      val (all, missing) = process(a, remoteRepo, localRepo)
      if (missing.nonEmpty) {
        missingSize = missing.size
        Console.err.println("Missing: " + missing.mkString(","))
      }
    }
    if (dest.isEmpty || missingSize > 0) {
      System.exit(1)
    } else {
      System.exit(0)
    }
  }

  /**
   * 可以解析一个war|jar，一个文本文件或者一个war解压后的文件夹
   */
  def resolveArchive(artifact: File): Iterable[Archive] = {
    val file = artifact.getAbsolutePath
    if (!artifact.exists) {
      println(s"Cannot find file $file")
      return List.empty
    }

    var url: URL = null
    if (isApp(file)) {
      val innerPath = if (file.endsWith(".war")) WarDependenciesFile else JarDependenciesFile
      val nestedUrl = new URL("jar:file:" + artifact.getAbsolutePath + "!" + innerPath)
      url = Detector.tryOpen(nestedUrl)
      if (null == url && file.endsWith(".war")) {
        url = Detector.tryOpen(new URL("jar:file:" + artifact.getAbsolutePath + "!" + OldWarDependenciesFile))
      }
    } else if (artifact.isDirectory) {
      val nestedFile = new File(file + WarDependenciesFile)
      if (nestedFile.isFile) {
        url = nestedFile.toURI.toURL
      }
    } else {
      url = artifact.toURI.toURL
    }
    if null == url then List.empty
    else DependencyResolver.resolve(url)
  }

  def fetch(file: String, remoteRepo: Repo.Remote, localRepo: Repo.Local, verbose: Boolean = true): Option[File] = {
    if (file.contains(":") && !file.contains("/") && !file.contains("\\")) {
      val war = Artifact(file)
      new ArtifactDownloader(remoteRepo, localRepo, verbose).download(List(war))
      if (!localRepo.exists(war)) {
        if (verbose) println("Cannot download:" + file)
        None
      } else {
        Some(new File(localRepo.file(war).getAbsolutePath))
      }
    } else if (isApp(file)) {
      val localFile = new File(file)
      if (localFile.exists) {
        Some(localFile)
      } else {
        if (verbose) println(s"Cannot find $file")
        None
      }
    } else {
      throw new RuntimeException("Cannot launch app " + file)
    }
  }

  def process(file: File, remoteRepo: Repo.Remote,
              localRepo: Repo.Local, verbose: Boolean = true): (Iterable[Archive], Iterable[Archive]) = {
    val archives = resolveArchive(file)
    val artifacts = Collections.newBuffer[Artifact]
    val missing = Collections.newBuffer[Archive]

    archives foreach {
      case a: Artifact => artifacts += a
      case lf@LocalFile(n) => if (!new File(n).exists) missing += lf
      case rf@RemoteFile(u) =>
        val localFile = rf.local(localRepo)
        new DefaultDownloader("default", new URL(u), localFile).start()
        if !localFile.exists() then missing += rf
      case _ =>
    }

    new ArtifactDownloader(remoteRepo, localRepo, verbose).download(artifacts)
    missing ++= archives.filter { x =>
      x match {
        case a: Artifact => !localRepo.file(a).exists
        case _ => false
      }
    }
    (archives, missing)
  }

}
