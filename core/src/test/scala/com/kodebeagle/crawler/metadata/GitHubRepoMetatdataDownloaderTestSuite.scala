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


package com.kodebeagle.crawler.metadata

import java.io.File

import com.kodebeagle.crawler.GitHubApiHelper
import com.kodebeagle.crawler.GitHubApiHelper._
import com.kodebeagle.crawler.metadata.processor.impl.{GitHubMetaDataLocalCopier, GitHubMetaDataLocalFileAppender}
import org.scalatest.FunSuite

import scala.collection.parallel.immutable.ParSeq
import scala.io.Source


/**
  * Created by bipulk on 18/7/16.
  *
  */
class GitHubRepoMetatdataDownloaderTestSuite extends FunSuite {

  val metaDir = "/tmp/kodebeagle/test/metadata/"
  val hdfsNameNode = "hdfs://ldap.localcluster.net:8020"
  val metaDirHdfs = "/user/bipulk/test/kodebeagle/metadata/"
  val start = 0
  val end = 100
  val testList: List[Long] = List(1, 26, 27, 28, 29, 31, 35, 36, 42, 43, 47,
    48, 52, 53, 54, 56, 61, 63, 65, 68, 71, 73, 74, 75, 92, 93, 95)

  var repoMetadataList: ParSeq[String] = List().par
  var repoMetadataJsonList: List[Long] = List()

  GitHubApiHelper.token = "98aa6a7a0ccf8bff08728ee6befc9fe14c6fc7d4"

  val startTime = System.currentTimeMillis()

  test("GitHubMetaDataDownloader") {

    val (allGithubRepos, next) = getAllGitHubRepos(start)
    repoMetadataList = allGithubRepos.filter(x => x("fork") == "false")
      .par.flatMap(fetchAllDetails)

    repoMetadataJsonList = repoMetadataList.toList
      .map(a => a.substring(a.indexOf(':') + 1, a.indexOf(",")).toLong)
      .filter(a => a <= end).sortWith((a: Long, b: Long) => {
      a < b
    })

    var i = 0
    for (a <- repoMetadataJsonList) {

      assert(a == repoMetadataJsonList(i))

      i = i + 1
    }

  }

  test("Test the GitHubMetaDataLocalDownloader") {

    val fileAppender: GitHubMetaDataLocalFileAppender =
      new GitHubMetaDataLocalFileAppender()

    fileAppender.process(repoMetadataList, start, start, end)
    val file: File = new File(GitHubRepoMetadataDownloader.tempFolder +
      fileAppender.getFileName(start, end))

    assert(file.lastModified() > startTime)

    val lines = Source.fromFile(file).getLines().toList
      .map(a => a.substring(a.indexOf(':') + 1, a.indexOf(",")).toLong)
      .filter(a => a <= end).sortWith((a: Long, b: Long) => {
      a < b
    })

    var i = 0
    for (b <- lines) {
      assert(b == repoMetadataJsonList(i))
      i = i + 1
    }
  }

  test("Test the GitHubMetaDataLocalCopier") {

    val localCopier = new MockGitHubMetaDataLocalCopier(metaDir)
    localCopier.process(start, end)
    val file: File = new File(metaDir +
      new GitHubMetaDataLocalFileAppender().getFileName(start, end))

    assert(file.lastModified() > startTime)

    val lines = Source.fromFile(file).getLines().toList
      .map(a => a.substring(a.indexOf(':') + 1, a.indexOf(",")).toLong)
      .filter(a => a <= end).sortWith((a: Long, b: Long) => {
      a < b
    })

    var i = 0
    for (b <- lines) {
      assert(b == repoMetadataJsonList(i))
      i = i + 1

    }
  }

  test("Test the persitent task tracker"){



  }
}

class MockGitHubMetaDataLocalCopier(metaDataDirc: String) extends
  GitHubMetaDataLocalCopier {

  override val metadataDir: String = metaDataDirc


}

