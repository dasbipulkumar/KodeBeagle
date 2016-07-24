package com.kodebeagle.crawler

import java.io.{BufferedWriter, OutputStreamWriter}
import java.net.URI

import com.kodebeagle.crawler.GithubApiHelper._
import com.kodebeagle.spark.util.SparkIndexJobHelper._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.SparkConf

object GithubMetadataIngestJob {

  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf()
    conf.setMaster("spark://prinhydspln0153:7077")
    conf.setAppName("GithubMetadataIngest")
    conf.set("spark.executor.memory", "10g")
    val sc = createSparkContext(conf)
    val outputPath = "hdfs://192.168.2.145:9000/metadata-test1"
    /*val rangeArr = KodeBeagleConfig.metadataRange.split("-").map(_.trim)*/
    val chunkSize = 1000
    val start = /*rangeArr(0).toInt*/ 0
    val end = /*rangeArr(1).toInt*/ 3000
    val range = (start to end).grouped(chunkSize).toList
    sc.parallelize(range, range.last.last / chunkSize).map { seq =>
      (start, end, queryRepoNamesWithInRange(seq.head, seq.last)
        .flatMap(queryRepoDetails)
        .mkString("\n"))
    }.foreachPartition { partition =>
      val config = new Configuration()
      conf.set("dfs.replication", "1")
      val next: (Int, Int, String) = partition.next()
      val fs: FileSystem = FileSystem.get(URI.create(s"$outputPath/${next._1 - next._2}"), config)
      val writer = new BufferedWriter(new OutputStreamWriter(fs.create(new Path(outputPath))))
      partition.foreach { record =>
        writer.write(record._3)
        writer.newLine()
      }
      writer.close()
    }
  }
}


