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

import java.io.IOException
import java.net.URL
import java.util.concurrent.{ ConcurrentHashMap, Executors }

import scala.collection.JavaConverters.mapAsScalaMap

import org.beangle.commons.codec.binary.Base64
import org.beangle.commons.collection.Collections
import org.beangle.repo.artifact.downloader.{ Downloader, RangeDownloader }
import org.beangle.repo.artifact.util.{ Delta, FileSize }

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
  def apply(remote: String, base: String = null): ArtifactDownloader = {
    new ArtifactDownloader(Repo.remote(remote), Repo.local(base))
  }
}

class ArtifactDownloader(private val remote: Repo.Remote, private val local: Repo.Local) {

  private val statuses = new ConcurrentHashMap[URL, Downloader]()

  private val executor = Executors.newFixedThreadPool(5)

  private val properties = Collections.newMap[String, String]

  var verbose: Boolean = true

  def authorization(username: String, password: String): Unit = {
    properties.put("Authorization", "Basic " + Base64.encode(s"$username:$password".getBytes))
  }

  def download(artifacts: Iterable[Artifact]): Unit = {
    val sha1s = new collection.mutable.ArrayBuffer[Artifact]
    val diffs = new collection.mutable.ArrayBuffer[Diff]

    for (artifact <- artifacts if !local.file(artifact).exists) {
      if (!artifact.packaging.endsWith("sha1")) {
        val sha1 = artifact.sha1
        if (!local.file(sha1).exists()) {
          sha1s += sha1
        }
      }
      local.lastestBefore(artifact) foreach { lastest =>
        diffs += Diff(lastest, artifact.version)
      }
    }
    doDownload(sha1s)

    // download diffs and patch them.
    doDownload(diffs)
    val newers = new collection.mutable.ArrayBuffer[Artifact]
    for (diff <- diffs) {
      val diffFile = local.file(diff)
      if (diffFile.exists) {
        if (verbose) println("Patching " + diff)
        Delta.patch(local.url(diff.older), local.url(diff.newer), local.url(diff))
        newers += diff.newer
      }
    }
    // check it,last time.
    for (artifact <- artifacts) {
      if (!local.file(artifact).exists) {
        newers += artifact
      }
    }
    doDownload(newers)
    // verify sha1 against newer artifacts.
    for (artifact <- newers) {
      if (verbose) println("Verifing " + artifact.sha1)
      if (!local.verifySha1(artifact)) {
        if (verbose) println("Error sha1 for " + artifact + ",Remove it.")
        local.remove(artifact)
      }
    }
    executor.shutdown()
  }

  private def doDownload(products: Iterable[Product]): Unit = {
    if (products.size <= 0) return
    var idx = 1
    for (artifact <- products) {
      if (!local.file(artifact).exists()) {
        val id = idx
        executor.execute(new Runnable() {
          def run() {
            val downloader = RangeDownloader(id + "/" + products.size, remote.url(artifact), local.url(artifact))
            downloader.verbose = verbose
            downloader.addProperties(properties)
            statuses.put(downloader.url, downloader)
            try {
              downloader.start()
            } catch {
              case e: IOException => e.printStackTrace()
            } finally {
              statuses.remove(downloader.url)
            }
          }
        })
        idx += 1
      }
    }

    sleep(500)
    var i = 0
    val splash = Array('\\', '|', '/', '-')
    val count = statuses.size
    while (!statuses.isEmpty && !executor.isTerminated) {
      sleep(500)
      print("\r")
      val sb = new StringBuilder()
      sb.append(splash(i % 4)).append("  ")
      for ((key, value) <- mapAsScalaMap(statuses)) {
        val downloader = value
        sb.append((FileSize(downloader.downloaded) + "/" + FileSize(downloader.contentLength) + "    "))
      }
      sb.append(" " * (100 - sb.length))
      i += 1
      print(sb.toString)
    }
    if (count > 0) print("\n")
  }

  private def sleep(millsecond: Int) {
    try {
      Thread.sleep(500)
    } catch {
      case e: InterruptedException => {
        e.printStackTrace()
        throw new RuntimeException(e)
      }
    }
  }
}
