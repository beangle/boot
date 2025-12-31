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

package org.beangle.boot.artifact

import org.beangle.boot.artifact.ArtifactDownloader.*
import org.beangle.boot.artifact.util.{Delta, FileSize}
import org.beangle.boot.downloader.{Downloader, RangeDownloader}
import org.beangle.commons.codec.binary.Base64
import org.beangle.commons.collection.Collections

import java.io.IOException
import java.net.URL
import java.util.concurrent.{ConcurrentHashMap, ExecutorService, Executors}
import scala.jdk.javaapi.CollectionConverters.asScala

/**
 * ArtifactDownloader
 * <p>
 * Support Features
 * <li>1. Display download processes
 * <li>2. Multiple thread downloading
 * <li>3. Detect resource status before downloading
 * </p>
 */
object ArtifactDownloader {
  def apply(remote: String, base: String = null, verbose: Boolean = true): ArtifactDownloader = {
    new ArtifactDownloader(Repo.remotes(remote), Repo.local(base), verbose)
  }

  val DiffSupports = Set("zip", "war", "ear")
}

class ArtifactDownloader(private val remotes: Seq[Repo.Remote], private val local: Repo.Local, var verbose: Boolean) {

  private val properties = Collections.newMap[String, String]

  var preferDiff: Boolean = false
  var maxthread: Int = 5

  def authorization(username: String, password: String): Unit = {
    properties.put("Authorization", "Basic " + Base64.encode(s"$username:$password".getBytes))
  }

  def download(artifacts: Iterable[Artifact]): Unit = {
    val statuses = new ConcurrentHashMap[URL, Downloader]
    val executor = Executors.newFixedThreadPool(maxthread)

    val sha1s = new collection.mutable.ArrayBuffer[Artifact]
    val diffs = new collection.mutable.ArrayBuffer[Diff]

    for (artifact <- artifacts if !artifact.isSnapshot) {
      if (!artifact.packaging.endsWith("sha1")) {
        val sha1 = artifact.sha1
        if !local.file(sha1).exists() then sha1s += sha1
      }
      if (!local.file(artifact).exists && preferDiff && DiffSupports.contains(artifact.packaging)) {
        local.latestBefore(artifact) foreach { latest =>
          diffs += Diff(latest, artifact.version)
        }
      }
    }
    // download sha1s
    doDownload(sha1s, executor, statuses)
    if (preferDiff) {
      // download diffs and patch them.
      doDownload(diffs, executor, statuses)
      for (diff <- diffs) {
        val diffFile = local.file(diff)
        if (diffFile.exists) {
          logInfo("Patching " + diff)
          Delta.patch(local.url(diff.older), local.url(diff.newer), local.url(diff))
        }
      }
    }

    val newers = new collection.mutable.ArrayBuffer[Artifact]
    // check it,last time.
    for (artifact <- artifacts) {
      if (needDownload(local, artifact)) {
        newers += artifact
      }
    }
    doDownload(newers, executor, statuses)
    // verify sha1 against newer artifacts.
    for (artifact <- newers) {
      needDownload(local, artifact)
    }
    executor.shutdown()
  }

  private def needDownload(local: Repo.Local, artifact: Artifact): Boolean = {
    if (local.file(artifact).exists && !artifact.isSnapshot) {
      local.verifySha1(artifact) match {
        case None =>
          logInfo("Cannot find " + artifact.sha1 + ",Verify aborted.")
          false
        case Some(false) =>
          logInfo("Error sha1 for " + artifact + ",Remove it.")
          local.remove(artifact)
          true
        case Some(true) => false
      }
    } else {
      true
    }
  }

  private def doDownload(products: Iterable[RepoArchive], executor: ExecutorService, statuses: ConcurrentHashMap[URL, Downloader]): Unit = {
    if (products.size <= 0) return
    var idx = 1
    for (artifact <- products) {
      if (!local.file(artifact).exists()) {
        val id = idx
        remotes.find(r => r.exists(artifact)) match
          case None =>
            println(s"Not found(${remotes.size} mirrors):" + artifact.url)
          case Some(remote) =>
            val downloader = RangeDownloader(s"$id/${products.size}", remote.url(artifact), local.url(artifact))
            downloader.verbose = verbose
            downloader.addProperties(properties)
            statuses.put(downloader.url, downloader)

            executor.execute(() => {
              try {
                downloader.start()
              } catch {
                case e: IOException => e.printStackTrace()
              } finally {
                statuses.remove(downloader.url)
              }
            })
        idx += 1
      }
    }

    val maxBufferWidth = 100
    val splash = Array('\\', '|', '/', '-')
    var i = 0
    while (!statuses.isEmpty && !executor.isTerminated) {
      sleep(1000)
      if (verbose) {
        print("\r")
        val sb = new StringBuilder()
        sb.append(splash(i % 4)).append("  ")
        for ((_, dl) <- asScala(statuses)) {
          sb.append(FileSize(dl.downloaded) + "/" + FileSize(dl.contentLength) + "    ")
        }
        if (sb.length() > maxBufferWidth) {
          sb.delete(maxBufferWidth, sb.length())
        }
        if (sb.length > 3) { //collect status
          sb.append(" " * (maxBufferWidth - sb.length))
          i += 1
          print(sb.toString)
        }
      }
    }
  }

  private def sleep(millsecond: Int): Unit = {
    try {
      Thread.sleep(millsecond)
    } catch {
      case e: InterruptedException =>
        e.printStackTrace()
        throw new RuntimeException(e)
    }
  }

  protected def logInfo(msg: String): Unit = {
    if verbose then println(msg)
  }

}
