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

import org.beangle.commons.lang.SystemInfo
import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ArtifactTest extends AnyFunSpec with Matchers {
  val older = Artifact("org.beangle.commons", "beangle-commons-core_2.12", "4.6.1", Some("sources"))
  val artifact = Artifact("org.beangle.commons", "beangle-commons-core_2.12", "4.6.2", Some("sources"))
  val jsonlib1 = Artifact("net.sf.json-lib:json-lib:jar:jdk15:2.4")
  val jsonlib2 = Artifact("net.sf.json-lib:json-lib:jar:2.4")
  val jsonlib3 = Artifact("net.sf.json-lib:json-lib:jdk15:2.4")

  val tempLocalRepo = new File(SystemInfo.tmpDir + "/.m2/repository")
  tempLocalRepo.mkdirs()
  val local = new Repo.Local(tempLocalRepo.getAbsolutePath)

  describe("artifact ") {
    it("to string") {
      artifact.toString should be equals ("org.beangle.commons:beangle-commons-core_2.12:4.6.2:sources:jar")
      jsonlib1.toString should be equals ("net.sf.json-lib:json-lib:2.4:jdk15:jar")
      jsonlib2.toString should be equals ("net.sf.json-lib:json-lib:2.4:jdk15:jar")
      jsonlib3.toString should be equals ("net.sf.json-lib:json-lib:2.4:jdk15:jar")
      jsonlib1.version should be equals ("2.4")
      jsonlib2.version should be equals ("2.4")
      jsonlib3.version should be equals ("2.4")

      jsonlib1.packaging should be equals ("jar")
      jsonlib2.packaging should be equals ("jar")
      jsonlib3.packaging should be equals ("jar")

      jsonlib1.classifier should be equals (Some("jdk15"))
      jsonlib2.classifier should be equals (None)
      jsonlib3.classifier should be equals (Some("jdk15"))
    }

    it("to path") {
      val loc = local.url(artifact)
      assert(loc == (tempLocalRepo.getAbsolutePath + "/org/beangle/commons/beangle-commons-core_2.12"
        + "/4.6.2/beangle-commons-core_2.12-4.6.2-sources.jar"))
    }
    it("diff path") {
      val diff = Diff(older, "4.6.2")
      val rs = local.url(diff).contains("org/beangle/commons/beangle-commons-core_2.12/4.6.2/beangle-commons-core_2.12-4.6.1_4.6.2-sources.jar.diff")
      rs should be equals true
    }
  }
}
