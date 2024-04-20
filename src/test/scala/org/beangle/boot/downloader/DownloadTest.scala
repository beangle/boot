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
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.net.URL

class DownloadTest extends AnyFunSpec with Matchers {
  describe("download missing") {
    it("download missing") {
      val location = File.createTempFile("ant", "jar")
      val errorUrl = "https://maven.aliyun.com/nexus/content/groups/public/ant/ant/1.5.4/ant-1.5.4_1.5.3.jar.diff"

      location.delete()
      val downloader = new DefaultDownloader("1", Networks.url(errorUrl), location)
      println(location)
      downloader.start()
    }
  }
}
