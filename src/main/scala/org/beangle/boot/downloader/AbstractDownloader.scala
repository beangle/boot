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

package org.beangle.boot.downloader

import org.beangle.boot.artifact.util.FileSize
import org.beangle.commons.net.http.HttpUtils

import java.io.File
import java.net.URI

abstract class AbstractDownloader(val name: String, val url: String, protected val location: File) extends Downloader {

  protected var status: Downloader.Status = _
  protected var startAt: Long = _
  var verbose: Boolean = true

  protected def logInfo(msg: String): Unit = {
    if verbose then println(msg)
  }

  def contentLength: Long = {
    if (null == status) 0 else status.total
  }

  def downloaded: Long = {
    if (null == status) 0 else status.count.get
  }

  def start(): Unit = {
    if (location.exists()) return
    location.getParentFile.mkdirs()
    this.startAt = System.currentTimeMillis
    downloading()
  }

  protected def downloading(): Unit

  protected def finish(url: String, elaps: Long): Unit = {
    if (verbose) {
      val printurl = "\r" + name + " " + url + " "
      if (status.total < 1024) {
        if (elaps == 0) println(printurl + FileSize(status.total))
        else println(printurl + status.total + "Byte(" + elaps / 1000 + "s)")
      } else {
        if (elaps == 0) println(printurl + FileSize(status.total))
        else println(printurl + FileSize(status.total) + "(" + ((status.total / 1024.0 / elaps * 100000.0).toInt / 100.0) + "KB/s)")
      }
    }
  }

  protected def defaultDownloading(uri: URI, expectSize: Long): Unit = {
    this.status = new Downloader.Status(expectSize)
    if (HttpUtils.download(uri.toString, location)) {
      this.status.count.set(location.length())
    }
    finish(url, System.currentTimeMillis - startAt)
  }

}
