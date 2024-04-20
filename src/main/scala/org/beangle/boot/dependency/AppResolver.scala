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
import org.beangle.commons.net.Networks

import java.io.File
import java.net.URL

/**
 * 可以解析一个war，一个文本文件或者一个war解压后的文件夹，或者一个gav字符串
 */
object AppResolver {
  private val JarDependenciesFile = "/META-INF/beangle/dependencies"
  private val WarDependenciesFile = "/WEB-INF/classes" + JarDependenciesFile

  private val OldWarDependenciesFile = "/WEB-INF/classes/META-INF/beangle/container.dependencies"

  private var verbose = true

  /** 解析一个war或者jar文件，如果成功，则输出这个文件的绝对地址。
   *
   * @param args
   */
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage:java org.beangle.boot.dependency.AppResolver artifact_file [--remote=remote_url] [--local=local_base] [--quiet]")
      return
    }
    val artifactURI = args(0)
    var remote = Seq(Repo.Remote.AliyunURL, Repo.Remote.HuaweiCloudURL, Repo.Remote.CentralURL).mkString(",")
    var local: String = null

    args foreach { arg =>
      if arg.startsWith("--remote=") then
        remote = arg.substring("--remote=".length).trim
      else if arg.startsWith("--local=") then
        local = arg.substring("--local=".length).trim
      else if arg == "--quiet" then
        verbose = false
    }
    if !remote.contains(Repo.Remote.CentralURL) then remote += "," + Repo.Remote.CentralURL
    val remoteRepos = Repo.remotes(remote)
    val localRepo = new Repo.Local(local)
    val dest = fetch(artifactURI, remoteRepos, localRepo, verbose)
    var missingSize = 0
    dest foreach { a =>
      val (all, missing) = process(a, remoteRepos, localRepo)
      if (missing.nonEmpty) {
        missingSize = missing.size
        if verbose then Console.err.println("Missing: " + missing.mkString(","))
      }
    }
    System.exit(if dest.isEmpty || missingSize > 0 then 1 else 0)
  }

  def fetch(file: String, remoteRepos: Seq[Repo.Remote], localRepo: Repo.Local, verbose: Boolean = true): Option[File] = {
    if (file.contains(":") && !file.contains("/") && !file.contains("\\")) {
      val war = Artifact(file)
      new ArtifactDownloader(remoteRepos, localRepo, verbose).download(List(war))
      if !localRepo.exists(war) then error("Cannot download:" + file)
      else Some(new File(localRepo.file(war).getAbsolutePath))
    } else if (isApp(file)) {
      val localFile = new File(file)
      if localFile.exists then Some(localFile)
      else error(s"Cannot find $file")
    } else {
      throw new RuntimeException("Cannot launch app " + file)
    }
  }

  def isApp(file: String): Boolean = {
    file.endsWith(".war") || file.endsWith(".jar")
  }

  private def error(msg: String): Option[File] = {
    if verbose then println(msg)
    None
  }

  def process(file: File, remoteRepos: Seq[Repo.Remote],
              localRepo: Repo.Local, verbose: Boolean = true): (Iterable[Archive], Iterable[Archive]) = {
    val archives = resolveArchive(file)
    val artifacts = Collections.newBuffer[Artifact]
    val missing = Collections.newBuffer[Archive]

    archives foreach {
      case a: Artifact => artifacts += a
      case lf@LocalFile(n) => if (!new File(n).exists) missing += lf
      case rf@RemoteFile(u) =>
        val localFile = rf.local(localRepo)
        new DefaultDownloader("default", Networks.url(u), localFile).start()
        if !localFile.exists() then missing += rf
      case _ =>
    }

    new ArtifactDownloader(remoteRepos, localRepo, verbose).download(artifacts)
    missing ++= archives.filter { x =>
      x match {
        case a: Artifact => !localRepo.file(a).exists
        case _ => false
      }
    }
    (archives, missing)
  }

  /**
   * 可以解析一个war|jar，一个文本文件或者一个war解压后的文件夹
   */
  def resolveArchive(artifact: File): Iterable[Archive] = {
    val file = artifact.getAbsolutePath
    if (!artifact.exists) {
      if verbose then println(s"Cannot find file $file")
      return List.empty
    }

    var url: URL = null
    if (isApp(file)) {
      val innerPath = if (file.endsWith(".war")) WarDependenciesFile else JarDependenciesFile
      url = Networks.tryOpen("jar:file:" + artifact.getAbsolutePath + "!" + innerPath).orNull
      if (null == url && file.endsWith(".war")) {
        url = Networks.tryOpen("jar:file:" + artifact.getAbsolutePath + "!" + OldWarDependenciesFile).orNull
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
}
