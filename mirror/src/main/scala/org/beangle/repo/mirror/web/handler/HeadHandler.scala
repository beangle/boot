/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2018, Beangle Software.
 *
 * Beangle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Beangle is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Beangle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.maven.repo.web.handler

import org.beangle.commons.lang.annotation.spi
import org.beangle.webmvc.api.action.ActionSupport
import org.beangle.webmvc.execution.Handler
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import javax.servlet.http.HttpServletResponse.{ SC_NOT_FOUND, SC_OK }
import org.beangle.commons.web.util.RequestUtils
import org.beangle.maven.repo.service.RepoService
import org.beangle.repo.artifact.Repo

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
      response.setStatus(SC_OK)
    } else {
      if (filePath.endsWith(".diff")) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND)
      } else {
        repos.find(filePath) match {
          case Some(repo) =>
            if (repos.cacheable) {
              response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY)
              response.setHeader("Location", repo.base + filePath)
            } else {
              response.setStatus(HttpServletResponse.SC_OK)
            }
          case None => response.setStatus(HttpServletResponse.SC_NOT_FOUND)
        }
      }
    }
  }

}
