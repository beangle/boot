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
import org.beangle.commons.io.Files

import java.io.File

/** 解析 artifact 依赖，将本地仓库中的 jar 集中复制到 --local 指定的仓库目录。 */
object Repo {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage:java org.beangle.boot.launcher.Repo artifact_file [--local=local_base] [--source=source_base] [--quiet]")
      System.exit(-1)
    }
    val artifactPath = args(0)
    var local: String = null
    var source: String = null
    var verbose = true
    args foreach { arg =>
      if arg.startsWith("--local=") then
        local = arg.substring("--local=".length).trim
      else if arg.startsWith("--source=") then
        source = arg.substring("--source=".length).trim
      else if arg == "--quiet" then
        verbose = false
    }
    val artifact = new File(artifactPath)
    if !artifact.exists() then
      if verbose then Console.err.println(s"Cannot find $artifactPath")
      System.exit(-1)

    val localRepo = new MavenRepo.Local(local)
    val sourceRepo = new MavenRepo.Local(source)
    if sameDir(sourceRepo.base, localRepo.base) then
      if verbose then Console.err.println(s"Source and local repo cannot be the same: ${localRepo.base}")
      System.exit(-1)

    val archives = resolveArchive(artifact)
    val missing = consolidate(archives, sourceRepo, localRepo)
    if (missing.nonEmpty) {
      if verbose then Console.err.println("Missing: " + missing.mkString(","))
      System.exit(-1)
    } else {
      print(localRepo.base)
      System.exit(0)
    }
  }

  private def sameDir(a: String, b: String): Boolean = {
    new File(a).getCanonicalFile == new File(b).getCanonicalFile
  }

  private def consolidate(archives: Iterable[Archive], sourceRepo: MavenRepo.Local,
                          localRepo: MavenRepo.Local): Iterable[Archive] = {
    val artifacts = Collections.newBuffer[Artifact]
    val missing = Collections.newBuffer[Archive]

    archives foreach {
      case a: Artifact => artifacts += a
      case _ =>
    }

    artifacts foreach { artifact =>
      if !localRepo.exists(artifact) && !copyFromSource(artifact, sourceRepo, localRepo) then
        missing += artifact
    }
    missing
  }

  private def copyFromSource(artifact: Artifact, source: MavenRepo.Local, target: MavenRepo.Local): Boolean = {
    if !source.exists(artifact) then return false
    copyRepoFile(source.file(artifact), target.file(artifact))
    val sha1 = artifact.sha1
    if source.exists(sha1) then copyRepoFile(source.file(sha1), target.file(sha1))
    true
  }

  private def copyRepoFile(src: File, dest: File): Unit = {
    dest.getParentFile.mkdirs()
    Files.copy(src, dest)
  }
}
