package io.ksmt

import io.ksmt.expr.KExpr
import io.ksmt.sort.KSort
import io.ksmt.sort.KStringSort
import io.ksmt.utils.StringUtils.STRING_FROM_CODE_UPPER_BOUND
import io.ksmt.utils.StringUtils.sequentialEngAlphabetChars
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.math.BigInteger

@Execution(ExecutionMode.CONCURRENT)
class StringSimplifyTest: ExpressionSimplifyTest() {

    @Test
    fun testStringLen() = testOperation(KContext::mkStringLen, KContext::mkStringLenNoSimplify)

    @Test
    fun testSuffixOf() = testOperation(KContext::mkStringSuffixOf, KContext::mkStringSuffixOfNoSimplify) {
        listOf(
            "prefixsuffix".expr,
            "prefix".expr,
            "suffix".expr
        )
    }

    @Test
    fun testPrefixOf() = testOperation(KContext::mkStringPrefixOf, KContext::mkStringPrefixOfNoSimplify) {
        listOf(
            "prefixsuffix".expr,
            "prefix".expr,
            "suffix".expr
        )
    }

    @Test
    fun testStringLt() = testOperation(KContext::mkStringLt, KContext::mkStringLtNoSimplify) {
        sequentialEngAlphabetChars()
    }

    @Test
    fun testStringLe() = testOperation(KContext::mkStringLe, KContext::mkStringLeNoSimplify) {
        sequentialEngAlphabetChars()
    }

    @Test
    fun testStringGt() = testOperation(KContext::mkStringGt, KContext::mkStringGtNoSimplify) {
        sequentialEngAlphabetChars()
    }

    @Test
    fun testStringGe() = testOperation(KContext::mkStringGe, KContext::mkStringGeNoSimplify) {
        sequentialEngAlphabetChars()
    }

    @Test
    fun testStringContais() = testOperation(KContext::mkStringContains, KContext::mkStringContainsNoSimplify) {
        listOf(
            "containsSomeString".expr,
            "Some".expr,
            "None".expr
        )
    }

    @Test
    fun testStringReplace() = testOperation(KContext::mkStringReplace, KContext::mkStringReplaceNoSimplify) {
        listOf(
            "containsSomeSomeString".expr,
            "Some".expr,
            "None".expr
        )
    }

    @Test
    fun testStringReplaceTreatsDotAsText() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = "ab".expr,
            simplifiedExpr = mkStringReplace("ab".expr, ".".expr, "X".expr),
            printArgs = { "str.replace of a dot" }
        )
    }

    @Test
    fun testStringReplaceTreatsDollarAsText() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = "aXb".expr,
            simplifiedExpr = mkStringReplace("a\$b".expr, "\$".expr, "X".expr),
            printArgs = { "str.replace of a dollar sign" }
        )
    }

    @Test
    fun testStringReplaceSupplementaryCharacter() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = "aXb".expr,
            simplifiedExpr = mkStringReplace(
                mkStringConcat(mkStringConcat("a".expr, emoji), "b".expr),
                emoji,
                "X".expr
            ),
            printArgs = { "str.replace of U+1F44D" }
        )
    }

    @Test
    fun testStringReplaceDoesNotMatchSurrogateHalf() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = emoji,
            simplifiedExpr = mkStringReplace(emoji, highSurrogateOfEmoji(), "X".expr),
            printArgs = { "str.replace of a surrogate half inside U+1F44D" }
        )
    }

    @Test
    fun testStringReplaceEmptySearch() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = "caba".expr,
            simplifiedExpr = mkStringReplace("aba".expr, "".expr, "c".expr),
            printArgs = { "str.replace of the empty string" }
        )
    }

    @Test
    fun testStringConcat() = testOperation(KContext::mkStringConcat, KContext::mkStringConcatNoSimplify) {
        listOf("".expr, "ab".expr, "\uD83D\uDC4D".expr)
    }

    @Test
    fun testStringConcatFoldsSupplementaryCharacter() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringLiteral("a\uD83D\uDC4Db"),
            simplifiedExpr = mkStringConcat(mkStringConcat("a".expr, emoji), "b".expr),
            printArgs = { "concat keeps U+1F44D intact" }
        )
    }

    @Test
    fun testStringReplaceAll() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = "cbc".expr,
            simplifiedExpr = mkStringReplaceAll("aba".expr, "a".expr, "c".expr),
            printArgs = { "str.replace_all of a" }
        )
    }

    @Test
    fun testStringReplaceAllEmptySearch() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = "aba".expr,
            simplifiedExpr = mkStringReplaceAll("aba".expr, "".expr, "c".expr),
            printArgs = { "str.replace_all of the empty string" }
        )
    }

    @Test
    fun testStringReplaceAllSupplementaryCharacter() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = "aXb".expr,
            simplifiedExpr = mkStringReplaceAll(
                mkStringConcat(mkStringConcat("a".expr, emoji), "b".expr),
                emoji,
                "X".expr
            ),
            printArgs = { "str.replace_all of U+1F44D" }
        )
    }

    @Test
    fun testStringReplaceAllDoesNotMatchSurrogateHalf() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        val highSurrogate = mkStringFromCode(mkIntNum(0xD83D))
        checker.check(
            unsimplifiedExpr = emoji,
            simplifiedExpr = mkStringReplaceAll(emoji, highSurrogate, "X".expr),
            printArgs = { "str.replace_all of a surrogate half inside U+1F44D" }
        )
    }

    @Test
    fun testStringToLower() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = "abc".expr,
            simplifiedExpr = mkStringToLower("AbC".expr),
            printArgs = { "str.to_lower of AbC" }
        )
    }

    @Test
    fun testStringToUpper() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = "ABC".expr,
            simplifiedExpr = mkStringToUpper("AbC".expr),
            printArgs = { "str.to_upper of AbC" }
        )
    }

    @Test
    fun testStringToLowerKeepsSupplementaryCharacter() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringConcat("a".expr, emoji),
            simplifiedExpr = mkStringToLower(mkStringConcat("A".expr, emoji)),
            printArgs = { "str.to_lower keeps U+1F44D" }
        )
    }

    @Test
    fun testStringToUpperKeepsSupplementaryCharacter() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringConcat("A".expr, emoji),
            simplifiedExpr = mkStringToUpper(mkStringConcat("a".expr, emoji)),
            printArgs = { "str.to_upper keeps U+1F44D" }
        )
    }

    @Test
    fun testStringToLowerKeepsTurkishI() = runTest { _, checker ->
        val letter = mkStringLiteral("\u0130")
        checker.check(
            unsimplifiedExpr = letter,
            simplifiedExpr = mkStringToLower(letter),
            printArgs = { "str.to_lower keeps U+0130" }
        )
    }

    @Test
    fun testStringToUpperKeepsTurkishI() = runTest { _, checker ->
        val letter = mkStringLiteral("\u0130")
        checker.check(
            unsimplifiedExpr = letter,
            simplifiedExpr = mkStringToUpper(letter),
            printArgs = { "str.to_upper keeps U+0130" }
        )
    }

    @Test
    fun testStringToLowerKeepsEszett() = runTest { _, checker ->
        val letter = mkStringLiteral("\u00DF")
        checker.check(
            unsimplifiedExpr = letter,
            simplifiedExpr = mkStringToLower(letter),
            printArgs = { "str.to_lower keeps U+00DF" }
        )
    }

    @Test
    fun testStringToUpperKeepsEszett() = runTest { _, checker ->
        val letter = mkStringLiteral("\u00DF")
        checker.check(
            unsimplifiedExpr = letter,
            simplifiedExpr = mkStringToUpper(letter),
            printArgs = { "str.to_upper keeps U+00DF" }
        )
    }

    @Test
    fun testStringToCode() = testOperation(KContext::mkStringToCode, KContext::mkStringToCodeNoSimplify) {
        sequentialEngAlphabetChars()
    }

    @Test
    fun testStringToInt() = testOperation(KContext::mkStringToInt, KContext::mkStringToIntNoSimplify) {
        listOf("1".expr, "123".expr, "01".expr)
    }

    @Test
    fun testStringToIntTwoToThe63() =
        checkToIntMatchesSolver(TWO_TO_THE_63.toString())

    @Test
    fun testStringToIntTwoToThe64() =
        checkToIntMatchesSolver(TWO_TO_THE_64.toString())

    @Test
    fun testStringFromIntTwoToThe63() =
        checkFromIntMatchesSolver(TWO_TO_THE_63)

    @Test
    fun testStringFromIntTwoToThe64() =
        checkFromIntMatchesSolver(TWO_TO_THE_64)

    @Test
    fun testStringToIntLongMaxValue() =
        checkToIntMatchesSolver(Long.MAX_VALUE.toString())

    @Test
    fun testStringFromIntLongMaxValue() =
        checkFromIntMatchesSolver(BigInteger.valueOf(Long.MAX_VALUE))

    @Test
    fun testStringFromIntNegative() =
        checkFromIntMatchesSolver(BigInteger.valueOf(-1))

    @Test
    fun testStringFromIntNegativeTwoToThe64() =
        checkFromIntMatchesSolver(TWO_TO_THE_64.negate())

    @Test
    fun testStringToIntNegativeSign() =
        checkToIntMatchesSolver("-1")

    @Test
    fun testStringToIntNonDigit() =
        checkToIntMatchesSolver("12a")

    @Test
    fun testStringToIntNonAsciiDigit() =
        checkToIntMatchesSolver("\u0661")

    private fun checkToIntMatchesSolver(digits: String) = runTest { _, checker ->
        val literal = mkStringLiteral(digits)
        checker.check(
            unsimplifiedExpr = mkStringToIntNoSimplify(literal),
            simplifiedExpr = mkStringToInt(literal),
            printArgs = { "str.to_int(\"$digits\") folded to ${mkStringToInt(literal)}" }
        )
    }

    private fun checkFromIntMatchesSolver(value: BigInteger) = runTest { _, checker ->
        val number = mkIntNum(value)
        checker.check(
            unsimplifiedExpr = mkStringFromIntNoSimplify(number),
            simplifiedExpr = mkStringFromInt(number),
            printArgs = { "str.from_int($value) folded to ${mkStringFromInt(number)}" }
        )
    }

    /**
     * https://smt-lib.org/theories-UnicodeStrings.shtml
     * "The singleton string constants represent all the Unicode code points in 
     * Planes 0 to 2 of Unicode, ranging from 0x00000 to 0x2FFFF (0 to 196607)."
     */
    @Test
    fun testStringFromCodeFirstSupplementaryCharacter() =
        checkFromCodeMatchesSolver(0x10000)

    @Test
    fun testStringFromCodeEmoji() =
        checkFromCodeMatchesSolver(0x1F44D)

    @Test
    fun testStringFromCodeLastStandardCodePoint() =
        checkFromCodeMatchesSolver(STRING_FROM_CODE_UPPER_BOUND)

    @Test
    fun testStringFromCodeZero() =
        checkFromCodeMatchesSolver(0)

    @Test
    fun testStringFromCodeLastBmpCharacter() =
        checkFromCodeMatchesSolver(0xFFFF)

    @Test
    fun testStringFromCodeSurrogateCodePoint() =
        checkFromCodeMatchesSolver(0xD800)

    @Test
    fun testStringFromCodeBelowRange() =
        checkFromCodeMatchesSolver(-1)

    @Test
    fun testStringFromCodeAboveRange() =
        checkFromCodeMatchesSolver(STRING_FROM_CODE_UPPER_BOUND + 1)

    @Test
    fun testStringLenOfSupplementaryCharacter() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringLenNoSimplify(emoji),
            simplifiedExpr = mkStringLen(emoji),
            printArgs = { "str.len of U+1F44D" }
        )
    }

    @Test
    fun testStringToCodeOfSupplementaryCharacter() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringToCodeNoSimplify(emoji),
            simplifiedExpr = mkStringToCode(emoji),
            printArgs = { "str.to_code of U+1F44D" }
        )
    }

    @Test
    fun testStringAtSupplementaryCharacter() = runTest { _, checker ->
        val text = textAroundSupplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringSingletonSubNoSimplify(text, mkIntNum(1)),
            simplifiedExpr = mkStringSingletonSub(text, mkIntNum(1)),
            printArgs = { "str.at of U+1F44D at code-point index 1" }
        )
    }

    @Test
    fun testStringSubDoesNotSplitSupplementaryCharacter() = runTest { _, checker ->
        val text = textAroundSupplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringSubNoSimplify(text, mkIntNum(1), mkIntNum(1)),
            simplifiedExpr = mkStringSub(text, mkIntNum(1), mkIntNum(1)),
            printArgs = { "str.substr of U+1F44D" }
        )
    }

    @Test
    fun testStringIndexOfSupplementaryCharacter() = runTest { _, checker ->
        val text = textAroundSupplementaryCharacter()
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringIndexOfNoSimplify(text, emoji, mkIntNum(0)),
            simplifiedExpr = mkStringIndexOf(text, emoji, mkIntNum(0)),
            printArgs = { "str.indexof of U+1F44D" }
        )
    }

    @Test
    fun testStringIndexOfHugeNegativeStart() = runTest { _, _ ->
        val folded = mkStringIndexOf("abc".expr, "a".expr, mkIntNum(-4294967296L))
        kotlin.test.assertSame(mkIntNum(-1), folded, "str.indexof with a huge negative start")
    }

    @Test
    fun testStringIndexOfStartBeyondIntMax() = runTest { _, _ ->
        val folded = mkStringIndexOf("abc".expr, "a".expr, mkIntNum(1L shl 31))
        kotlin.test.assertSame(mkIntNum(-1), folded, "str.indexof with a start past Int.MAX_VALUE")
    }

    @Test
    fun testStringIndexOfEmptySearchAtLength() = runTest { _, _ ->
        val folded = mkStringIndexOf("ab".expr, "".expr, mkIntNum(2))
        kotlin.test.assertSame(mkIntNum(2), folded, "str.indexof of the empty string at the length")
    }

    @Test
    fun testStringIndexOfDoesNotMatchSurrogateHalf() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        val highSurrogate = mkStringFromCode(mkIntNum(0xD83D))
        checker.check(
            unsimplifiedExpr = mkStringIndexOfNoSimplify(emoji, highSurrogate, mkIntNum(0)),
            simplifiedExpr = mkStringIndexOf(emoji, highSurrogate, mkIntNum(0)),
            printArgs = { "str.indexof of a surrogate half inside U+1F44D" }
        )
    }

    @Test
    fun testStringLenOfConcatenatedSupplementaryCharacters() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        val text = mkStringConcat(emoji, emoji)
        checker.check(
            unsimplifiedExpr = mkStringLenNoSimplify(text),
            simplifiedExpr = mkStringLen(text),
            printArgs = { "str.len of two supplementary characters" }
        )
    }

    @Test
    fun testStringContainsSupplementaryCharacter() = runTest { _, checker ->
        val text = textAroundSupplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringContainsNoSimplify(text, supplementaryCharacter()),
            simplifiedExpr = mkStringContains(text, supplementaryCharacter()),
            printArgs = { "str.contains of U+1F44D" }
        )
    }

    @Test
    fun testStringLtSupplementaryCharacter() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = mkStringLtNoSimplify(mkStringLiteral("a"), supplementaryCharacter()),
            simplifiedExpr = mkStringLt(mkStringLiteral("a"), supplementaryCharacter()),
            printArgs = { "str.< of a BMP character and U+1F44D" }
        )
    }

    @Test
    fun testStringLtLastBmpBeforeFirstSupplementary() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = mkStringLtNoSimplify(lastBmpCharacter(), firstSupplementaryCharacter()),
            simplifiedExpr = mkStringLt(lastBmpCharacter(), firstSupplementaryCharacter()),
            printArgs = { "str.< of U+FFFF and U+10000" }
        )
    }

    @Test
    fun testStringLeLastBmpBeforeFirstSupplementary() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = mkStringLeNoSimplify(lastBmpCharacter(), firstSupplementaryCharacter()),
            simplifiedExpr = mkStringLe(lastBmpCharacter(), firstSupplementaryCharacter()),
            printArgs = { "str.<= of U+FFFF and U+10000" }
        )
    }

    @Test
    fun testStringGtFirstSupplementaryAfterLastBmp() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = mkStringGtNoSimplify(firstSupplementaryCharacter(), lastBmpCharacter()),
            simplifiedExpr = mkStringGt(firstSupplementaryCharacter(), lastBmpCharacter()),
            printArgs = { "str.> of U+10000 and U+FFFF" }
        )
    }

    @Test
    fun testStringGeFirstSupplementaryAfterLastBmp() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = mkStringGeNoSimplify(firstSupplementaryCharacter(), lastBmpCharacter()),
            simplifiedExpr = mkStringGe(firstSupplementaryCharacter(), lastBmpCharacter()),
            printArgs = { "str.>= of U+10000 and U+FFFF" }
        )
    }

    @Test
    fun testStringContainsDoesNotMatchSurrogateHalf() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = mkStringContainsNoSimplify(supplementaryCharacter(), highSurrogateOfEmoji()),
            simplifiedExpr = mkStringContains(supplementaryCharacter(), highSurrogateOfEmoji()),
            printArgs = { "str.contains of a surrogate half inside U+1F44D" }
        )
    }

    @Test
    fun testStringPrefixOfSupplementaryCharacter() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringPrefixOfNoSimplify(emoji, mkStringConcat(emoji, "a".expr)),
            simplifiedExpr = mkStringPrefixOf(emoji, mkStringConcat(emoji, "a".expr)),
            printArgs = { "str.prefixof of U+1F44D" }
        )
    }

    @Test
    fun testStringPrefixOfDoesNotMatchSurrogateHalf() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = mkStringPrefixOfNoSimplify(highSurrogateOfEmoji(), supplementaryCharacter()),
            simplifiedExpr = mkStringPrefixOf(highSurrogateOfEmoji(), supplementaryCharacter()),
            printArgs = { "str.prefixof of a surrogate half inside U+1F44D" }
        )
    }

    @Test
    fun testStringSuffixOfSupplementaryCharacter() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        checker.check(
            unsimplifiedExpr = mkStringSuffixOfNoSimplify(emoji, mkStringConcat("a".expr, emoji)),
            simplifiedExpr = mkStringSuffixOf(emoji, mkStringConcat("a".expr, emoji)),
            printArgs = { "str.suffixof of U+1F44D" }
        )
    }

    @Test
    fun testStringSuffixOfDoesNotMatchSurrogateHalf() = runTest { _, checker ->
        checker.check(
            unsimplifiedExpr = mkStringSuffixOfNoSimplify(lowSurrogateOfEmoji(), supplementaryCharacter()),
            simplifiedExpr = mkStringSuffixOf(lowSurrogateOfEmoji(), supplementaryCharacter()),
            printArgs = { "str.suffixof of a surrogate half inside U+1F44D" }
        )
    }

    @Test
    fun testStringReverseKeepsSupplementaryCharacter() = runTest { _, checker ->
        val emoji = supplementaryCharacter()
        val text = mkStringConcat(emoji, mkStringLiteral("a"))
        checker.check(
            unsimplifiedExpr = mkStringConcat(mkStringLiteral("a"), emoji),
            simplifiedExpr = mkStringReverse(text),
            printArgs = { "str.rev keeps U+1F44D intact" }
        )
    }

    @Test
    fun testStringIsDigitOfSupplementaryCharacter() = runTest { _, _ ->
        kotlin.test.assertSame(mkBool(false), mkStringIsDigit(supplementaryCharacter()))
    }

    @Test
    fun testStringIsDigitZero() = checkIsDigit("0", expected = true)

    @Test
    fun testStringIsDigitNine() = checkIsDigit("9", expected = true)

    @Test
    fun testStringIsDigitLetter() = checkIsDigit("a", expected = false)

    @Test
    fun testStringIsDigitTwoCharacters() = checkIsDigit("10", expected = false)

    @Test
    fun testStringIsDigitArabicIndicDigit() = checkIsDigit("\u0661", expected = false)

    @Test
    fun testStringIsDigitFullwidthDigit() = checkIsDigit("\uFF15", expected = false)

    @Test
    fun testStringIsDigitSuperscriptDigit() = checkIsDigit("\u00B2", expected = false)

    private fun checkIsDigit(text: String, expected: Boolean) = runTest { _, _ ->
        val folded = mkStringIsDigit(mkStringLiteral(text))
        val codes = text.codePoints().toArray().joinToString { "U+%04X".format(it) }
        kotlin.test.assertSame(mkBool(expected), folded, "str.is_digit($codes) folded to $folded")
    }

    private fun KContext.supplementaryCharacter() =
        mkStringFromCode(mkIntNum(0x1F44D))

    private fun KContext.lastBmpCharacter() =
        mkStringFromCode(mkIntNum(0xFFFF))

    private fun KContext.firstSupplementaryCharacter() =
        mkStringFromCode(mkIntNum(0x10000))

    private fun KContext.highSurrogateOfEmoji() =
        mkStringFromCode(mkIntNum(0xD83D))

    private fun KContext.lowSurrogateOfEmoji() =
        mkStringFromCode(mkIntNum(0xDC4D))

    private fun KContext.textAroundSupplementaryCharacter() =
        mkStringConcat(mkStringConcat(mkStringLiteral("a"), supplementaryCharacter()), mkStringLiteral("b"))

    private fun checkFromCodeMatchesSolver(codePoint: Int) = runTest { _, checker ->
        val code = mkIntNum(codePoint)
        checker.check(
            unsimplifiedExpr = mkStringFromCodeNoSimplify(code),
            simplifiedExpr = mkStringFromCode(code),
            printArgs = {
                "str.from_code($codePoint) folded to ${mkStringFromCode(code)}, " +
                    "expected the character U+${codePoint.toString(16).uppercase()}"
            }
        )
    }

    @JvmName("testUnary")
    private fun <T : KSort> testOperation(
        operation: KContext.(KExpr<KStringSort>) -> KExpr<T>,
        operationNoSimplify: KContext.(KExpr<KStringSort>) -> KExpr<T>,
        mkSpecialValues: KContext.(KStringSort) -> List<KExpr<KStringSort>> = { emptyList() },
    ) = runTest { sort: KStringSort, checker ->
        val x = mkConst("x", sort)
        val specialValues = mkSpecialValues(sort)

        (listOf(x) + specialValues).forEach { value ->
            val unsimplifiedExpr = operationNoSimplify(value)
            val simplifiedExpr = operation(value)
            checker.check(unsimplifiedExpr = unsimplifiedExpr, simplifiedExpr = simplifiedExpr) { "$value" }
        }
    }

    @JvmName("testBinaryNotNested")
    private fun <T : KSort> testOperation(
        operation: KContext.(KExpr<KStringSort>, KExpr<KStringSort>) -> KExpr<T>,
        operationNoSimplify: KContext.(KExpr<KStringSort>, KExpr<KStringSort>) -> KExpr<T>,
        mkSpecialValues: KContext.(KStringSort) -> List<KExpr<KStringSort>> = { emptyList() }
    ) = runTest { sort: KStringSort, checker ->
        val a = mkConst("a", sort)
        val b = mkConst("b", sort)
        val specialValues = mkSpecialValues(sort)

        (listOf(a) + specialValues).forEach { lhs ->
            (listOf(b) + specialValues).forEach { rhs ->
                val unsimplifiedExpr = operationNoSimplify(lhs, rhs)
                val simplifiedExpr = operation(lhs, rhs)
                checker.check(unsimplifiedExpr = unsimplifiedExpr, simplifiedExpr = simplifiedExpr) {
                    "$lhs, $rhs"
                }
            }
        }
    }

    @JvmName("testTernaryNotNested")
    private fun <T : KSort> testOperation(
        operation: KContext.(KExpr<KStringSort>, KExpr<KStringSort>, KExpr<KStringSort>) -> KExpr<T>,
        operationNoSimplify: KContext.(KExpr<KStringSort>, KExpr<KStringSort>, KExpr<KStringSort>) -> KExpr<T>,
        mkSpecialValues: KContext.(KStringSort) -> List<KExpr<KStringSort>> = { emptyList() }
    ) = runTest { sort: KStringSort, checker ->
        val a = mkConst("a", sort)
        val b = mkConst("b", sort)
        val c = mkConst("c", sort)
        val specialValues = mkSpecialValues(sort)

        (listOf(a) + specialValues).forEach { arg0 ->
            (listOf(b) + specialValues).forEach { arg1 ->
                (listOf(c) + specialValues).forEach { arg2 -> 
                    val unsimplifiedExpr = operationNoSimplify(arg0, arg1, arg2)
                    val simplifiedExpr = operation(arg0, arg1, arg2)
                    checker.check(unsimplifiedExpr = unsimplifiedExpr, simplifiedExpr = simplifiedExpr) {
                        "$arg0, $arg1, $arg2"
                    }
                }
            }
        }
    }

    private fun runTest(test: KContext.(KStringSort, TestRunner) -> Unit) = runTest(
        mkSort = { stringSort },
        test = test
    )

    companion object {
        private val TWO_TO_THE_63: BigInteger = BigInteger.ONE.shiftLeft(63)
        private val TWO_TO_THE_64: BigInteger = BigInteger.ONE.shiftLeft(64)
    }
}
