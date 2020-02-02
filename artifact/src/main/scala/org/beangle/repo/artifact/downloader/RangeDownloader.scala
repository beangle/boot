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
package org.beangle.repo.artifact.downloader

import java.io.{File, FileOutputStream}
import java.net.{HttpURLConnection, URL}
import java.util.concurrent.{Callable, ExecutorService, Executors}

import org.beangle.commons.collection.Collections
import org.beangle.commons.io.IOs
import org.beangle.commons.net.http.HttpUtils

object RangeDownloader {
  def apply(name: String, url: String, location: String): RangeDownloader = {
    new RangeDownloader(name, new URL(url), new File(location))
  }
}

class RangeDownloader(name: String, url: URL, location: File) extends AbstractDownloader(name, url, location) {

  var threads: Int = 20

  //one step is 100k
  var step: Int = 100 * 1024

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
    val urlStatus = access()
    if (urlStatus.length < 0) {
      logger.info("\r" + HttpUtils.toString(urlStatus.status) + " " + url)
      return
    }
    //小于100k的普通下载
    if (urlStatus.length < 102400 || !urlStatus.supportRange) {
      if (verbose) logger.info("Downloading " + urlStatus.target + "[" + urlStatus.length + "b]")
      super.defaultDownloading(urlStatus.target.openConnection)
      return
    } else {
      if (verbose) logger.info("Range-Downloading " + url)
    }
    this.status = new Downloader.Status(urlStatus.length)
    if (this.status.total > java.lang.Integer.MAX_VALUE) {
      throw new RuntimeException(s"Cannot download $url with size ${this.status.total}")
    }

    val total = this.status.total.toInt
    val totalbuffer = Array.ofDim[Byte](total)
    var lastModified: Long = urlStatus.lastModified
    var begin = 0
    val tasks = new java.util.ArrayList[Callable[Integer]]
    while (begin < this.status.total) {
      val start = begin
      val end = if (start + step - 1 >= total) total - 1 else start + step - 1
      tasks.add(() => {
        val connection = urlStatus.target.openConnection.asInstanceOf[HttpURLConnection]
        connection.setRequestProperty("RANGE", "bytes=" + start + "-" + end)
        properties foreach (e => connection.setRequestProperty(e._1, e._2))
        val input = connection.getInputStream
        if (lastModified == 0) lastModified = connection.getLastModified
        val buffer = Array.ofDim[Byte](1024)
        var n = input.read(buffer)
        var next = start
        var tooMore = false
        while (-1 != n && !tooMore) {
          if (next + n - 1 > end) {
            n = end - next + 1
            tooMore = true
          }
          System.arraycopy(buffer, 0, totalbuffer, next, n)
          status.count.addAndGet(n)
          next += n
          n = input.read(buffer)
        }
        IOs.close(input)
        end
      })
      begin += step
    }
    try {
      executor.invokeAll(tasks)
      executor.shutdown()
    } catch {
      case e: Throwable => e.printStackTrace()
    }
    if (status.count.get == status.total) {
      val output = new FileOutputStream(location)
      output.write(totalbuffer, 0, total)
      if (lastModified > 0) location.setLastModified(lastModified)
      IOs.close(output)
    } else {
      throw new RuntimeException(s"Download error: expect ${status.total} but get ${status.count.get}.")
    }
    finish(url, System.currentTimeMillis() - startAt)
  }

}
