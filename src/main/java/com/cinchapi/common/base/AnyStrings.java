/*
 * Copyright (c) 2015 Cinchapi Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cinchapi.common.base;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * A collection of functions that efficiently operate on {@link String strings}.
 * <p>
 * In some cases, we provide optimized implementations of functionality that
 * exists within the {@link String} class itself or other frameworks.
 * </p>
 * 
 * @author Jeff Nelson
 */
public class AnyStrings {

    /**
     * The set of all unicode double quotation mark characters
     *
     * See <a href=
     * "http://www.unicode.org/Public/security/revision-03/confusablesSummary.txt"
     * >http://www.unicode.org/Public/security/revision-03/confusablesSummary.
     * txt</a> for the list of characters
     * </p>
     **/
    private static final Set<Character> DOUBLE_QUOTE_UNICODE_CHARS = ImmutableSet
            .of('ʺ', '˝', 'ˮ', '˶', 'ײ', '״', '“', '”', '‟', '″', '‶', '〃',
                    '＂');
    /**
     * The set of all unicode single quotation mark characters
     *
     * See <a href=
     * "http://www.unicode.org/Public/security/revision-03/confusablesSummary.txt"
     * >http://www.unicode.org/Public/security/revision-03/confusablesSummary.
     * txt</a> for the list of characters
     * </p>
     **/
    private static final Set<Character> SINGLE_QUOTE_UNICODE_CHARS = ImmutableSet
            .of('`', 'ꞌ', 'ʻ', 'ʼ', 'י', 'ʹ', 'ʽ', 'ʾ', 'ˊ', 'ˋ', 'ߴ', 'ߵ', 'ʹ',
                    '׳', '’', '˴', '՚', '՝', '‘', '‛', '′', '‵', '´', '΄', '᾽',
                    '᾿', '`', '´', '῾', '＇', '｀');

    /**
     * The start of a placeholder sequence for the
     * {@link #format(String, Object...)} method.
     */
    private static final char PLACEHOLDER_BEGIN = '{';

    /**
     * The end of a placeholder sequence for the
     * {@link #format(String, Object...)} method.
     */
    private static final char PLACEHOLDER_END = '}';

    /**
     * Perform a {@link String#compareToIgnoreCase(String) case insensitive
     * comparison} between {@code s1} and {@code s2}.
     * 
     * @param s1
     * @param s2
     * @return the result of the comparison
     */
    public static int compareToIgnoreCase(String s1, String s2) {
        return s1.compareToIgnoreCase(s2);
    }

    /**
     * Ensure that {@code string} ends with {@code suffix} by appending it to
     * {@code string} if and only if it is not already the last sequence of
     * characters in the string.
     * 
     * @param string the {@link String} to that should end with {@code suffix}
     * @param suffix the {@link String} of characters with which {@code string}
     *            should end
     * @return {@code string} if it already ends with {@code suffix} or a new
     *         {@link String} that contains {@code suffix} appended to
     *         {@code string}
     */
    public static String ensureEndsWith(String string, String suffix) {
        if(string.endsWith(suffix)) {
            return string;
        }
        else {
            return joinSimple(string, suffix);
        }
    }

    /**
     * Ensure that {@code string} starts with {@code prefix} by prepending it to
     * {@code string} if and only if it is not already the first sequence of
     * characters in the string.
     * 
     * @param string the {@link String} to that should start with {@code prefix}
     * @param prefix the {@link String} of characters with which {@code string}
     *            should start
     * @return {@code string} if it already begins with {@code prefix} or a new
     *         {@link String} that contains {@code prefix} prepended to
     *         {@code string}
     */
    public static String ensureStartsWith(String string, String prefix) {
        if(string.startsWith(prefix)) {
            return string;
        }
        else {
            return joinSimple(prefix, string);
        }
    }

    /**
     * Ensure that {@code string} is surrounded by quotes. If that is not the
     * case, alter the string so that it is and return the altered form.
     * 
     * <p>
     * Calling {@link Strings#isWithinQuotes(String)} on the result of this
     * method will always return {@code true}.
     * </p>
     * 
     * @param string the string that must be quoted
     * @return {@code string} or {@code string} surrounded by quotes if it is
     *         not already
     */
    public static String ensureWithinQuotes(String string) {
        return isWithinQuotes(string) ? string : joinSimple("\"", string, "\"");
    }

    /**
     * Wrap {@code string} within quotes if it is necessary to do so. Otherwise,
     * return the original {@code string}.
     * 
     * <p>
     * The original {@code string} will be wrapped in quotes and returned as
     * such if:
     * <ul>
     * <li>it is not already wrapped {@link #isWithinQuotes(String) within
     * quotes}, and</li>
     * <li>{@code delimiter} appears at least once</li>
     * </ul>
     * If those conditions are met, the original string will be wrapped in
     * either
     * <ul>
     * <li>double quotes if a single quote appears in the original string,
     * or</li>
     * <li>single quotes if a double quote appears in the original string,
     * or</li>
     * <li>double quotes if both a single and double quote appear in the
     * original string; furthermore, all instances of double quotes within the
     * original string will be escaped</li>
     * </ul>
     * </p>
     * 
     * @param string the string to potentially quote
     * @param delimiter the delimiter that determines whether quoting should
     *            happen
     * @return the original {@code string} or a properly quoted alternative
     */
    public static String ensureWithinQuotesIfNeeded(String string,
            char delimiter) {
        boolean foundDouble = false;
        boolean foundSingle = false;
        boolean foundDelimiter = false;
        StringBuilder escaped = new StringBuilder();
        escaped.append('"');
        if(!isWithinQuotes(string)) {
            char[] chars = string.toCharArray();
            for (int i = 0; i < chars.length; ++i) {
                char c = chars[i];
                if(c == delimiter) {
                    foundDelimiter = true;
                }
                else if(c == '"') {
                    foundDouble = true;
                    escaped.append('\\');
                }
                else if(c == '\'') {
                    foundSingle = true;
                }
                escaped.append(c);
            }
            escaped.append('"');
            if(foundDelimiter) {
                if(foundDouble && foundSingle) {
                    return escaped.toString();
                }
                else if(foundDouble) {
                    return format("'{}'", string);
                }
                else { // foundSingle or found no quotes
                    return format("\"{}\"", string);
                }
            }
        }
        return string;
    }

    /**
     * Wrap {@code string} within quotes if it is necessary to do so. Otherwise,
     * return the original {@code string}.
     * 
     * <p>
     * The original {@code string} will be wrapped in quotes and returned as
     * such if:
     * <ul>
     * <li>it is not already wrapped {@link #isWithinQuotes(String) within
     * quotes}, and</li>
     * <li>{@code delimiter} appears at least once</li>
     * </ul>
     * If those conditions are met, the original string will be wrapped in
     * either
     * <ul>
     * <li>double quotes if a single quote appears in the original string,
     * or</li>
     * <li>single quotes if a double quote appears in the original string,
     * or</li>
     * <li>double quotes if both a single and double quote appear in the
     * original string; furthermore, all instances of double quotes within the
     * original string will be escaped</li>
     * </ul>
     * </p>
     * 
     * @param string the string to potentially quote
     * @param delimiters the delimiters that determines whether quoting should
     *            happen
     * @return the original {@code string} or a properly quoted alternative
     */
    public static String ensureWithinQuotesIfNeeded(String string,
            Character... delimiters) {
        Set<Character> _delimiters = Sets.newHashSet(delimiters);
        _delimiters.remove('\'');
        _delimiters.remove('"');
        boolean foundDouble = false;
        boolean foundSingle = false;
        boolean foundDelimiter = false;
        StringBuilder escaped = new StringBuilder();
        escaped.append('"');
        if(!isWithinQuotes(string)) {
            char[] chars = string.toCharArray();
            for (int i = 0; i < chars.length; ++i) {
                char c = chars[i];
                if(_delimiters.contains(c)) {
                    foundDelimiter = true;
                }
                else if(c == '"') {
                    foundDouble = true;
                    escaped.append('\\');
                }
                else if(c == '\'') {
                    foundSingle = true;
                }
                escaped.append(c);
            }
            escaped.append('"');
            if(foundDelimiter) {
                if(foundDouble && foundSingle) {
                    return escaped.toString();
                }
                else if(foundDouble) {
                    return "'" + string + "'";
                }
                else { // foundSingle or found no quotes
                    return "\"" + string + "\"";
                }
            }
        }
        return string;
    }

    /**
     * Efficiently escape inner occurrences of each of the {@code characters}
     * within the {@code string}, if necessary.
     * <p>
     * Escaped characters are prepended with the backslash ('\') character.
     * </p>
     * <p>
     * An "inner occurrence" for a character is one that is not at the head or
     * tail of the string.
     * </p>
     * 
     * @param string the string to escape
     * @param characters the characters to escape within the {@code string}
     * @return the escaped {@code string}
     */
    public static String escapeInner(String string, char... characters) {
        char c = '\0';
        char pchar = '\0';
        StringBuilder sb = null;
        Set<Character> chars = null;
        if(characters.length == 1) {
            c = characters[0];
        }
        else {
            chars = Sets.newHashSetWithExpectedSize(characters.length);
            for (char ch : characters) {
                chars.add(ch);
            }
        }
        char[] schars = string.toCharArray();
        int offset = 0;
        int i = 0;
        while (i < schars.length) {
            if(i > 0 && i < schars.length - 1) {
                char schar = schars[i];
                if(pchar != '\\' && ((c != '\0' && c == schar)
                        || (chars != null && chars.contains(schar)))) {
                    sb = MoreObjects.firstNonNull(sb, new StringBuilder());
                    sb.append(schars, offset, i - offset);
                    sb.append('\\');
                    char escaped = Characters
                            .getEscapedCharOrNullLiteral(schar);
                    if(escaped != '0') {
                        sb.append(escaped);
                    }
                    else {
                        sb.append(schar);
                    }
                    offset = i + 1;
                }
                pchar = schar;
            }
            ++i;
        }
        if(sb != null) {
            sb.append(schars, offset, i - offset);
            return sb.toString();
        }
        else {
            return string;
        }
    }

    /**
     * <em>Inspired by the SLF4J logging framework!</em>
     * <p>
     * Take a {@code template} string that contains placeholders ({}) and inject
     * each of the {@code args} in their place, respectively.
     * </p>
     * <p>
     * This method behaves similarly to {@link MessageFormat#format(Object)}
     * except the placeholders do not need to be numbered (i.e. "foo {} bar" vs
     * "foo {0} bar"). In most cases, the implementation of this method is also
     * much more efficient.
     * </p>
     * <p>
     * If there are more placeholders than {@code args}, the extra placeholders
     * will be retained formatted string.
     * </p>
     * <p>
     * If there are more {@code args} than placeholders, the extra {@code args}
     * will be placed at the end of the formatted string.
     * </p>
     * <p>
     * If an {@link Exception} is included in the injected {@code args}, only
     * the Exception's message will be included in the formatted. If you want to
     * include the full stack trace of an Exception, place it as the last of the
     * {@code args} and make sure that there is no correspond placeholder.
     * <h2>Example (borrowed from the SLf4J docs)</h2>
     * 
     * <pre>
     * String s = &quot;Hello world&quot;;
     * try {
     *     Integer i = Integer.valueOf(s);
     * }
     * catch (NumberFormatException e) {
     *     System.out.println(AnyStrings.format(&quot;Failed to format {}&quot;, s, e));
     * }
     * </pre>
     * 
     * </p>
     * 
     * @param template a template that may or may not contain placeholders for
     *            variable {@code args}
     * @param args the values to inject in the {@code template} placeholders,
     *            respectively
     * @return the formatted string
     */
    public static String format(String template, Object... args) {
        if(args == null || args.length == 0) {
            return template;
        }
        else if(template.isEmpty()) {
            return formatExtraArgs(new StringBuilder(), args, 0, args.length)
                    .toString();
        }
        else {
            StringBuilder sb = new StringBuilder();
            char[] chars = template.toCharArray();
            int templateLength = chars.length;
            int argsLength = args.length;
            int templateIndex = 0;
            int argsIndex = 0;
            int copyOffset = 0;
            int copyLength = 0;
            while (templateIndex < templateLength) {
                char c = chars[templateIndex];
                int next = templateIndex + 1;
                char nextc = next < templateLength ? chars[next]
                        : Characters.NULL;
                if(c == Characters.ESCAPE && nextc == PLACEHOLDER_BEGIN) {
                    // When escaping the placeholder, simply append any
                    // characters that are currently buffered (see copyLength)
                    // and then skip the escape character before proceeding
                    if(copyLength > 0) {
                        sb.append(chars, copyOffset, copyLength);
                    }
                    sb.append(chars, templateIndex + 1, 1); // append
                                                            // PLACEHOLDER_BEGIN
                                                            // so that we don't
                                                            // execute the block
                                                            // below on the next
                                                            // loop
                    templateIndex += 2;
                    copyOffset = templateIndex;
                    copyLength = 0;
                }
                else if(c == PLACEHOLDER_BEGIN && nextc == PLACEHOLDER_END
                        && argsIndex < argsLength) {
                    sb.append(chars, copyOffset, copyLength);
                    sb.append(String.valueOf(args[argsIndex]));
                    templateIndex = next + 1;
                    ++argsIndex;
                    copyOffset = templateIndex;
                    copyLength = 0;
                }
                else {
                    ++templateIndex;
                    ++copyLength;
                }
            }
            sb.append(chars, copyOffset, copyLength);
            if(argsIndex < argsLength) {
                // If there are remaining args that weren't represented in the
                // template with variable markers, simply append them as a list
                sb.append(": ");
                formatExtraArgs(sb, args, argsIndex, argsLength);
            }
            return sb.toString();
        }
    }

    /**
     * Return a set that contains every possible substring of {@code string}
     * excluding pure whitespace strings.
     * 
     * @param string the string to divide into substrings
     * @return the set of substrings
     */
    public static Set<String> getAllSubStrings(String string) {
        Set<String> result = Sets.newHashSet();
        for (int i = 0; i < string.length(); ++i) {
            for (int j = i + 1; j <= string.length(); ++j) {
                String substring = string.substring(i, j).trim();
                if(!com.google.common.base.Strings.isNullOrEmpty(substring)) {
                    result.add(substring);
                }
            }
        }
        return result;
    }

    /**
     * An optimized version of {@link String#contains(CharSequence)} to see if
     * {@code needle} is a substring of {@code haystack}.
     * 
     * @param needle the substring for which to search
     * @param haystack the string in which to search for the substring
     * @return {@code true} if {@code needle} is a substring
     */
    public static boolean isSubString(String needle, String haystack) {
        if(needle.length() > haystack.length()) {
            return false;
        }
        else if(needle.length() == haystack.length()) {
            return needle.equals(haystack);
        }
        else {
            char[] n = needle.toCharArray();
            char[] h = haystack.toCharArray();
            int npos = 0;
            int hpos = 0;
            int stop = h.length - n.length;
            int hstart = -1;
            while (hpos < h.length && npos < n.length) {
                char hi = h[hpos];
                char ni = n[npos];
                if(hi == ni) {
                    if(hstart == -1) {
                        hstart = hpos;
                    }
                    ++npos;
                    ++hpos;
                }
                else {
                    if(npos > 0) {
                        npos = 0;
                        hpos = hstart + 1;
                        hstart = -1;
                    }
                    else {
                        ++hpos;
                    }
                    if(hpos > stop) {
                        return false;
                    }
                }
            }
            return npos == n.length;
        }
    }

    /**
     * Return {@code true} if {@code string} both starts and ends with single or
     * double quotes.
     * 
     * @param string
     * @param forceTreatAsNonQuoteChar a list of characters may be
     *            technically quotes but should not be considered when checking
     *            if {@code string} is within quotes
     * @return {@code true} if the string is between quotes
     */
    public static boolean isWithinQuotes(String string,
            Character... forceTreatAsNonQuoteChar) {
        string = replaceUnicodeConfusables(string, forceTreatAsNonQuoteChar);
        if(string.length() > 2) {
            char first = string.charAt(0);
            if(first == '"' || first == '\'') {
                char last = string.charAt(string.length() - 1);
                return first == last;
            }
        }
        return false;
    }

    /**
     * Concatenates the {@link Object#toString string} representation of all the
     * {@code args}, separated by the {@code separator} char in an efficient
     * manner.
     * 
     * @param separator the separator to place between each of the {@code args}
     * @param args the args to join
     * @return the resulting String
     */
    public static String join(char separator, Object... args) {
        return join(String.valueOf(separator), args);
    }

    /**
     * Concatenates the {@link Object#toString string} representation of all the
     * {@code args}, separated by the {@code separator} string in an efficient
     * manner.
     * 
     * @param separator the separator to place between each of the {@code args}
     * @param args the args to join
     * @return the resulting String
     */
    public static String join(String separator, Object... args) {
        StringBuilder builder = new StringBuilder();
        if(args.length > 0) {
            for (int i = 0; i < args.length; ++i) {
                builder.append(args[i]);
                builder.append(separator);
            }
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    /**
     * Concatenates the toString values of all the {@code args} in an efficient
     * manner.
     * 
     * @param args
     * @return the resulting String
     */
    public static String joinSimple(Object... args) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; ++i) {
            builder.append(args[i]);
        }
        return builder.toString();
    }

    /**
     * Concatenates the toString values of all the {@code args}, separated by
     * whitespace in an efficient manner.
     * 
     * @param args
     * @return the resulting String
     */
    public static String joinWithSpace(Object... args) {
        return join(' ', args);
    }

    /**
     * Replace all instances of "confusable" unicode characters with a
     * canoncial/normalized character.
     * <p>
     * See <a href=
     * "http://www.unicode.org/Public/security/revision-03/confusablesSummary.txt"
     * >http://www.unicode.org/Public/security/revision-03/confusablesSummary.
     * txt</a> for a list of characters that are considered to be confusable.
     * </p>
     * 
     * @param string the {@link String} in which the replacements should occur
     * @param a list of characters that should not be replaced, even if they are
     *            a confusable
     * @return a {@link String} free of confusable unicode characters
     */
    public static String replaceUnicodeConfusables(String string,
            Character... preserve) {
        Set<Character> preserved = Arrays.stream(preserve)
                .collect(Collectors.toSet());
        char[] chars = string.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            if(DOUBLE_QUOTE_UNICODE_CHARS.contains(c)
                    && !preserved.contains(c)) {
                c = '"';
            }
            else if(SINGLE_QUOTE_UNICODE_CHARS.contains(c)
                    && !preserved.contains(c)) {
                c = '\'';
            }
            chars[i] = c;
        }
        return String.valueOf(chars);
    }

    /**
     * Split a string, using whitespace as a delimiter, as long as the
     * whitespace is not wrapped in double or single quotes.
     * 
     * @param string
     * @return the tokens that result from the split
     * @deprecated in version 0.5.0, use {@link QuoteAwareStringSplitter}
     *             instead.
     */
    @Deprecated
    public static String[] splitButRespectQuotes(String string) {
        return splitStringByDelimiterButRespectQuotes(string, " ");
    }

    /**
     * Split a camel case {@code string} into tokens that represent the distinct
     * words.
     * <p>
     * <h1>Example</h1>
     * <ul>
     * thisIsACamelCaseSTRING -> [this, Is, A, Camel, Case, S, T, R, I, N, G]
     * </ul>
     * <ul>
     * ThisIsACamelCaseSTRING -> [This, Is, A, Camel, Case, S, T, R, I, N, G]
     * </ul>
     * <ul>
     * thisisacamelcasestring -> [thisisacamelcasestring]
     * </ul>
     * </p>
     * 
     * @param string
     * @return a list of tokens after splitting the string on camel case word
     *         boundaries
     */
    public static List<String> splitCamelCase(String string) {
        List<String> words = Lists.newArrayList();
        char[] chars = string.toCharArray();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            if(Character.isUpperCase(c) || c == '$') {
                if(word.length() > 0) {
                    words.add(word.toString());
                }
                word.setLength(0);
            }
            word.append(c);
        }
        words.add(word.toString());
        return words;
    }

    /**
     * Split a string on a delimiter as long as that delimiter is not wrapped in
     * double or single quotes.
     * <p>
     * If {@code delimiter} is a single character string, it is more efficient
     * to use a {@link StringSplitter} as opposed to this method.
     * </p>
     * 
     * @param string the string to split
     * @param delimiter the delimiting string/regex on which the input
     *            {@code string} is split
     * @return the tokens that result from the split
     */
    public static String[] splitStringByDelimiterButRespectQuotes(String string,
            String delimiter) {
        // This is pretty inefficient: convert all single quotes to double
        // quotes (except one off single quotes that are used as apostrophes) so
        // the regex below works
        string = string.replaceAll(" '", " \"");
        string = string.replaceAll("' ", "\" ");
        string = string.replaceAll("'$", "\"");
        return string.split(delimiter + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
    }

    /**
     * This method efficiently tries to parse {@code value} into a
     * {@link Boolean} object if possible. If the string is not a boolean, then
     * the method returns {@code null} as quickly as possible.
     * 
     * @param value
     * @return a Boolean object that represents the string or {@code null} if it
     *         is not possible to parse the string into a boolean
     */
    public static Boolean tryParseBoolean(String value) {
        if(value.equalsIgnoreCase("true")) {
            return true;
        }
        else if(value.equalsIgnoreCase("false")) {
            return false;
        }
        else {
            return null;
        }
    }

    /**
     * This method efficiently tries to parse {@code value} into a
     * {@link Number} object if possible. If the string is not a number, then
     * the method returns {@code null} as quickly as possible.
     * 
     * @param value
     * @return a Number object that represents the string or {@code null} if it
     *         is not possible to parse the string into a number
     */
    @Nullable
    public static Number tryParseNumber(String value) {
        int size = value.length();
        if(value == null || size == 0) {
            return null;
        }
        else if(value.charAt(0) == '0' && size > 1 && value.charAt(1) != '.') {
            // Do not parse a string as a number if it has a leading 0 that is
            // not followed by a decimal (i.e. 007)
            return null;
        }
        boolean decimal = false;
        boolean scientific = false;
        for (int i = 0; i < size; ++i) {
            char c = value.charAt(i);
            if(!Character.isDigit(c)) {
                if(i == 0 && c == '-'
                        || (scientific && (c == '-' || c == '+'))) {
                    continue;
                }
                else if(c == '.') {
                    if(!decimal && size > 1) {
                        decimal = true;
                    }
                    else {
                        // Since we've already seen a decimal, the appearance of
                        // another one suggests this is an IP address instead of
                        // a number
                        return null;
                    }
                }
                else if(i == size - 1 && c == 'D' && size > 1) {
                    // Respect the convention to coerce numeric strings to
                    // Double objects by appending a single 'D' character.
                    return Double.valueOf(value.substring(0, i));
                }
                else if((c == 'E' || c == 'e') && i < size - 1) {
                    // CON-627: Account for valid representations of scientific
                    // notation
                    if(!scientific) {
                        scientific = true;
                    }
                    else {
                        // Since we've already seen a scientific notation
                        // indicator, another one suggests that this is not
                        // really a number
                        return null;
                    }
                }
                else {
                    return null;
                }
            }
        }
        try {
            if(decimal || scientific) {
                // Try to return a float (for space compactness) if it is
                // possible to fit the entire decimal without any loss of
                // precision. In order to do this, we have to compare the string
                // output of both the parsed double and the parsed float. This
                // is kind of inefficient, so substitute for a better way if it
                // exists.
                double d = Doubles.tryParse(value);
                float f = Floats.tryParse(value);
                if(String.valueOf(d).equals(String.valueOf(f))) {
                    return f;
                }
                else {
                    return d;
                }
            }
            else if(value.equals("-")) { // CON-597
                return null;
            }
            else {
                return MoreObjects.firstNonNull(Ints.tryParse(value),
                        Longs.tryParse(value));
            }
        }
        catch (NullPointerException e) {
            throw new NumberFormatException(format(
                    "{} appears to be a number but cannot be parsed as such",
                    value));
        }
    }

    /**
     * A stricter version of {@link #tryParseNumber(String)} that does not parse
     * strings that masquerade as numbers (i.e. 3.124D). Instead this method
     * will only parse the string into a Number if it contains characters that
     * are either a decimal digit, a decimal separator or a negative sign.
     * 
     * @param value
     * @return a Number object that represents the string or {@code null} if it
     *         is not possible to parse the string into a number
     */
    @Nullable
    public static Number tryParseNumberStrict(String value) {
        if(value == null || value.length() == 0) {
            return null;
        }
        char last = value.charAt(value.length() - 1);
        if(Character.isDigit(last)) {
            return tryParseNumber(value);
        }
        else {
            return null;
        }
    }

    /**
     * Similar to the {@link String#valueOf(char)} method, but this one will
     * return a cached copy of the string for frequently used characters.
     * 
     * @param c the character to convert
     * @return a string of length 1 containing the input char
     */
    public static String valueOfCached(char c) {
        if(c == '(') {
            return "(";
        }
        else if(c == ')') {
            return ")";
        }
        else {
            return String.valueOf(c);
        }
    }

    /**
     * Add the {@code wrapper} character to the beginning and end of the
     * {@code string} and escape any instance of the {@code wrapper} within the
     * {@code string} with the specified {@code escape} character.
     * 
     * @param string
     * @param wrapper
     * @param escape
     * @return the wrapped (and escaped, if necessary) string
     */
    public static String wrap(String string, char wrapper, char escape) {
        StringBuilder sb = new StringBuilder();
        sb.append(wrapper);
        char[] chars = string.toCharArray();
        for (char c : chars) {
            if(c == wrapper) {
                sb.append(escape);
            }
            sb.append(c);
        }
        sb.append(wrapper);
        return sb.toString();
    }

    /**
     * Execute the rules for formatting {@code args} with no corresponding
     * placeholders in the original template.
     * 
     * @param sb the {@link StringBuilder} that contains the formatted string;
     *            modified in-place within this method and returned for
     *            convenience
     * @param args all of the placeholder values
     * @param argsIndex the index of the first of the {@code args} that is
     *            "extra"
     * @param argsLength the length of {@code args}, required so it is not
     *            computed twice
     * @return {@code sb} for convenience
     */
    private static StringBuilder formatExtraArgs(StringBuilder sb,
            Object[] args, int argsIndex, int argsLength) {
        for (int i = argsIndex; i < argsLength; ++i) {
            Object arg = args[i];
            int nextIndex = i + 1;
            if(nextIndex == argsLength && arg instanceof Exception) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ((Exception) arg).printStackTrace(new PrintStream(baos));
                sb.append(baos.toString());
            }
            else {
                sb.append(String.valueOf(arg));
            }
            if(nextIndex != argsLength) {
                sb.append(' ');
            }
        }
        return sb;
    }

}
