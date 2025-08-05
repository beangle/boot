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

import org.beangle.boot.artifact.Repo.LocalSnapshot
import org.beangle.commons.io.{Dirs, Files}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.io.File

class RepoTest extends AnyFunSpec with Matchers {

  describe("Local repo") {
    it("to string") {
      val local = Repo.local(null)
      val artifact = Artifact("org.beangle.commons:beangle-commons-web_2.12:5.0.0.M6")
      println(local.latestBefore(artifact))
      println(local.latest(artifact))
    }
    it("make a local snapshot repo") {
      val local = LocalSnapshot(null)
      println(local.base)
    }
    it("find local snapshot artifact") {
      val artifact = Artifact("org.beangle.commons:beangle-commons:5.0.0-SNAPSHOT")
      val local = LocalSnapshot(null)
      val base = local.base
      val dirs = Dirs.on(new File(base + "/org/beangle/commons/beangle-commons/5.0.0-SNAPSHOT/"))
      dirs.mkdirs()
      dirs.delete(dirs.ls(): _*)
      dirs.touch("beangle-commons-5.0.0-20250803.132600-31.jar")
      dirs.touch("beangle-commons-5.0.0-20250803.132600-9.jar")
      val file = local.latest(artifact)
      val path = file.getAbsolutePath.replace("\\", "/")
      println(path)
      path should equal(base.replace("\\", "/") + "/org/beangle/commons/beangle-commons/5.0.0-SNAPSHOT/beangle-commons-5.0.0-20250803.132600-31.jar")
    }
  }
}
