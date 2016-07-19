package com.kodebeagle.crawler

import com.kodebeagle.spark.util.SparkIndexJobHelper._
import org.apache.spark.SparkConf

import scala.collection.immutable.IndexedSeq

object GithubMetadataIngestJob {

  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf()
    conf.setAppName("MetadataDownlaoder")
    conf.setMaster("local[*]")
    val sc = createSparkContext(conf)
    val outputPath = /*KodeBeagleConfig.metaDir*/ "/home/keerathj/metadata-test"
    /*val rangeArr = KodeBeagleConfig.metadataRange.split("-").map(_.trim)*/
    val chunkSize = /*KodeBeagleConfig.chunkSize.toInt*/ 1000
    val start = /*rangeArr(0).toInt*/ 0
    val end = /*rangeArr(1).toInt*/ 3000
    val range: List[IndexedSeq[Int]] = (start to end).grouped(chunkSize).toList
    sc.parallelize(range, range.last.last / chunkSize).map { seq =>
      (GitHubApiHelper.getAllGitHubRepos(seq.head)._1
        .filter(x => x("id").toInt <= end && !x("fork").toBoolean) flatMap GitHubApiHelper.fetchAllDetails).mkString("\n")
    }.saveAsTextFile(outputPath)
  }
}
