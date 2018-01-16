/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2018, Beangle Software.
 *
 * Beangle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Beangle is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Beangle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.repo.artifact.downloader

import java.io.{ Closeable, File, IOException }
import java.net.{ HttpURLConnection, URL }
import java.net.HttpURLConnection._
import java.io.FileOutputStream
import org.beangle.commons.io.IOs
import java.io.OutputStream
import java.io.InputStream
import java.net.URLConnection

abstract class AbstractDownloader(val name: String, val url: String, protected val location: String) extends Downloader {
  protected var status: Downloader.Status = null
  protected var startAt: Long = _

  def contentLength: Long = {
    if ((null == status)) 0 else status.total
  }

  def downloaded: Long = {
    if ((null == status)) 0 else status.count.get
  }

  def start(): Unit = {
    val file = new File(location)
    if (file.exists()) return
    file.getParentFile.mkdirs()
    this.startAt = System.currentTimeMillis
    downloading(new URL(url))
  }

  protected def downloading(resource: URL): Unit

  protected def httpCodeString(httpCode: Int): String = {
    httpCode match {
      case HTTP_OK           => "OK"
      case HTTP_FORBIDDEN    => "Access denied!"
      case HTTP_NOT_FOUND    => "Not Found"
      case HTTP_UNAUTHORIZED => "Access denied"
      case code: Any         => String.valueOf(code)
    }
  }

  protected def access(url: URL, method: String = "GET"): ResourceStatus = {
    try {
      val hc = url.openConnection().asInstanceOf[HttpURLConnection]
      hc.setRequestMethod(method)
      val rc = hc.getResponseCode
      rc match {
        case HTTP_OK => ResourceStatus(rc, hc.getContentLengthLong, hc)
        case _       => ResourceStatus(rc, -1, null)
      }
    } catch {
      case e: IOException => ResourceStatus(-1, -1, null)
    }
  }

  private def buildRedirect(loc: String, ref: URL): URL = {
    if (null == loc) return null
    if (loc.startsWith("http:") || loc.startsWith("https:")) {
      new URL(loc)
    } else {
      new URL(ref.getProtocol + "://" + ref.getHost + ":" + ref.getPort + loc)
    }
  }

  protected def finish(url: URL, elaps: Long) {
    val printurl = "\r" + name + " " + url + " "
    if (status.total < 1024) {
      if (elaps == 0) println(printurl + status.total + "Byte")
      else println(printurl + status.total + "Byte(" + elaps / 1000 + "s)")
    } else {
      if (elaps == 0) println(printurl + (status.total / 1024) + "KB")
      else println(printurl + (status.total / 1024) + "KB(" + ((status.total / 1024.0 / elaps * 100000.0).toInt / 100.0) + "KB/s)")
    }
  }

  protected def defaultDownloading(conn: URLConnection) {
    var input: InputStream = null
    var output: OutputStream = null
    try {
      val file = new File(location + ".part")
      file.delete()
      val buffer = Array.ofDim[Byte](1024 * 4)
      this.status = new Downloader.Status(conn.getContentLengthLong)
      input = conn.getInputStream
      output = new FileOutputStream(file)
      var n = input.read(buffer)
      while (-1 != n) {
        output.write(buffer, 0, n)
        status.count.addAndGet(n)
        n = input.read(buffer)
      }
      file.renameTo(new File(location))
      if (this.status.total < 0) {
        this.status.total = this.status.count.get
      }
    } finally {
      IOs.close(input, output)
    }
    finish(conn.getURL, System.currentTimeMillis - startAt)
  }

  case class ResourceStatus(status: Int, length: Long, conn: HttpURLConnection = null)
}
