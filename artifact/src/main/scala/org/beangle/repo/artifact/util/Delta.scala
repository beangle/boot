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
package org.beangle.repo.artifact.util

import java.io.{ BufferedReader, File, InputStreamReader }
import org.beangle.commons.file.diff.Bsdiff
import org.beangle.commons.lang.Strings

object Delta {

  def diff(oldFile: String, newFile: String, diffFile: String): Unit = {
    Bsdiff.diff(new File(oldFile), new File(newFile), new File(diffFile))
  }

  def patch(oldFile: String, newFile: String, patchFile: String): Unit = {
    Bsdiff.patch(new File(oldFile), new File(newFile), new File(patchFile))
  }

  def sha1(fileLoc: String): String = {
    //windows下执行的有反斜线
    var rs = exec("sha1sum", fileLoc)
    rs = Strings.replace(rs, "\\", "")
    //sha1出来的内容往往是 带有文件名,这是过滤摘要后面的文件名
    val spaceIdx = rs.indexOf(' ')
    if (-1 == spaceIdx) rs.trim
    else rs.substring(0, spaceIdx).trim
  }

  private def exec(command: String, args: String*): String = {
    try {
      val arguments = new collection.mutable.ArrayBuffer[String]
      arguments += command
      arguments ++= args

      val pb = new ProcessBuilder(arguments: _*)
      pb.redirectErrorStream(true)
      val pro = pb.start()
      pro.waitFor()
      val reader = new BufferedReader(new InputStreamReader(pro.getInputStream()))
      val sb = new StringBuilder()
      var line = reader.readLine()
      while (line != null) {
        sb.append(line).append('\n')
        line = reader.readLine()
      }
      reader.close()
      sb.toString
    } catch {
      case e: Throwable => throw new RuntimeException(e)
    }
  }
}
