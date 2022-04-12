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

import java.io.File
import java.util.zip.ZipFile

object War {
  def isLibEmpty(path: String): Boolean = {
    val file = new File(path)
    if (file.exists()) {
      val war = new ZipFile(file)
      val entries = war.entries()
      var finded = false
      while (entries.hasMoreElements && !finded) {
        val entry = entries.nextElement().getName
        finded = (entry.startsWith("WEB-INF/lib/") && entry.endsWith(".jar"))
      }
      !finded
    } else {
      throw new RuntimeException(s"Cannot find war file located at $path")
    }
  }
}
