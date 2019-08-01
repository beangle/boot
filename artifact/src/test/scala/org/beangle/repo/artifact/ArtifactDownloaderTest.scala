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

import org.beangle.commons.collection.Collections
import org.beangle.commons.io.Dirs
import org.beangle.commons.lang.SystemInfo
import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.funspec.AnyFunSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ArtifactDownloaderTest extends AnyFunSpec with Matchers {

  val tempLocalRepo = new File(SystemInfo.tmpDir + "/.m2/repository")
  tempLocalRepo.mkdirs()
  val downloader = ArtifactDownloader(Repo.Remote.AliyunURL, tempLocalRepo.getAbsolutePath)

  val huaweiloader = ArtifactDownloader("https://mirrors.huaweicloud.com/repository/maven/", tempLocalRepo.getAbsolutePath)
  huaweiloader.authorization("anonymous", "devcloud")

  val slf4j_1_7_24 = new Artifact("org.slf4j", "slf4j-api", "1.7.24", None, "jar")
  val slf4j_1_7_25 = new Artifact("org.slf4j", "slf4j-api", "1.7.25", None, "jar")

  val slf4j_1_8_0 = new Artifact("org.slf4j", "slf4j-api", "1.8.0-beta2", None, "jar")
  val beangle_model_3_6_3 = new Artifact("org.beangle.commons", "beangle-commons-model", "3.6.3", None, "jar")

  describe("artifact downloader") {
    it("can download such jars") {
      Dirs.delete(tempLocalRepo)
      val artifacts = Collections.newBuffer[Artifact]
      artifacts += beangle_model_3_6_3
      downloader.verbose = true

      println("Download int :" + tempLocalRepo)
      downloader.download(artifacts)
      assert(new File(tempLocalRepo.getAbsolutePath +
        "/org/beangle/commons/beangle-commons-model/3.6.3/beangle-commons-model-3.6.3.jar").exists)
      downloader.download(List(Artifact("org.beangle.commons:beangle-commons-model:3.6.4")))
    }
    it("can download with password") {
      Dirs.delete(tempLocalRepo)
      huaweiloader.verbose = false
      huaweiloader.download(List(slf4j_1_8_0))
    }
  }
}
