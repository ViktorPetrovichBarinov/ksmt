package io.ksmt.utils

import io.ksmt.KContext
import io.ksmt.expr.KStringLiteralExpr
import io.ksmt.expr.KIntNumExpr
import io.ksmt.expr.KInterpretedValue
import io.ksmt.sort.KBoolSort

object StringUtils {

    const val STRING_FROM_CODE_UPPER_BOUND: Int = 196607

    /** Singleton string for an SMT-LIB code point. Codes above U+FFFF need a surrogate pair. */
    @JvmStatic
    fun codePointToString(codePoint: Int): String =
        String(Character.toChars(codePoint))

    /** Unicode code points of [value]. A supplementary character is one point, not two UTF-16 units. */
    @JvmStatic
    fun codePoints(value: String): IntArray {
        val points = IntArray(value.codePointCount(0, value.length))
        var index = 0
        var offset = 0
        while (offset < value.length) {
            val codePoint = value.codePointAt(offset)
            points[index++] = codePoint
            offset += Character.charCount(codePoint)
        }
        return points
    }

    @JvmStatic
    fun getStringLen(arg: KStringLiteralExpr): KIntNumExpr = with(arg.ctx) {
        mkIntNum(arg.value.codePointCount(0, arg.value.length))
    }

    /** Character at a code-point index. An index outside the string yields the empty string. */
    @JvmStatic
    fun charAtCodePoint(value: String, index: Int): String {
        if (index < 0 || index >= value.codePointCount(0, value.length)) return ""
        val offset = value.offsetByCodePoints(0, index)
        return codePointToString(value.codePointAt(offset))
    }

    /** Substring by code-point offset and length. Out of range yields the empty string. */
    @JvmStatic
    fun substringByCodePoints(value: String, start: Int, length: Int): String {
        if (start < 0 || length < 0) return ""
        val count = value.codePointCount(0, value.length)
        if (start >= count) return ""
        val end = minOf(start.toLong() + length.toLong(), count.toLong()).toInt()
        val startOffset = value.offsetByCodePoints(0, start)
        val endOffset = value.offsetByCodePoints(startOffset, end - start)
        return value.substring(startOffset, endOffset)
    }

    /** First code-point index of [search] at or after [start], or -1. */
    @JvmStatic
    fun indexOfCodePoints(value: String, search: String, start: Int): Int {
        val haystack = codePoints(value)
        if (start < 0 || start > haystack.size) return -1
        if (search.isEmpty()) return start
        val needle = codePoints(search)
        val last = haystack.size - needle.size
        for (index in start..last) {
            if (needle.indices.all { needle[it] == haystack[index + it] }) return index
        }
        return -1
    }

    @JvmStatic
    fun concatStrings(
        lhs: KStringLiteralExpr,
        rhs: KStringLiteralExpr
    ): KStringLiteralExpr = with(lhs.ctx) {
        mkStringLiteral(lhs.value + rhs.value)
    }

    @JvmStatic
    fun isStringSuffix(
        arg0: KStringLiteralExpr,
        arg1: KStringLiteralExpr
    ): KInterpretedValue<KBoolSort> = with(arg0.ctx) {
        mkBool(hasSuffixCodePoints(arg1.value, arg0.value)).uncheckedCast()
    }

    @JvmStatic
    fun isStringPrefix(
        arg0: KStringLiteralExpr,
        arg1: KStringLiteralExpr
    ): KInterpretedValue<KBoolSort> = with(arg0.ctx) {
        mkBool(hasPrefixCodePoints(arg1.value, arg0.value)).uncheckedCast()
    }

    @JvmStatic
    fun stringLt(
        arg0: KStringLiteralExpr,
        arg1: KStringLiteralExpr
    ): KInterpretedValue<KBoolSort> = with(arg0.ctx) {
        mkBool(compareCodePoints(arg0.value, arg1.value) < 0).uncheckedCast()
    }

    @JvmStatic
    fun stringLe(
        arg0: KStringLiteralExpr,
        arg1: KStringLiteralExpr
    ): KInterpretedValue<KBoolSort> = with(arg0.ctx) {
        mkBool(compareCodePoints(arg0.value, arg1.value) <= 0).uncheckedCast()
    }

    @JvmStatic
    fun stringGt(
        arg0: KStringLiteralExpr,
        arg1: KStringLiteralExpr
    ): KInterpretedValue<KBoolSort> = with(arg0.ctx) {
        mkBool(compareCodePoints(arg0.value, arg1.value) > 0).uncheckedCast()
    }

    @JvmStatic
    fun stringGe(
        arg0: KStringLiteralExpr,
        arg1: KStringLiteralExpr
    ): KInterpretedValue<KBoolSort> = with(arg0.ctx) {
        mkBool(compareCodePoints(arg0.value, arg1.value) >= 0).uncheckedCast()
    }

    @JvmStatic
    fun stringContains(
        arg0: KStringLiteralExpr,
        arg1: KStringLiteralExpr
    ): KInterpretedValue<KBoolSort> = with(arg0.ctx) {
        mkBool(containsCodePoints(arg0.value, arg1.value)).uncheckedCast()
    }

    @JvmStatic
    fun stringToLowerCase(
        arg: KStringLiteralExpr
    ): KStringLiteralExpr = with(arg.ctx) {
        mkStringLiteral(arg.value.lowercase())
    }

    @JvmStatic
    fun stringToUpperCase(
        arg: KStringLiteralExpr
    ): KStringLiteralExpr = with(arg.ctx) {
        mkStringLiteral(arg.value.uppercase())
    }

    @JvmStatic
    fun stringReverse(
        arg: KStringLiteralExpr
    ): KStringLiteralExpr = with(arg.ctx) {
        val points = codePoints(arg.value)
        points.reverse()
        mkStringLiteral(String(points, 0, points.size))
    }

    @JvmStatic
    fun stringIsDigit(
        arg: KStringLiteralExpr
    ): KInterpretedValue<KBoolSort> = with(arg.ctx) {
        mkBool(arg.value.length == 1 && arg.value[0] in '0'..'9').uncheckedCast()
    }
    
    @JvmStatic
    fun stringToCode(
        arg: KStringLiteralExpr
    ): KIntNumExpr = with(arg.ctx) {
        val points = codePoints(arg.value)
        mkIntNum(if (points.size == 1) points[0] else -1)
    }

    @JvmStatic
    fun stringToInt(
        arg: KStringLiteralExpr
    ): KIntNumExpr = with(arg.ctx) {
        val digits = arg.value
        return if (digits.isNotEmpty() && digits.all { it in '0'..'9' }) {
            mkIntNum(digits.toBigInteger())
        } else {
            mkIntNum(-1)
        }
    }

    @JvmStatic
    fun strintReplace(
        arg0: KStringLiteralExpr,
        arg1: KStringLiteralExpr,
        arg2: KStringLiteralExpr
    ): KStringLiteralExpr = with(arg0.ctx) {
        val str = arg0.value
        val search = arg1.value
        val replace = arg2.value
        return mkStringLiteral(if (search.isEmpty()) (replace + str) else str.replaceFirst(search, replace))
    }

    @JvmStatic
    fun strintReplaceAll(
        arg0: KStringLiteralExpr,
        arg1: KStringLiteralExpr,
        arg2: KStringLiteralExpr
    ): KStringLiteralExpr = with(arg0.ctx) {
        val str = arg0.value
        val search = arg1.value
        val replace = arg2.value
        return mkStringLiteral(replaceAllCodePoints(str, search, replace))
    }

    /** Lexicographic order of Unicode code points, the shorter string first after a shared prefix. */
    private fun compareCodePoints(left: String, right: String): Int {
        val a = codePoints(left)
        val b = codePoints(right)
        val shared = minOf(a.size, b.size)
        for (index in 0 until shared) {
            val difference = a[index].compareTo(b[index])
            if (difference != 0) return difference
        }
        return a.size.compareTo(b.size)
    }

    private fun containsCodePoints(value: String, search: String): Boolean =
        indexOfCodePoints(value, search, 0) >= 0

    private fun hasPrefixCodePoints(value: String, prefix: String): Boolean {
        val text = codePoints(value)
        val needle = codePoints(prefix)
        if (needle.size > text.size) return false
        return needle.indices.all { needle[it] == text[it] }
    }

    private fun hasSuffixCodePoints(value: String, suffix: String): Boolean {
        val text = codePoints(value)
        val needle = codePoints(suffix)
        if (needle.size > text.size) return false
        val offset = text.size - needle.size
        return needle.indices.all { needle[it] == text[offset + it] }
    }

    /** Non-overlapping replacement by code points. An empty search leaves the string unchanged. */
    private fun replaceAllCodePoints(value: String, search: String, replacement: String): String {
        if (search.isEmpty()) return value
        val text = codePoints(value)
        val needle = codePoints(search)
        if (needle.size > text.size) return value
        val inserted = codePoints(replacement)
        val out = ArrayList<Int>(text.size)
        var index = 0
        val last = text.size - needle.size
        while (index <= last) {
            val matches = needle.indices.all { needle[it] == text[index + it] }
            if (matches) {
                inserted.forEach { out.add(it) }
                index += needle.size
            } else {
                out.add(text[index])
                index++
            }
        }
        while (index < text.size) {
            out.add(text[index])
            index++
        }
        val points = out.toIntArray()
        return String(points, 0, points.size)
    }

    @JvmStatic
    fun KContext.sequentialEngAlphabetChars(): List<KStringLiteralExpr> {
        val alphabet = ('A'..'Z') + ('a'..'z')

        return alphabet.map { char ->
            this.mkStringLiteral(char.toString())
        }
    }
}
