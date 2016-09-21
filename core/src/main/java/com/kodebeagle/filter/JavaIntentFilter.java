/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kodebeagle.filter;

/**
 * Created by bipulk on 9/20/16.
 */

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.util.Arrays;
import java.util.List;

public final class JavaIntentFilter extends FilteringTokenFilter {

    private final int minLength;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private static final CharArraySet JAVA_STOP_PHRASES_SET;

    static {
        final List<String> stopPhrases = Arrays.asList(

                ".printStackTrace", "System.out.", "System.err."

        );
        final CharArraySet stopSet = new CharArraySet(stopPhrases, false);
        JAVA_STOP_PHRASES_SET = CharArraySet.unmodifiableSet(stopSet);
    }

    private static final CharArraySet JAVA_STOP_WORDS_SET;

    static {
        final List<String> stopPhrases = Arrays.asList(

                "java"

        );
        final CharArraySet stopSet = new CharArraySet(stopPhrases, false);
        JAVA_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet);
    }


    public JavaIntentFilter(TokenStream in, int minLength) {
        super(in);
        this.minLength = minLength;
    }

    @Override
    public boolean accept() {

        String term = termAtt.toString();

        if (termAtt.length() < minLength) {

            return false;
        }
        if (!term.matches("([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*")) {

            return false;

        }
        if (!term.contains(".") && (term.matches("[a-z_$][a-z\\d_$]*") || term.matches("[A-Z_$][A-Z\\d_$]*"))) {

            return false;
        }

        for (int i = 0; i <= termAtt.length(); i++) {

            for (int j = 1; j <= termAtt.length() - i; j++) {

                if (JAVA_STOP_PHRASES_SET.contains(termAtt.buffer(), i, j)) {

                    return false;
                }

                if (JAVA_STOP_WORDS_SET.contains(termAtt.buffer(), i, j)) {

                    return false;
                }
            }

        }

        return true;
    }



}
