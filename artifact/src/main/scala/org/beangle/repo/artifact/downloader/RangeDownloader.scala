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

import java.io.{ File, FileOutputStream }
import java.net.{ HttpURLConnection, URL }
import java.util.concurrent.{ Callable, ExecutorService, Executors }
import org.beangle.commons.io.IOs

class RangeDownloader(name: String, url: String, location: String) extends AbstractDownloader(name, url, location) {

  var threads: Int = 20

  var step: Int = 100 * 1024

  var executor: ExecutorService = Executors.newFixedThreadPool(threads)

  protected override def downloading(resource: URL) {
    println("Downloading " + resource)
    val urlStatus = access(resource)
    if (null == urlStatus.conn) {
      println("\r" + httpCodeString(urlStatus.status) + " " + resource)
      return
    }
    if (urlStatus.length < 0) {
      super.defaultDownloading(urlStatus.conn)
      return
    }
    val newUrl = urlStatus.conn.getURL
    this.status = new Downloader.Status(urlStatus.length)
    if (this.status.total > java.lang.Integer.MAX_VALUE) {
      throw new RuntimeException(s"Cannot download ${url} with size ${this.status.total}")
    }

    val conn = urlStatus.conn
    val total = this.status.total.toInt
    val totalbuffer = Array.ofDim[Byte](total)
    var begin = 0
    val tasks = new java.util.ArrayList[Callable[Integer]]
    while (begin < this.status.total) {
      val start = begin
      val end = if (((start + step - 1) >= total)) (total - 1) else (start + step - 1)
      tasks.add(new Callable[Integer]() {
        def call(): Integer = {
          val connection = newUrl.openConnection().asInstanceOf[HttpURLConnection]
          connection.setRequestProperty("RANGE", "bytes=" + start + "-" + end)
          val input = connection.getInputStream
          val buffer = Array.ofDim[Byte](1024)
          var n = input.read(buffer)
          var next = start
          while (-1 != n) {
            System.arraycopy(buffer, 0, totalbuffer, next, n)
            status.count.addAndGet(n)
            next += n
            n = input.read(buffer)
          }
          IOs.close(input)
          end
        }
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
      val targetFile = new File(location)
      val output = new FileOutputStream(targetFile)
      output.write(totalbuffer, 0, total)
      targetFile.setLastModified(conn.getLastModified)
      IOs.close(output)
    } else {
      throw new RuntimeException("Download error")
    }
    finish(conn.getURL, System.currentTimeMillis() - startAt)
  }

}
