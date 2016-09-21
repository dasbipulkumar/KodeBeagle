/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kodebeagle.lucene

import com.kodebeagle.util.LuceneTokenizerUtils
import org.scalatest.FunSuite

import scala.io.Source

/**
  * Created by bipulk on 9/21/16.
  */
class LuceneJavaIntentTokenizerTestSuite extends FunSuite {


  val stream = this.getClass.getResourceAsStream("/LuceneCustomTokenizerTestFile.txt")
  val fileContent = Source.fromInputStream(stream).mkString
  // scalastyle:off
  test("test cleanAndTokenizeForJavaIntentField") {

    val tokenizedResult =
      s"""org.apache.commons.net.ftp.FTPFile .enterLocalPassiveMode
          | Apache .getAbsolutePath tempFile tempFolder Using This Server .connect Could FTPClient
          | Connect FileOutputStream .get .size .disconnect org.apache.commons.net.ftp.FTPReply .deleteOnExit Exception
          | getName .getReplyCode File Login FTPFile .logout FTPReply.isPositiveCompletion What Disconnect Commons .login
          | .getName Arrays.asList .createNewFile ArrayList .listFiles .retrieveFile List org.apache.commons.net.ftp.FTPClient
          | Create .mkdirs .close String""".stripMargin.replaceAll("\n", "")


    assert(tokenizedResult == LuceneTokenizerUtils.cleanAndTokenizeForJavaIntentField(fileContent))

  }
  // scalastyle:off
  test("test cleanAndTokenizeForJavaIntentSearchField") {

    val tokenizedResult =
      s"""ftp static allowed e pre read able for program lots Apache people
          | name this in have is loop
          | It's data get server try tempFile private TEMPORARY tempFile deleteOnExit tempFolder want
          | Using email connect This Server FTPReply isPositiveCompletion
          |  username Get return if
          | DOWNLOAD downloading etc so do java util Arrays all java io File host f ftp listFiles
          | ready data Could void just out close FTPClient it user's a Connect
          |  information FileOutputStream
          | e printStackTrace permissions data size load DATA ftp enterLocalPassiveMode
          |  FTP java io FileOutputStream
          | tempFolder mkdirs public ftp getReplyCode they far amp 21 I tempFile createNewFile
          |  files that out Exception
          | getName No to tempFolder getAbsolutePath least code 00 000 0 0 DISCONNECT File
          |  Net granted personal Login
          | at through FTPFile import Arrays asList folder tempFile getAbsolutePath
          |  on highest my Is 3 2 temp ftp logout
          | meaning What Disconnect them Commons final even however should new int their
          |  p So specific not with from
          | 0 person ArrayList pass tips class file quot ftp retrieveFile an FILE java
          |  util List SERVER be connecting
          | anything List login they're ftp login org apache commons net ftp FTPClient
          |  ftp disconnect WITHOUT catch
          | contains current SSN System out println Create xA see lt add gt download
          |  file getName user possible
          | Any of and org apache commons net ftp FTPFile one without ftp connect right
          |  phone access view level
          | String the password org apache commons net ftp FTPReply only""".
            stripMargin.replaceAll("\n", "").replaceAll("  "," ")


    assert(tokenizedResult == LuceneTokenizerUtils.
      cleanAndTokenizeForJavaIntentSearchField(fileContent))

  }
}


