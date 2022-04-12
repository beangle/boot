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

package org.beangle.boot.artifact.util

import org.beangle.commons.codec.binary.Hex
import org.beangle.commons.codec.digest.Digests
import org.beangle.commons.file.diff.Bsdiff
import org.beangle.commons.io.IOs
import org.beangle.commons.lang.{Charsets, Strings}

import java.io.{BufferedReader, File, FileInputStream, InputStreamReader}
import java.security.MessageDigest

object Delta {

  def diff(oldFile: String, newFile: String, diffFile: String): Unit = {
    Bsdiff.diff(new File(oldFile), new File(newFile), new File(diffFile))
  }

  def patch(oldFile: String, newFile: String, patchFile: String): Unit = {
    Bsdiff.patch(new File(oldFile), new File(newFile), new File(patchFile))
  }

  def sha1(fileLoc: String): String = {
    val crypt =MessageDigest.getInstance("SHA-1")
    val input = new FileInputStream(fileLoc)
    val buffer = new Array[Byte](4 * 1024)
    var n = input.read(buffer)
    while (-1 != n) {
      crypt.update(buffer, 0, n)
      n = input.read(buffer)
    }
    IOs.close(input)
    Hex.encode(crypt.digest(), true)
  }

  def verifySha1(fileLoc: String, sha1File: String): Boolean = {
    val sha1inFile = IOs.readString(new FileInputStream(sha1File), Charsets.UTF_8).trim().toLowerCase()
    val sha1Calculated = sha1(fileLoc)
    sha1inFile.contains(sha1Calculated)
  }

}
