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

import java.io.File

import org.beangle.repo.artifact.downloader.RangeDownloader

trait Repos {

  def find(path: String): Option[Repo.Remote]

  def local: Repo.Local

  def download(path: String, remote: Repo.Remote): File = {
    val localFile = local.file(path)
    if (!localFile.exists()) {
      RangeDownloader("download", remote.base + path, localFile.getAbsolutePath).start()
    }
    localFile
  }
  var cacheable: Boolean = true
}

class ProxyRepos(val local: Repo.Local, val remote: Repo.Remote) extends Repos {
  override def find(path: String): Option[Repo.Remote] = {
    if (remote.exists(path)) Some(remote) else None
  }
}

class MirrorRepos(val local: Repo.Local, val mirrors: List[Repo.Mirror], val backend: Repo.Mirror) extends Repos {
  override def find(path: String): Option[Repo.Remote] = {
    val matches = mirrors.filter(x => x.matches(path))
    val exists = matches.find(x => x.exists(path))
    exists match {
      case Some(e) => exists
      case None    => if (backend.exists(path)) Some(backend) else None
    }
  }
}
