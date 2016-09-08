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

import scala.util.Try


/**
  * Created by bipulk on 8/30/16.
  */
class StackoverflowRawPost(idParam: Long, parentIdParam: Long, postTypeParam: Int,
                           rawPostDataParam: Array[Char]) extends Serializable {

  val id = idParam
  val parentId = parentIdParam
  val postTypeId = postTypeParam
  val rawPostData = rawPostDataParam


  override def toString: String = {

    val rawPostDataString = StringBuilder.newBuilder
    for (elem <- rawPostData) {

      rawPostDataString.append(elem)

    }

    s"StackoverflowRawPost(id=$id, parentId=$parentId, " +
      s"postTypeId=$postTypeId, rawPostData=${rawPostDataString.toString})"
  }
}

object StackoverflowRawPost {

  def apply(rawPostString: String): Option[StackoverflowRawPost] = {

    Try {
      val id = extractPostInfo(rawPostString, "Id").get.toLong
      val postTypeId = extractPostInfo(rawPostString, "PostTypeId").get.toInt
      val parentId = extractPostInfo(rawPostString, "ParentId").getOrElse(s"$id").toLong
      new StackoverflowRawPost(id, parentId, postTypeId, rawPostString.toCharArray)
    }.toOption
  }

  def extractPostInfo(rawPostString: String, key: String): Option[String] = {

    val actualKey = " " + key + "=\""
    val index = rawPostString.indexOf(actualKey)

    val startIndex = index + actualKey.length
    val value = if (index != -1) rawPostString.substring(startIndex,
      rawPostString.indexOf("\"", startIndex))
    else ""

    value match {

      case "" => None
      case _ => Some(value)

    }
  }

  def extractTags(tagsString: String): Array[String] = {

    tagsString.split("&gt;&lt;").map(x => x.trim)
        .map(y => y.replace("&lt;","")).map(z => z.replace("&gt;",""))

  }
}

class StackoverflowAnswer(idParam: Long, scoreParam: Int,
                          bodyParam: Array[Char]) extends Serializable {

  val id = idParam
  val score = scoreParam
  val body = bodyParam


  override def toString: String = {
    val rawBodyString = StringBuilder.newBuilder
    for (elem <- body) {
      rawBodyString.append(elem)
    }

    s"""StackoverflowAnswer(id="$id", score="$score", body="${rawBodyString.toString}")"""
  }
}

object StackoverflowAnswer {

  def apply(rawPost: StackoverflowRawPost): StackoverflowAnswer = {

    val rawPostString = StringBuilder.newBuilder
    for (elem <- rawPost.rawPostData) {

      rawPostString.append(elem)

    }

    val rawPostDataString = rawPostString.toString

    val id = StackoverflowRawPost.extractPostInfo(rawPostDataString,"Id").get.toLong
    val score = StackoverflowRawPost.extractPostInfo(rawPostDataString,"Score").
          getOrElse(s"0").toInt
    val body = StackoverflowRawPost.extractPostInfo(rawPostDataString,"Body").get.toCharArray

    new StackoverflowAnswer(id, score, body)

  }
  
}


class StackoverflowPost(idParam: Long, titleParam: String, bodyParam: Array[Char],
                        scoreParam: Int, answerCountParam: Int, favCountParam: Int,
                        viewCountParam: Long, acceptedAnswerIdParam: Long,
                        tagsParam: List[String], answersParam: List[StackoverflowAnswer])
  extends Serializable {

  val id = idParam
  val title = titleParam
  val body = bodyParam
  val score = scoreParam
  val answerCount = answerCountParam
  val favCount = favCountParam
  val viewCount = viewCountParam
  val acceptedAnswerId = acceptedAnswerIdParam
  val tags = tagsParam
  val answers = answersParam


  override def toString: String = {
    val rawBodyString = StringBuilder.newBuilder
    for (elem <- body) {

      rawBodyString.append(elem)

    }

    s"""StackoverflowPost(id="$id", title="$title", body="${rawBodyString.toString()}",
    | score="$score", answerCount="$answerCount", favCount="$favCount",
    | viewCount="$viewCount", acceptedAnswerId="$acceptedAnswerId",
    | tags="$tags", answers="$answers")""".stripMargin.replaceAll("\n", "")

  }

}


object StackoverflowPost {

  def apply(rawStackOverflowPostList: Iterable[StackoverflowRawPost]): Option[StackoverflowPost] = {

    Try {
      val answers = rawStackOverflowPostList.filter(rawPost => rawPost.id != rawPost.parentId).
        map(answerPost => StackoverflowAnswer(answerPost))

      val question = rawStackOverflowPostList.filter(rawPost => rawPost.id == rawPost.parentId).
          head.rawPostData

      val rawPostString = StringBuilder.newBuilder
      for (elem <- question) {
        rawPostString.append(elem)
      }

      val rawPostDataString = rawPostString.toString

      val id = StackoverflowRawPost.extractPostInfo(rawPostDataString,"Id").get.toLong
      val title = StackoverflowRawPost.extractPostInfo(rawPostDataString,"Title").get
      val body = StackoverflowRawPost.extractPostInfo(rawPostDataString,"Body").get
      val score = StackoverflowRawPost.extractPostInfo(rawPostDataString,"Score").
        getOrElse(s"0").toInt
      val answerCount = StackoverflowRawPost.extractPostInfo(rawPostDataString,"AnswerCount").
        getOrElse(s"0").toInt
      val favCount = StackoverflowRawPost.extractPostInfo(rawPostDataString,"FavoriteCount").
        getOrElse(s"0").toInt
      val viewCount = StackoverflowRawPost.extractPostInfo(rawPostDataString,"ViewCount").
        getOrElse(s"0").toLong
      val acceptedAnswerId = StackoverflowRawPost.extractPostInfo(rawPostDataString,
        "AcceptedAnswerId").getOrElse(s"-1").toLong
      val tags = StackoverflowRawPost.extractTags(StackoverflowRawPost.
                extractPostInfo(rawPostDataString,"Tags").get).toList

      new StackoverflowPost(id,title,body.toCharArray,score,answerCount,
            favCount,viewCount,acceptedAnswerId,tags,answers.toList)

    }.toOption

  }

}

