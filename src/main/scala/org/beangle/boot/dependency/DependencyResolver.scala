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

package org.beangle.boot.dependency

import org.beangle.boot.artifact.Archive
import org.beangle.commons.collection.Collections

import java.io.{InputStreamReader, LineNumberReader}
import java.net.URL

object DependencyResolver {
  def resolve(resource: URL): Iterable[Archive] = {
    val archives = Collections.newBuffer[Archive]
    if (null == resource) return List.empty
    try {
      val reader = new InputStreamReader(resource.openStream())
      val lr = new LineNumberReader(reader)
      var line: String = null
      while {
        line = lr.readLine()
        if (line != null && line.nonEmpty) {
          archives += Archive(line)
        }
        line != null
      } do ()
      lr.close()
    } catch {
      case e: Exception => e.printStackTrace()
    }
    archives
  }
}
