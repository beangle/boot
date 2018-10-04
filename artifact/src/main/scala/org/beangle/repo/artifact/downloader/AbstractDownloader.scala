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

import java.io.{ File, FileOutputStream, IOException, InputStream, OutputStream }
import java.net.{ URL, URLConnection }
import java.net.HttpURLConnection.{ HTTP_FORBIDDEN, HTTP_NOT_FOUND, HTTP_OK, HTTP_UNAUTHORIZED, HTTP_MOVED_PERM, HTTP_MOVED_TEMP }
import java.net.HttpURLConnection

import org.beangle.commons.io.IOs

abstract class AbstractDownloader(val name: String, val url: URL, protected val location: File)
  extends Downloader {

  protected var status: Downloader.Status = null
  protected var startAt: Long = _

  def contentLength: Long = {
    if ((null == status)) 0 else status.total
  }

  def downloaded: Long = {
    if ((null == status)) 0 else status.count.get
  }

  def start(): Unit = {
    if (location.exists()) return
    location.getParentFile.mkdirs()
    this.startAt = System.currentTimeMillis
    downloading()
  }

  protected def downloading(): Unit

  protected def httpCodeString(httpCode: Int): String = {
    httpCode match {
      case HTTP_OK           => "OK"
      case HTTP_FORBIDDEN    => "Access denied!"
      case HTTP_NOT_FOUND    => "Not Found"
      case HTTP_UNAUTHORIZED => "Access denied"
      case code: Any         => String.valueOf(code)
    }
  }

  protected def access(): ResourceStatus = {
    try {
      val hc = url.openConnection().asInstanceOf[HttpURLConnection]
      hc.setRequestMethod("HEAD")
      val rc = hc.getResponseCode
      val supportRange = ("bytes" == hc.getHeaderField("Accept-Ranges"))
      rc match {
        case HTTP_OK | HTTP_MOVED_TEMP | HTTP_MOVED_PERM =>
          ResourceStatus(rc, hc.getURL, hc.getHeaderFieldLong("Content-Length", 0), hc.getLastModified, supportRange)
        case _ => ResourceStatus(rc, hc.getURL, -1, -1, false)
      }
    } catch {
      case e: IOException => ResourceStatus(-1, null, -1, 0, false)
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
      file.renameTo(location)
      if (this.status.total < 0) {
        this.status.total = this.status.count.get
      }
    } finally {
      IOs.close(input, output)
    }
    finish(conn.getURL, System.currentTimeMillis - startAt)
  }

  case class ResourceStatus(status: Int, target: URL, length: Long, lastModified: Long, supportRange: Boolean)
}
