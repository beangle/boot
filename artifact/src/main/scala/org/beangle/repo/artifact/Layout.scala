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

trait Layout {
  def path(artifact: Artifact): String
  def path(diff: Diff): String
}

object Layout {
  object Maven2 extends Layout {
    def path(a: Artifact): String = {
      "/" + a.groupId.replace('.', '/') + "/" + a.artifactId + "/" + a.version + "/" +
        a.artifactId + "-" + a.version + (if (a.classifier.isEmpty) "" else "-" + a.classifier.get) +
        "." + a.packaging
    }

    def path(d: Diff): String = {
      "/" + d.groupId.replace('.', '/') + "/" + d.artifactId + "/" + d.newVersion +
        "/" + d.artifactId + "-" + d.oldVersion + "_" + d.newVersion +
        (if (d.classifier.isEmpty) "" else ("-" + d.classifier.get)) + "." + d.packaging;
    }
  }

  object Ivy2 extends Layout {
    def path(a: Artifact): String = {
      "/" + a.groupId + "/" + a.artifactId +
        "/jars/" + a.artifactId + "-" + a.version + "." + a.packaging
    }
    def path(d: Diff): String = {
      "/" + d.groupId + "/" + d.artifactId + "/diffs/" +
        d.artifactId + "-" + d.oldVersion + "_" + d.newVersion +
        "." + d.packaging;
    }
  }
}
