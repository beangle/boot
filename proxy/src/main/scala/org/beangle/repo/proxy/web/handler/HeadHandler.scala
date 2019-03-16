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

import java.io.File

import org.beangle.commons.web.util.RequestUtils
import org.beangle.repo.proxy.service.RepoService
import org.beangle.webmvc.execution.Handler

import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import javax.servlet.http.HttpServletResponse.SC_OK

/**
 * @author chaostone
 */
class HeadHandler extends Handler {

  def handle(request: HttpServletRequest, response: HttpServletResponse): Any = {
    val filePath = RequestUtils.getServletPath(request)
    val repos = RepoService.repos
    val local = repos.local
    val localFile = local.file(filePath)
    if (localFile.exists) {
      outhead(localFile, response)
    } else {
      if (filePath.endsWith(".diff")) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND)
      } else {
        repos.find(filePath) match {
          case Some(repo) =>
            if (repos.cacheable) {
              outhead(repos.download(filePath, repo), response)
            } else {
              response.sendRedirect(repo.base + filePath)
            }
          case None => response.setStatus(HttpServletResponse.SC_NOT_FOUND)
        }
      }
    }
  }

  private def outhead(file: File, response: HttpServletResponse): Unit = {
    if (null != file && file.exists) {
      response.setContentLengthLong(file.length)
      response.setDateHeader("Last-Modified", file.lastModified)
      response.setHeader("Accept-Ranges", "bytes")
      response.setStatus(SC_OK)
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }
  }
}
