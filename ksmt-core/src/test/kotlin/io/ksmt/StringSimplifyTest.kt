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
