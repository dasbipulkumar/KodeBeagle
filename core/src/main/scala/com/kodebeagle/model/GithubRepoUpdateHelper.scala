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
import org.apache.hadoop.fs.{Path, FileSystem}

import scala.collection.mutable.ListBuffer

class GithubRepoUpdateHelper(val configuration: Configuration,
                             val repoPath: String) extends Logger {

  import sys.process._
  import GithubRepoUpdateHelper._

  // TODO: Get these from configuration?
  val remoteUrlPrefix = "https://github.com/"
  val masterBranchName = "master"
  val gitDBName = "git"
  val fsRepoDirPath = "/kodebeagle/repos/"

  def fs: FileSystem = FileSystem.get(configuration)

  def localCloneDir: String = KodeBeagleConfig.repoCloneDir

  def fsRepoPath: Path = new Path(join("", fsRepoDirPath, repoPath))

  def localRepoPath: String = join(File.separator, localCloneDir, repoPath)

  def localBranchPath: String = join(File.separator, localRepoPath, masterBranchName)

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
      val filestatus = fs.getFileStatus(new Path(join(File.separator, fsRepoDirPath, gitDBName)))
      val elapsedTime = System.currentTimeMillis - filestatus.getModificationTime
      if (elapsedTime / (1000 * 60 * 60 * 24) > KodeBeagleConfig.repoUpdateFreqDays) {
        shouldUpdate = true
      }
    } else {
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

    val tarMasterCmd = bashCmdsFromDir(localRepoPath,
      Seq(
        s"""cd ${localRepoPath}""",
        s"""tar -zcf master.tar.gz ${repoName}/ --exclude "${repoName}/.git""""))

    val tarGitCmd = bashCmdsFromDir(localRepoPath,
      Seq(
        s"""cd ${localRepoPath}""",
        s"""tar -zcf git.tar.gz ${repoName}/.git/"""))

    log.debug("Clean clone command is : " + cleanClone)
    log.debug("Tar master command is : " + tarMasterCmd)
    log.debug("Tar git command is : " + tarGitCmd)

    cleanClone.!!
    tarMasterCmd.!!
    tarGitCmd.!!

    val pathsToCopy = Array(new Path(s"""${localRepoPath}/master.tar.gz"""),
      new Path(s"""${localRepoPath}/git.tar.gz"""))

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

    val files = fs.listStatus(fsRepoPath);
    val fileBuff = ListBuffer[String]()
    val localRepoCrtOp = Seq("/bin/bash", "-c", s"""mkdir -p ${localRepoPath})""").!!
    log.info(localRepoCrtOp)
    for (f <- files) {
      val fileName = f.getPath().getName
      f.getModificationTime
      val localFilePath = join(File.separator, localRepoPath, fileName)
      fs.copyToLocalFile(false, f.getPath, new Path(localFilePath))
      if (f.getPath().getName.endsWith("gz")) {
        val output = s"""tar -xzf ${localFilePath} -C $localRepoPath""".!!
        log.info(output)

        val delOut = s"""rm $localFilePath"""
        log.info(delOut)
      }

      fileBuff += join(File.separator,
        localRepoPath, fileName.substring(0, fileName.indexOf("\\.")))
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
