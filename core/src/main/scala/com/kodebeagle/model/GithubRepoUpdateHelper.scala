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
package com.kodebeagle.model

import java.io.File

import com.kodebeagle.configuration.KodeBeagleConfig
import com.kodebeagle.logging.Logger
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

import scala.collection.mutable.ListBuffer
import scala.sys.process.ProcessLogger

class GithubRepoUpdateHelper(val configuration: Configuration,
                             val repoPath: String) extends Logger {

  import GithubRepoUpdateHelper._

  import sys.process._

  // TODO: Get these from configuration?
  val remoteUrlPrefix = "https://github.com/"
  val gitDBName = "git.tar.gz"
  val fsRepoDirPath = "/kodebeagle/repos/"

  def fs: FileSystem = FileSystem.get(configuration)

  def localCloneDir: String = KodeBeagleConfig.repoCloneDir

  def fsRepoPath: Path = new Path(join("", fsRepoDirPath, repoPath))

  def localRepoPath: String = join(File.separator, localCloneDir, repoPath)

  def localGitPath: String = join(File.separator, localRepoPath, gitDBName)

  /**
    * Checks if the repository exists on fs, if not then return true.
    * If yes, then checks what its modification time was, if the time since
    * exceeds a configured maximum then repository is marked for update (i.e.
    * false is returned).
    *
    * @return - whether to update (reclone) this repository from Guthub
    */
  def shouldUpdate(): Boolean = {
    val exists = fs.exists(fsRepoPath)
    var shouldUpdate = false
    if (exists) {
      log.info(s"Repo exists at ${fsRepoPath.toString}")
      val filestatus = fs.getFileStatus(new Path(join(File.separator,
        fsRepoPath.toString, gitDBName)))
      val elapsedTime = System.currentTimeMillis - filestatus.getModificationTime
      if (elapsedTime / (1000 * 60 * 60 * 24) > KodeBeagleConfig.repoUpdateFreqDays) {
        log.info(s"Repo exists at ${fsRepoPath.toString} but out of date; will clone")
        shouldUpdate = true
      } else {
        log.info(s"Repo up-t-date at ${fsRepoPath.toString}; will NOT clone")
      }
    } else {
      log.info(s"Repo does not exists at ${fsRepoPath.toString}; will clone")
      shouldUpdate = true
    }
    shouldUpdate
  }

  /**
    * Does the actual updating of a github repo in following steps:
    * 1. Clone the repository on to the local file system.
    * 2. tar it
    * 3. Upload the local tar(s) to fs
    *
    * @return - true if the repo was updated successfully
    */
  def update(): Boolean = {
    val cloneUrl = remoteUrlPrefix + repoPath
    val repoName = repoPath.split("/")(1)

    val cleanClone = buildCloneCommand(repoName, cloneUrl)

    val tarGitCmd = bashCmdsFromDir(localRepoPath,
      Seq(
        s"""cd ${localRepoPath}""",
        s"""tar -zcf git.tar.gz ${repoName}/.git/"""))

    log.info(s"clonig repo ${repoName}")
    log.info("Clean clone command is : " + cleanClone)
    log.info("Tar git command is : " + tarGitCmd)

    val emptyDetector = new EmptyRepoCloneDetector()
    val rtrnCode = cleanClone.!(emptyDetector)

    if (rtrnCode != 0) {
      log.warn(s"${repoPath} does not seem to exist any more. It will be skipped.")
      throw new IllegalStateException("Repo metadata was present but repo no longer exists.")
    }

    if(emptyDetector.isEmpty){
      log.warn(s"${repoPath} seems to be empty. It will be skipped.")
      throw new IllegalStateException("Repo metadata was present but repo was empty.")
    }

    tarGitCmd.!!

    val pathsToCopy = Array(new Path(s"""${localRepoPath}/git.tar.gz"""))

    fs.mkdirs(fsRepoPath)
    fs.copyFromLocalFile(true, true, pathsToCopy, fsRepoPath)

    true
  }

  // TODO: Rename to load()?

  /**
    * @return -- list of files downloaded from hdfs for this repo
    */

  def downloadLocalFromDfs(): List[String] = {
    import sys.process._

    log.info(s"Downloading repo to ${localRepoPath}")
    val files = fs.listStatus(fsRepoPath);
    val fileBuff = ListBuffer[String]()
    val localRepoCrtOp = s"""mkdir -p ${localRepoPath}""".!!
    for (f <- files) {
      val fileName = f.getPath().getName
      val localFilePath: String = join(File.separator, localRepoPath, fileName)
      fs.copyToLocalFile(false, f.getPath, new Path(localFilePath))
      if (f.getPath().getName.endsWith("gz")) {
        val output = s"""tar -xzf ${localFilePath} -C ${localRepoPath}""".!!
        val delOut = s"""rm $localFilePath""".!!
      }

      fileBuff += join(File.separator,
        localRepoPath, repoPath.split("/")(1))
    }
    fileBuff.toList
  }

  def buildCloneCommand(repoName: String, cloneUrl: String): Seq[String] = {
    // TODO: Use case matching
    if (cloneUrl.startsWith("https://github.com/")) {
      bashCmdsFromDir(localRepoPath,
        Seq(
          s"""cd ${localRepoPath}""",
          s"""rm -rf ${repoName}""",
          s"""git clone ${cloneUrl}.git"""),
        true)
    } else {
      bashCmdsFromDir(localRepoPath,
        Seq(
          s"""cd ${localRepoPath}""",
          s"""rm -rf ${repoName}""",
          s"""cp -r ${cloneUrl} ."""),
        true)
    }

  }

}

class EmptyRepoCloneDetector extends ProcessLogger {

  var isEmpty = false

  override def out(s: => String): Unit = {if (s.contains("empty repository")) isEmpty = true}

  override def err(s: => String): Unit = {if (s.contains("empty repository")) isEmpty = true}

  override def buffer[T](f: => T): T = f
}

object GithubRepoUpdateHelper {

  def bashCmdsFromDir(dir: String, cmds: Seq[String],
                      createDir: Boolean = false): Seq[String] = {
    val base = Seq("/bin/bash", "-c")
    val firstCmd = if (createDir) s"""mkdir -p ${dir}""" else ""
    base :+ cmds.foldLeft(firstCmd)((a, b) => join(" && ", a, b))
  }

  def join(sep: String, elements: String*): String = {
    val sb = new StringBuilder
    for (e <- elements) {
      if (sb.length > 0 && e.length > 0) {
        sb.append(sep)
      }
      sb.append(e)
    }
    sb.toString
  }
}
