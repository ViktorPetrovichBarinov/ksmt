package io.ksmt.solver.z3

import com.microsoft.z3.Context
import com.microsoft.z3.Native
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.KSolverUnsupportedFeatureException
import io.ksmt.sort.KRegexSort
import io.ksmt.sort.KSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StringTest {

    @Test
    fun testLiteralRoundTrip() = with(context()) {
        literals().forEach { text ->
            assertRoundTrip(mkStringLiteral(text), text)
        }
    }

    @Test
    fun testOperationRoundTrip() = with(context()) {
        val left = mkConst("s", stringSort)
        val right = mkConst("t", stringSort)
        val index = mkConst("i", intSort)
        val length = mkConst("n", intSort)
        val pattern = mkStringToRegexNoSimplify(right)

        listOf(
            mkStringConcatNoSimplify(left, right),
            mkStringLenNoSimplify(left),
            mkStringSuffixOfNoSimplify(left, right),
            mkStringPrefixOfNoSimplify(left, right),
            mkStringLtNoSimplify(left, right),
            mkStringLeNoSimplify(left, right),
            mkStringContainsNoSimplify(left, right),
            mkStringSingletonSubNoSimplify(left, index),
            mkStringSubNoSimplify(left, index, length),
            mkStringIndexOfNoSimplify(left, right, index),
            mkStringReplaceNoSimplify(left, right, mkConst("u", stringSort)),
            mkStringReplaceAllNoSimplify(left, right, mkConst("u", stringSort)),
            mkStringReplaceWithRegexNoSimplify(left, pattern, mkConst("u", stringSort)),
            mkStringReplaceAllWithRegexNoSimplify(left, pattern, mkConst("u", stringSort)),
            mkStringIsDigitNoSimplify(left),
            mkStringToCodeNoSimplify(left),
            mkStringFromCodeNoSimplify(index),
            mkStringToIntNoSimplify(left),
            mkStringFromIntNoSimplify(index),
            mkStringToRegexNoSimplify(left),
            mkStringInRegexNoSimplify(left, pattern),
            mkRegexConcatNoSimplify(pattern, mkRegexAllChar()),
            mkRegexUnionNoSimplify(pattern, mkRegexEpsilon()),
            mkRegexIntersectionNoSimplify(pattern, mkRegexAll()),
            mkRegexStarNoSimplify(pattern),
            mkRegexRangeNoSimplify(left, right)
        ).forEach { expr ->
            assertRoundTrip(expr, expr::class.simpleName ?: "expression")
        }
    }

    @Test
    fun testModelValueRoundTrip() = with(context()) {
        literals().forEach { text ->
            KZ3Solver(this).use { solver ->
                val variable = mkConst("x", stringSort)
                val literal = mkStringLiteral(text)
                solver.assert(variable eq literal)
                assertEquals(KSolverStatus.SAT, solver.check(), describe(text))
                assertEquals(literal, solver.model().eval(variable), describe(text))
            }
        }
    }

    @Test
    fun testUnsupportedOperationsStayRejected() = with(context()) {
        val text = mkConst("s", stringSort)
        val pattern = mkStringToRegexNoSimplify(text)
        val index = mkConst("i", intSort)

        KZ3Solver(this).use { solver ->
            fun <T : KSort> reject(expr: KExpr<T>) {
                val failure = assertFailsWith<KSolverUnsupportedFeatureException>(expr.toString()) {
                    solver.assert(expr eq expr)
                }
                assertTrue(failure.message!!.contains("not supported in z3"), failure.message)
            }

            reject(mkStringIndexOfRegexNoSimplify(text, pattern, index))
            reject(mkStringToLowerNoSimplify(text))
            reject(mkStringToUpperNoSimplify(text))
            reject(mkStringReverseNoSimplify(text))
        }
    }

    @Test
    fun testPrintedLiteralParsesBack() = with(context()) {
        listOf(
            "\"",
            "\\",
            "\n",
            "\u0000",
            String(Character.toChars(0x1F44D))
        ).forEach { text ->
            val literal = mkStringLiteral(text)
            val variable = mkConst("s", stringSort)
            val assertion = variable eq literal
            val parsed = KZ3SMTLibParser(this).parse(
                "(declare-fun s () String)\n(assert $assertion)"
            )
            assertEquals(listOf(assertion), parsed, literal.toString())
        }
    }

    @Test
    fun testThreeArgumentsBecomeNestedPairs() = checkArguments(3)

    @Test
    fun testFourArgumentsBecomeNestedPairs() = checkArguments(4)

    @Test
    fun testFiveArgumentsBecomeNestedPairs() = checkArguments(5)

    @Test
    fun testSixArgumentsBecomeNestedPairs() = checkArguments(6)

    @Test
    fun testSevenArgumentsBecomeNestedPairs() = checkArguments(7)

    @Test
    fun testEightArgumentsBecomeNestedPairs() = checkArguments(8)

    @Test
    fun testTwentyArgumentsBecomeNestedPairs() = checkArguments(20)

    private fun checkArguments(count: Int) = with(context()) {
        val strings = List(count) { mkConst("s$it", stringSort) }
        val patterns: List<KExpr<KRegexSort>> = strings.map { mkStringToRegexNoSimplify(it) }

        Context().use { z3Native ->
            val internalizer = KZ3ExprInternalizer(this, KZ3Context(this, z3Native))
            val converter = KZ3ExprConverter(this, KZ3Context(this, z3Native))
            fun native(expr: KExpr<*>) = with(internalizer) { expr.internalizeExpr() }
            fun restore(nativeExpr: Long): KExpr<*> = with(converter) { nativeExpr.convertExpr<KSort>() }
            fun check(expected: KExpr<*>, nativeExpr: Long, label: String) {
                assertEquals(expected, restore(nativeExpr), "$count $label")
            }

            val nativeStrings = LongArray(count) { native(strings[it]) }
            val nativePatterns = LongArray(count) { native(patterns[it]) }

            check(
                strings.reduceRight { left, right -> mkStringConcatNoSimplify(left, right) },
                Native.mkSeqConcat(internalizer.nCtx, count, nativeStrings),
                "concat"
            )
            check(
                patterns.reduce { left, right -> mkRegexConcatNoSimplify(left, right) },
                Native.mkReConcat(internalizer.nCtx, count, nativePatterns),
                "re.++"
            )
            check(
                patterns.reduce { left, right -> mkRegexUnionNoSimplify(left, right) },
                Native.mkReUnion(internalizer.nCtx, count, nativePatterns),
                "re.union"
            )
            check(
                patterns.reduce { left, right -> mkRegexIntersectionNoSimplify(left, right) },
                Native.mkReIntersect(internalizer.nCtx, count, nativePatterns),
                "re.inter"
            )
        }
    }

    private fun KContext.assertRoundTrip(expr: KExpr<*>, label: String) {
        val ctx = this
        Context().use { z3Native ->
            val internalizer = KZ3ExprInternalizer(ctx, KZ3Context(ctx, z3Native))
            val converter = KZ3ExprConverter(ctx, KZ3Context(ctx, z3Native))
            val converted = with(internalizer) { expr.internalizeExpr() }
            val restored = with(converter) { converted.convertExpr<KSort>() }
            assertEquals(expr, restored, label)
        }
    }

    companion object {
        init {
            KZ3Solver(KContext()).close()
        }

        private fun context() = KContext(simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY)

        private fun literals(): List<String> = listOf(
            "hello",
            "",
            "\u0000",
            String(Character.toChars(0xD800)),
            String(Character.toChars(0xFFFF)),
            String(Character.toChars(0x1F44D))
        )

        private fun describe(text: String): String {
            val points = text.codePoints().toArray().joinToString { "U+%04X".format(it) }
            return "string [$points]"
        }
    }
}
