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

package com.kodebeagle.util

import java.util.regex.Pattern

import com.kodebeagle.lucene.tokenizer.{JavaIntentFieldAnalyzer, JavaIntentSearchFieldAnalyzer}
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.{Analyzer, TokenStream}
import org.apache.lucene.util.Version

import scala.collection.mutable

/**
  * Created by bipulk on 9/16/16.
  */
object LuceneTokenizerUtils {

  def cleanAndTokenizeForJavaIntentField(rawString: String): String = {

    val javaIntentAnalyzer: Analyzer = new JavaIntentFieldAnalyzer()

    javaIntentAnalyzer.setVersion(Version.LATEST)

    // scalastyle:off
    val tokenStream: TokenStream = javaIntentAnalyzer.tokenStream(null, rawString)

    val tokens = extractTokens(tokenStream).map(token =>

      if (Pattern.compile("^[a-z]+([A-Za-z])+\\.[a-z]+([A-Za-z])+$").matcher(token).find()) {
        token.substring(token.indexOf('.'))
      }
      else {
        token
      }
    )

    tokens mkString " "

  }

  def cleanAndTokenizeForJavaIntentSearchField(rawString: String): String = {

    val stadardAnalyzer: Analyzer = new JavaIntentSearchFieldAnalyzer()

    stadardAnalyzer.setVersion(Version.LATEST)

    // scalastyle:off
    val tokenStream: TokenStream = stadardAnalyzer.tokenStream(null, rawString)

    val tokens = extractTokens(tokenStream)

    tokens mkString " "

  }

  private def extractTokens(tokenStream: TokenStream): Set[String] = {
    val charTermAttribute: CharTermAttribute =
      tokenStream.addAttribute[CharTermAttribute](classOf[CharTermAttribute])

    val list = mutable.MutableList[String]()

    tokenStream.reset()

    while (tokenStream.incrementToken()) {

      val token = charTermAttribute.toString

      list += token

    }

    tokenStream.end()
    tokenStream.close()

    list.toSet
  }

}
