package org.beangle.boot.dependency

import org.beangle.boot.artifact.Archive
import org.beangle.commons.collection.Collections

import java.io.{InputStreamReader, LineNumberReader}
import java.net.URL

object DependencyResolver {
  def resolve(resource: URL): Iterable[Archive] = {
    val archives = Collections.newBuffer[Archive]
    if (null == resource) return List.empty
    try {
      val reader = new InputStreamReader(resource.openStream())
      val lr = new LineNumberReader(reader)
      var line: String = null
      do {
        line = lr.readLine()
        if (line != null && line.nonEmpty) {
          archives += Archive(line)
        }
      } while (line != null)
      lr.close()
    } catch {
      case e: Exception => e.printStackTrace()
    }
    archives
  }
}
