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
 *
 */

package com.kodebeagle.crawler.metadata.processor.impl

import java.io.{BufferedWriter, FileWriter, File}

import com.kodebeagle.crawler.metadata.GitHubRepoMetadataDownloader
import com.kodebeagle.crawler.metadata.processor.GitHubMetaDataBunchProcessor

import scala.collection.parallel.immutable.ParSeq

class GitHubMetaDataLocalFileAppender extends GitHubMetaDataBunchProcessor {

  protected val metadataDownloadTempDir: String = GitHubRepoMetadataDownloader.tempFolder

  override def process(metaDataBunch: ParSeq[String], from: Int, since: Int, to: Int): Unit = {

    val CONSOLIDATED_FILE_NAME = getFileName(from, to)

    val fileWritter = new FileWriter(this.metadataDownloadTempDir
      + "/" + CONSOLIDATED_FILE_NAME,true)
    val bufferWritter = new BufferedWriter(fileWritter)

    metaDataBunch.foreach { a => bufferWritter.write(a + "\n") }

    bufferWritter.flush()
    bufferWritter.close()
  }


  def getFileName(from: Int, to: Int): String = {
    s"KodeBeagleGitHubMetaData_$from-$to.txt"
  }
}
