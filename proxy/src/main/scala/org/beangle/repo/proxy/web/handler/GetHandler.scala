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
package org.beangle.repo.proxy.web.handler

import java.io.{ File, FileInputStream }
import java.text.SimpleDateFormat
import java.util.Arrays

import org.beangle.commons.activation.MimeTypes
import org.beangle.commons.lang.Strings
import org.beangle.commons.web.io.RangedWagon
import org.beangle.commons.web.util.RequestUtils
import org.beangle.webmvc.api.util.CacheControl
import org.beangle.webmvc.execution.Handler

import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import org.beangle.repo.proxy.service.RepoService
import org.beangle.repo.artifact.Repo

/**
 * @author chaostone
 */
class GetHandler extends Handler {
  val wagon = new RangedWagon
  def handle(request: HttpServletRequest, response: HttpServletResponse): Any = {
    val filePath = RequestUtils.getServletPath(request)
    val repos = RepoService.repos
    val local = repos.local
    if (filePath.endsWith("/")) {
      val localFile = local.file(filePath)
      if (localFile.exists) listDir(filePath, localFile, request, response) else response.setStatus(HttpServletResponse.SC_NOT_FOUND)
    } else {
      val localFile = local.file(filePath)
      if (localFile.exists) {
        if (localFile.isDirectory) listDir(filePath, localFile, request, response)
        else transfer(localFile, request, response)
      } else {
        if (filePath.endsWith(".diff")) {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND)
        } else {
          repos.find(filePath) match {
            case Some(repo) =>
              if (repos.cacheable) {
                transfer(repos.download(filePath, repo), request, response)
              } else {
                response.sendRedirect(repo.base + filePath)
              }
            case None =>
              response.setStatus(HttpServletResponse.SC_NOT_FOUND)
          }
        }
      }
    }
  }

  private def transfer(file: File, request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val fileName = file.getName
    val ext = Strings.substringAfterLast(fileName, ".")
    if (Strings.isNotEmpty(ext)) MimeTypes.getMimeType(ext) foreach (m => response.setContentType(m.toString))
    if (!fileName.contains("SNAPSHOT")) CacheControl.expiresAfter(10, response)
    wagon.copy(new FileInputStream(file), request, response)
  }

  private def listDir(uri: String, dir: File, request: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setCharacterEncoding("utf-8")
    val formater = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    val writer = response.getWriter
    writer.write(s"<!DOCTYPE html><head><title>Index of $uri</title></head><body>")
    writer.write(s"<h1>Index of $uri</h1><hr/><pre>")
    //uri != "/" && uri != ""
    if (uri.length > 1) {
      if (uri.endsWith("/")) writer.write("<a href=\"../\">../</a>\n")
      else writer.write("<a href=\"./\">../</a>\n")
    }
    val buffer = new StringBuilder(200)
    val items = dir.list()
    var prefix = ""
    if (!uri.endsWith("/")) {
      prefix = Strings.substringAfterLast(uri, "/")
      if (prefix.isEmpty()) prefix = request.getContextPath + "/" else prefix += "/"
    }
    Arrays.sort(items.asInstanceOf[Array[Object]])
    items foreach { fileName =>
      val href = prefix + fileName
      buffer.clear()
      val item = new File(dir.getAbsolutePath + File.separator + fileName)
      if (!item.isHidden && fileName.charAt(0) != '_') {
        if (item.isDirectory) {
          buffer ++= ("<a href=\"" + href + "/\">" + s"$fileName/</a>")
          if (fileName.length < 59) buffer ++= " " * (59 - fileName.length)
        } else {
          writer.write("<a href=\"" + href + "\">" + s"$fileName</a>")
          if (fileName.length < 60) buffer ++= " " * (60 - fileName.length)
        }
        val lastModified = new java.util.Date(item.lastModified)
        buffer ++= (formater.format(lastModified))
        if (item.isDirectory) {
          buffer ++= "                   -"
        } else {
          val fileSize = item.length.toString
          buffer ++= (" " * (20 - fileSize.length))
          buffer ++= fileSize
        }
        buffer ++= ("\n")
        writer.write(buffer.toString)
      }
    }
    writer.write("</pre><hr></body></html>")
  }

}
