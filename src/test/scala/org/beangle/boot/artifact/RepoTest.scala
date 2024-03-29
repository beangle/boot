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

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class RepoTest extends AnyFunSpec with Matchers {

  describe("Local repo") {
    it("to string") {
      val local = Repo.local(null)
      val artifact = Artifact("org.beangle.commons:beangle-commons-web_2.12:5.0.0.M6")
      println(local.latestBefore(artifact))
      println(local.latest(artifact))
    }
  }
}
