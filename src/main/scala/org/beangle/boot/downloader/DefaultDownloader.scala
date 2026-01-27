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

import org.beangle.commons.net.Networks
import org.beangle.commons.net.http.HttpUtils

import java.io.File

class DefaultDownloader(id: String, url: String, location: File) extends AbstractDownloader(id, url, location) {
  protected override def downloading(): Unit = {
    val urlStatus = HttpUtils.access(this.url)
    if (urlStatus.length < 0) {
      logInfo("Cannot download " + this.url + s",due to content lenth is ${urlStatus.length}")
      return
    }
    logInfo("Downloading " + this.url)
    super.defaultDownloading(Networks.uri(this.url),urlStatus.length)
  }
}
