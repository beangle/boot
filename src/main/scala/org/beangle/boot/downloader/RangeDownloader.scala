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
package org.beangle.boot.downloader

import java.io._
import java.net.{HttpURLConnection, URL}
import java.util.concurrent.{Callable, ExecutorService, Executors}

import org.beangle.commons.collection.Collections
import org.beangle.commons.io.IOs
import org.beangle.commons.lang.Strings
import org.beangle.commons.net.http.HttpUtils

import scala.collection.mutable

object RangeDownloader {
  def apply(name: String, url: String, location: String): RangeDownloader = {
    new RangeDownloader(name, new URL(url), new File(location))
  }
}

class RangeDownloader(name: String, url: URL, location: File) extends AbstractDownloader(name, url, location) {

  var threads: Int = 10

  var executor: ExecutorService = Executors.newFixedThreadPool(threads)

  private val properties = Collections.newMap[String, String]

  def addProperty(name: String, value: String): this.type = {
    properties.put(name, value)
    this
  }

  def addProperties(props: collection.Map[String, String]): this.type = {
    properties ++= props
    this
  }

  protected override def downloading(): Unit = {
    val urlStatus = HttpUtils.access(this.url)
    if (urlStatus.length < 0) {
      println("\r" + HttpUtils.toString(urlStatus.status) + " " + this.url)
      return
    }
    //小于1M的普通下载
    if (urlStatus.length <= 1024 * 1024 || !urlStatus.supportRange) {
      if (verbose) println("\nDownloading " + urlStatus.target + "[" + urlStatus.length + "b]")
      super.defaultDownloading(urlStatus.target.openConnection)
      return
    } else {
      if (verbose) println("\nRange-Downloading " + this.url)
    }
    this.status = new Downloader.Status(urlStatus.length)
    if (this.status.total > java.lang.Integer.MAX_VALUE) {
      throw new RuntimeException(s"Cannot download ${this.url} with size ${this.status.total}")
    }

    var lastModified: Long = urlStatus.lastModified
    val tasks = new java.util.ArrayList[Callable[Unit]]
    val parts = new mutable.ArrayBuffer[File]
    val steps = Array.ofDim[(Long, Long)](threads)
    val avgStep = this.status.total / threads
    steps.indices foreach { i =>
      steps(i) = (i * avgStep, (i + 1) * avgStep - 1) // range contains head and tail
    }
    val lastSegment = steps(steps.length - 1)
    steps(steps.length - 1) = (lastSegment._1, lastSegment._2 + this.status.total - avgStep * steps.length)
    steps foreach { seg =>
      val start = seg._1
      val end = seg._2
      val part = new File(location.getCanonicalPath + s".part_${start}_$end")
      parts += part
      tasks.add(() => {
        var input: InputStream = null
        var output: OutputStream = null
        try {
          val connection = urlStatus.target.openConnection.asInstanceOf[HttpURLConnection]
          connection.setRequestProperty("RANGE", "bytes=" + start + "-" + end)
          output = new FileOutputStream(part)
          properties foreach (e => connection.setRequestProperty(e._1, e._2))
          input = connection.getInputStream
          if (lastModified == 0) lastModified = connection.getLastModified
          val buffer = Array.ofDim[Byte](1024)
          var n = input.read(buffer)
          var next = start
          var tooMore = false
          while (-1 != n && !tooMore) {
            if (next + n - 1 > end) { //sometimes,it gives more than requires
              n = (end - next + 1).asInstanceOf[Int]
              tooMore = true
            }
            output.write(buffer, 0, n)
            status.count.addAndGet(n)
            next += n
            n = input.read(buffer)
          }
        } catch {
          case e: Throwable => e.printStackTrace()
        } finally {
          IOs.close(input, output)
        }
      })
    }
    try {
      executor.invokeAll(tasks)
      executor.shutdown()
    } catch {
      case e: Throwable => e.printStackTrace()
    }
    if (status.count.get == status.total) {
      if (corrupted(parts)) {
        parts foreach (_.delete())
      } else {
        val part = new File(location.getCanonicalPath + s".part")
        val output = new FileOutputStream(part)
        parts foreach { p =>
          IOs.copy(new FileInputStream(p), output)
          p.delete()
        }
        IOs.close(output)
        part.renameTo(location)
      }
      if (lastModified > 0) location.setLastModified(lastModified)
    } else {
      corrupted(parts)
      parts foreach (_.delete())
      throw new RuntimeException(s"Download error: expect ${status.total} but get ${status.count.get}.")
    }
    finish(url, System.currentTimeMillis() - startAt)
  }

  private def corrupted(parts: Iterable[File]): Boolean = {
    var corrupted = false
    parts foreach { p =>
      if (p.exists()) {
        val sizeRange = Strings.split(Strings.substringAfterLast(p.getName, ".part_"), "_")
        val size = (sizeRange(1).toLong - sizeRange(0).toLong + 1)
        if (size != p.length()) {
          println(s"${p.getCanonicalPath} is corrupted.")
          corrupted = true
        }
      } else {
        corrupted = true
      }
    }
    corrupted
  }
}
