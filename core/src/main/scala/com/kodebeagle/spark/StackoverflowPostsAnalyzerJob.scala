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
package com.kodebeagle.spark

import com.kodebeagle.configuration.KodeBeagleConfig
import com.kodebeagle.indexer.StackoverflowIntentIndicies
import com.kodebeagle.model.{StackoverflowIndex, StackoverflowPost, StackoverflowRawPost}
import com.kodebeagle.util.SparkIndexJobHelper._
import org.apache.spark.SparkConf

/**
  * Created by bipulk on 8/31/16.
  */
object StackoverflowPostsAnalyzerJob {

  def main(args: Array[String]) {

    val conf = new SparkConf()
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .setMaster(KodeBeagleConfig.sparkMaster)
      .setAppName("StackoverflowPostsAnalyzerJob")
    val sc = createSparkContext(conf)
    sc.hadoopConfiguration.set("dfs.replication", "1")

    // val stackoverflowPostsDataPath = "/home/bipulk/Development/data/Stackoverflow_dump/Posts.xml"

    val rawDataRdd = sc.textFile(KodeBeagleConfig.stackoverflowRawDataPath).
           flatMap(dataLine => StackoverflowRawPost(dataLine)).
            map(rawPostObj => (rawPostObj.parentId,rawPostObj)).
            groupByKey().
            map(x => StackoverflowPost(x._2)).filter(_.isDefined).
            filter(_.get.score > 0).filter(_.get.answerCount > 0).
            map(y => StackoverflowIndex(y.get)).filter(_.isDefined).
            filter(_.get.tags.contains("java")).
            map(z => toIndexTypeJson("java", "stackoverflow",
              StackoverflowIntentIndicies(z.get.id, z.get.title,
                z.get.searchTokens, z.get.intentTokens, z.get.tags, z.get.score))).
            saveAsTextFile(s"${KodeBeagleConfig.repoIndicesHdfsPath}Java/stackoverflow")

  }

}
