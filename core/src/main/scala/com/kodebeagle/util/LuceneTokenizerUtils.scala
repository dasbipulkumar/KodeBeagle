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

import com.kodebeagle.tokenizer.{JavaIntentFieldAnalyzer, JavaIntentSearchFieldAnalyzer}
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


  def main(args: Array[String]) {

    val txt = "Some(StackoverflowPost(id=\"14487075\", title=\"Java + Apache Commons Net: Read FTPFile Without Downloading\", body=\"&lt;p&gt;Using Apache Commons Net 3.2, my program is connecting to an FTP server and downloading files from it.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;What it should be able to do, however, is to read the files on the server WITHOUT downloading them.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;Is this even possible?&lt;/p&gt;&#xA;&#xA;&lt;p&gt;It's just that the server contains lots of personal information, SSN, phone, email, etc, and only specific people with the right password should be able to access them.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;No one should be able to download anything from the server, at least not without the highest level of permissions granted in my program!&lt;/p&gt;&#xA;&#xA;&lt;p&gt;So far I have an FTPFile [] with all the data files on the server.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;I want to loop through them, see if they have the current user's name on it (meaning they're allowed to view this person/file), and if so, add their data to an ArrayList .&lt;/p&gt;&#xA;&#xA;&lt;p&gt;Any tips?&lt;/p&gt;&#xA;&#xA;&lt;pre&gt;&lt;code&gt;import java.io.File;&#xA;import java.io.FileOutputStream;&#xA;import java.util.List;&#xA;import java.util.Arrays;&#xA;&#xA;import org.apache.commons.net.ftp.FTPClient;&#xA;import org.apache.commons.net.ftp.FTPFile;&#xA;import org.apache.commons.net.ftp.FTPReply;&#xA;&#xA;public class Server {&#xA;    private static final String server = &quot;/server&quot;;&#xA;    private static final String host = &quot;00.000.0.0&quot;;&#xA;    private static final String user = &quot;username&quot;;&#xA;    private static final String pass = &quot;password&quot;;&#xA;    private static List &amp;lt;FTPFile&amp;gt; data;&#xA;    private static FTPClient ftp = new FTPClient ();&#xA;&#xA;    public static void load (String folder) {&#xA;        try {&#xA;            // Connect to the SERVER&#xA;            ftp.connect(host, 21);&#xA;            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {&#xA;                System.out.println(&quot;Could not connect to the server.&quot;);&#xA;                return;&#xA;            }&#xA;&#xA;            // Login to the SERVER&#xA;            ftp.enterLocalPassiveMode();&#xA;            if (!ftp.login(user, pass)) {&#xA;                System.out.println(&quot;Could not login to the server.&quot;);&#xA;                return;&#xA;            }&#xA;&#xA;            // Get DATA from the SERVER&#xA;            System.out.println(server + &quot;/&quot; + folder);&#xA;            data = Arrays.asList(ftp.listFiles(server + &quot;/&quot; + folder));&#xA;            System.out.println(data.size());&#xA;            for (int f = 0; f &amp;lt; data.size(); f++)&#xA;                System.out.println(data.get(f).getName());&#xA;&#xA;            // Disconnect from the SERVER&#xA;            ftp.logout();&#xA;            ftp.disconnect();&#xA;&#xA;        } catch (Exception e) {&#xA;            e.printStackTrace();&#xA;        }&#xA;    }&#xA;&#xA;    public static String read (FTPFile file) {&#xA;        try {&#xA;            String name = file.getName();&#xA;            File tempFolder = new File (&quot;temp/&quot; + name);&#xA;            tempFolder.mkdirs();&#xA;&#xA;            // Create a TEMPORARY DATA FILE&#xA;            File tempFile = new File (tempFolder.getAbsolutePath() + &quot;/data&quot;);&#xA;            System.out.println(tempFile.getAbsolutePath());&#xA;            tempFile.createNewFile();&#xA;            tempFile.deleteOnExit();&#xA;&#xA;            // Get ready to DOWNLOAD DATA from the SERVER&#xA;            FileOutputStream out = new FileOutputStream (new File (name));&#xA;            ftp.connect(host, 21);&#xA;            ftp.login(user,  pass);&#xA;&#xA;            // DOWNLOAD and DISCONNECT&#xA;            ftp.retrieveFile(name, out);&#xA;            out.close();&#xA;            ftp.logout();&#xA;            ftp.disconnect();&#xA;        } catch (Exception e) {&#xA;            e.printStackTrace();&#xA;        }&#xA;        return &quot;&quot;; // This should return a String with data read from the file&#xA;    }&#xA;}&#xA;&lt;/code&gt;&lt;/pre&gt;&#xA;\", score=\"0\", answerCount=\"2\", favCount=\"0\", viewCount=\"1662\", acceptedAnswerId=\"14487577\", tags=\"List(java, apache-commons-net, readfile)\", answers=\"List(StackoverflowAnswer(id=\"14487569\", score=\"3\", body=\"&lt;p&gt;No it is not possible to read a file from clientside over ftp without downloading it first.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;Yoou will have to install software at (ftp-) serverside.&lt;/p&gt;&#xA;\"), StackoverflowAnswer(id=\"14487577\", score=\"4\", body=\"&lt;p&gt;If you want to &quot;read&quot; (as in inspect its content) a file you have to download, i.e. transfer its contents from the server to the client. There is no way around it, when you are restricted to a client sided solution.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;In general it is a bad idea to check user permissions in the client only. If a malicious user has the correct credentials to access the server (that could be extracted from the client), he can circumvent the client sided authorization, rendering it useless.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;Authentication and authorization of users should always occur server sided.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;In your example you could consider the creation of different users on the ftp server and appropriate rights to access, read and write files / directories.&lt;/p&gt;&#xA;\"))\"))"


    println(cleanAndTokenizeForJavaIntentField(txt))

    println(cleanAndTokenizeForJavaIntentSearchField(txt))

  }

}
