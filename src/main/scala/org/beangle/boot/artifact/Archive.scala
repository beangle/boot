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

package org.beangle.boot.artifact

import org.beangle.commons.lang.Strings.*
import org.beangle.commons.lang.{Strings, SystemInfo}

import java.io.File

sealed trait Archive {
  def url: String

  override def toString: String = url
}

object Archive {
  def apply(url: String): Archive = {
    if (url.startsWith("http")) {
      RemoteFile(url)
    } else if (url.startsWith("gav://") || !url.contains("/")) {
      if (url.startsWith("gav://")) {
        Artifact(substringAfter(url, "gav://"))
      } else {
        Artifact(url)
      }
    } else {
      var file = url
      if (file.startsWith("file://")) {
        file = substringAfter(url, "file://")
      }
      if (file.startsWith("~")) {
        file = replace(file, "~", SystemInfo.user.home)
      }
      var variable = substringBetween(file, "${", "}")
      while (isNotEmpty(variable)) {
        file = replace(file, "${" + variable + "}", SystemInfo.properties.getOrElse(variable, variable))
        variable = substringBetween(file, "${", "}")
      }
      LocalFile(file)
    }
  }
}

trait RepoArchive extends Archive

object Artifact {
  private val packagings =
    Set("jar", "war", "pom", "zip", "ear", "rar", "ejb", "ejb3", "tar", "tar.gz")

  /**
   * Resolve gav string
   * net.sf.json-lib:json-lib:jar:jdk15:2.4
   * net.sf.json-lib:json-lib:jar:jdk15:2.4
   */
  def apply(gav: String): Artifact = {
    val infos = gav.split(":")
    if (infos.length == 4) {
      val cOp = infos(2)
      var classifier: Option[String] = None
      var packaging = ""
      if (packagings.contains(cOp)) {
        classifier = None
        packaging = cOp
      } else {
        classifier = Some(cOp)
        packaging = "jar"
      }
      val version = infos(infos.length - 1)

      new Artifact(infos(0), infos(1), version, classifier, packaging)
    } else if (infos.length == 5) {
      new Artifact(infos(0), infos(1), infos(4), Some(infos(3)), infos(2))
    } else if (infos.length == 3) {
      new Artifact(infos(0), infos(1), infos(2), None, "jar")
    } else {
      throw new RuntimeException("Cannot recoganize artifact format " + gav)
    }
  }
}

case class Artifact(groupId: String, artifactId: String,
                    version: String, classifier: Option[String] = None, packaging: String = "jar")
  extends RepoArchive {

  def packaging(newPackaing: String): Artifact = {
    Artifact(groupId, artifactId, version, classifier, newPackaing)
  }

  def md5: Artifact = {
    Artifact(groupId, artifactId, version, classifier, packaging + ".md5")
  }

  def sha1: Artifact = {
    Artifact(groupId, artifactId, version, classifier, packaging + ".sha1")
  }

  def forVersion(newVersion: String): Artifact = {
    new Artifact(groupId, artifactId, newVersion, classifier, packaging)
  }

  override def hashCode: Int = {
    toString.hashCode
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case o: Artifact =>
        this.groupId == o.groupId && this.artifactId == o.artifactId &&
          this.version == o.version && this.classifier == o.classifier &&
          this.packaging == o.packaging
      case _ => false
    }
  }

  def isSnapshot: Boolean = {
    version.contains("SNAPSHOT")
  }

  def url: String = {
    classifier match {
      case Some(c) => s"gav://$groupId:$artifactId:$packaging:$c:$version"
      case None => s"gav://$groupId:$artifactId:$packaging:$version"
    }
  }
}

object Diff {
  def apply(old: Artifact, newVersion: String): Diff = {
    Diff(old.groupId, old.artifactId, old.version, newVersion, old.classifier, old.packaging + ".diff")
  }
}

case class Diff(groupId: String, artifactId: String,
                oldVersion: String, newVersion: String, classifier: Option[String], packaging: String = "jar")
  extends RepoArchive {

  def older: Artifact = {
    new Artifact(groupId, artifactId, oldVersion, classifier, packaging.replace(".diff", ""))
  }

  def newer: Artifact = {
    new Artifact(groupId, artifactId, newVersion, classifier, packaging.replace(".diff", ""))
  }

  override def url: String = {
    classifier match {
      case Some(c) => s"$groupId:$artifactId:$packaging:$c:${oldVersion}_$newVersion"
      case None => s"$groupId:$artifactId:$packaging:${oldVersion}_$newVersion"
    }
  }

}

case class LocalFile(file: String) extends Archive {
  override def url: String = {
    s"file://$file"
  }
}

case class RemoteFile(url: String) extends Archive {

  def local(localRepo: Repo.Local): File = {
    var path = url
    path = replace(path, "https://", "")
    path = replace(path, "http://", "")
    if (path.contains("?")) {
      path = substringBefore(path, "?")
    }
    new File(localRepo.base + "/" + path)
  }
}
