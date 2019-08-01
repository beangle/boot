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

import java.io.{File, FileInputStream}
import java.net.{HttpURLConnection, URL}

import org.beangle.commons.collection.Collections
import org.beangle.commons.io.IOs
import org.beangle.commons.lang.Charsets
import org.beangle.commons.net.http.HttpUtils
import org.beangle.repo.artifact.util.Delta

object Repo {

  def local(base: String): Local = {
    new Local(base, Layout.Maven2)
  }

  def remote(base: String): Remote = {
    new Remote(base, base, Layout.Maven2)
  }

  abstract class Repository {
    def id: String

    def base: String

    def layout: Layout

    def exists(filePath: String): Boolean

    def exists(a: Artifact): Boolean = {
      exists(layout.path(a))
    }

    def url(p: Product): String = {
      p match {
        case a: Artifact => base + layout.path(a)
        case d: Diff => base + layout.path(d)
      }
    }
  }

  class Local(ibase: String = null, val layout: Layout = Layout.Maven2) extends Repository {
    def id = "local"

    def pattern = "*"

    val base: String = findLocalBase(layout, ibase)
    new File(this.base).mkdirs()

    override def exists(path: String): Boolean = {
      new File(base + path).exists
    }

    def file(filePath: String): File = {
      new File(base + filePath)
    }

    def file(product: Product): File = {
      new File(url(product))
    }

    def remove(product: Product): Boolean = {
      new File(url(product)).delete()
    }

    def verifySha1(artifact: Artifact): Boolean = {
      val sha1 = artifact.sha1
      if (exists(artifact) && exists(sha1)) {
        val sha1sum = Delta.sha1(url(artifact))
        val sha1inFile = IOs.readString(new FileInputStream(url(sha1)), Charsets.UTF_8).trim()
        sha1inFile.contains(sha1sum)
      } else {
        true
      }
    }

    def lastestBefore(artifact: Artifact): Option[Artifact] = {
      lastest(artifact, isLessThen = true)
    }

    def lastest(artifact: Artifact): Option[Artifact] = {
      lastest(artifact, isLessThen = false)
    }

    def lastest(artifact: Artifact, isLessThen: Boolean): Option[Artifact] = {
      val parent = new File(url(artifact)).getParentFile.getParentFile
      if (parent.exists()) {
        val siblings = parent.list().toList
        val versions = Collections.newBuffer[String]
        for (sibling <- siblings) {
          if (!sibling.contains("SNAPSHOT")
            && new File(parent.getAbsolutePath + File.separator + sibling).isDirectory) {
            if (isLessThen) {
              if (sibling.compareTo(artifact.version) < 0) versions += sibling
            } else {
              versions += sibling
            }
          }
        }
        val rs = versions.sorted
        if (rs.isEmpty) None
        else Some(artifact.forVersion(rs(rs.size - 1)))
      } else {
        None
      }
    }
  }

  object Remote {
    val CentralURL = "http://central.maven.org/maven2"
    val AliyunURL = "https://maven.aliyun.com/nexus/content/groups/public"
  }

  class Remote(val id: String, var base: String, val layout: Layout = Layout.Maven2) extends Repository {
    this.base = normalizeUrl(base)

    def this() {
      this("central", Remote.CentralURL, Layout.Maven2)
    }

    override def exists(filePath: String): Boolean = {
      try {
        val hc = HttpUtils.followRedirect(new URL(base + filePath).openConnection(), "HEAD")
        hc.getResponseCode == HttpURLConnection.HTTP_OK
      } catch {
        case _: Throwable => false
      }
    }

    override def hashCode: Int = {
      id.hashCode
    }

    override def equals(any: Any): Boolean = {
      any match {
        case r: Remote => r.id.equals(this.id)
        case _ => false
      }
    }

    override def toString: String = {
      id + ":" + base
    }
  }

  class Mirror(id: String, base: String, val pattern: String = "*",
               layout: Layout = Layout.Maven2) extends Remote(id, base, layout) {
    def matches(filePath: String): Boolean = {
      pattern == "*" || filePath.startsWith(pattern)
    }

    override def exists(filePath: String): Boolean = {
      if (matches(filePath)) super.exists(filePath) else false
    }

  }

  private def normalizeUrl(baseUrl: String): String = {
    val httpBase = if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) "http://" + baseUrl else baseUrl
    if (httpBase.endsWith("/")) httpBase.substring(0, httpBase.length - 1) else httpBase
  }

  private def findLocalBase(layout: Layout, base: String): String = {
    if (null == base) {
      if (layout == Layout.Maven2) {
        System.getProperty("user.home") + "/.m2/repository"
      } else if (layout == Layout.Ivy2) {
        System.getProperty("user.home") + "/.ivy2/cache"
      } else {
        throw new RuntimeException("Do not support layout $layout,Using maven2 or ivy2")
      }
    } else {
      if (base.endsWith("/")) base.substring(0, base.length - 1) else base
    }
  }

}
