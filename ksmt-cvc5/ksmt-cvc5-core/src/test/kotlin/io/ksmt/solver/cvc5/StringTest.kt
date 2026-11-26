package io.ksmt.solver.cvc5

import io.github.cvc5.Kind
import io.github.cvc5.Solver
import io.github.cvc5.Term
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KRegexSort
import io.ksmt.sort.KSort
import kotlin.test.Test
import kotlin.test.assertEquals

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
            mkStringIndexOfRegexNoSimplify(left, pattern, index),
            mkStringReplaceNoSimplify(left, right, mkConst("u", stringSort)),
            mkStringReplaceAllNoSimplify(left, right, mkConst("u", stringSort)),
            mkStringReplaceWithRegexNoSimplify(left, pattern, mkConst("u", stringSort)),
            mkStringReplaceAllWithRegexNoSimplify(left, pattern, mkConst("u", stringSort)),
            mkStringIsDigitNoSimplify(left),
            mkStringToCodeNoSimplify(left),
            mkStringFromCodeNoSimplify(index),
            mkStringToIntNoSimplify(left),
            mkStringFromIntNoSimplify(index),
            mkStringToLowerNoSimplify(left),
            mkStringToUpperNoSimplify(left),
            mkStringReverseNoSimplify(left),
            mkStringToRegexNoSimplify(left),
            mkStringInRegexNoSimplify(left, pattern),
            mkRegexConcatNoSimplify(pattern, mkRegexAllChar()),
            mkRegexUnionNoSimplify(pattern, mkRegexEpsilon()),
            mkRegexIntersectionNoSimplify(pattern, mkRegexAll()),
            mkRegexStarNoSimplify(pattern),
            mkRegexCrossNoSimplify(pattern),
            mkRegexDifferenceNoSimplify(pattern, mkRegexEpsilon()),
            mkRegexComplementNoSimplify(pattern),
            mkRegexOptionNoSimplify(pattern),
            mkRegexRangeNoSimplify(left, right),
            mkRegexPowerNoSimplify(2, pattern),
            mkRegexLoopNoSimplify(1, 3, pattern)
        ).forEach { expr ->
            assertRoundTrip(expr, expr::class.simpleName ?: "expression")
        }
    }

    @Test
    fun testModelValueRoundTrip() = with(context()) {
        literals().forEach { text ->
            KCvc5Solver(this).use { solver ->
                val variable = mkConst("x", stringSort)
                val literal = mkStringLiteral(text)
                solver.assert(variable eq literal)
                assertEquals(KSolverStatus.SAT, solver.check(), describe(text))
                assertEquals(literal, solver.model().eval(variable), describe(text))
            }
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
        val ctx = this

        KCvc5TermManager().use { terms ->
            val cvc5Ctx = KCvc5Context(terms, ctx)
            val nativeSolver = terms.builder { Solver(this) }
            try {
                val internalizer = KCvc5ExprInternalizer(cvc5Ctx, nativeSolver)
                val converter = KCvc5ExprConverter(ctx, cvc5Ctx, nativeSolver)
                fun native(expr: KExpr<*>) = with(internalizer) { expr.internalizeExpr() }
                fun restore(nativeExpr: Term): KExpr<*> = with(converter) { nativeExpr.convertExpr<KSort>() }
                fun check(expected: KExpr<*>, nativeExpr: Term, label: String) {
                    assertEquals(expected, restore(nativeExpr), "$count $label")
                }

                val nativeStrings = Array(count) { native(strings[it]) }
                val nativePatterns = Array(count) { native(patterns[it]) }

                check(
                    strings.reduce { left, right -> mkStringConcatNoSimplify(left, right) },
                    terms.mkTerm(Kind.STRING_CONCAT, nativeStrings),
                    "concat"
                )
                check(
                    patterns.reduce { left, right -> mkRegexConcatNoSimplify(left, right) },
                    terms.mkTerm(Kind.REGEXP_CONCAT, nativePatterns),
                    "re.++"
                )
                check(
                    patterns.reduce { left, right -> mkRegexUnionNoSimplify(left, right) },
                    terms.mkTerm(Kind.REGEXP_UNION, nativePatterns),
                    "re.union"
                )
                check(
                    patterns.reduce { left, right -> mkRegexIntersectionNoSimplify(left, right) },
                    terms.mkTerm(Kind.REGEXP_INTER, nativePatterns),
                    "re.inter"
                )
            } finally {
                cvc5Ctx.close()
            }
        }
    }

    private fun KContext.assertRoundTrip(expr: KExpr<*>, label: String) {
        val ctx = this
        KCvc5TermManager().use { terms ->
            val cvc5Ctx = KCvc5Context(terms, ctx)
            val nativeSolver = terms.builder { Solver(this) }
            try {
                val internalizer = KCvc5ExprInternalizer(cvc5Ctx, nativeSolver)
                val converter = KCvc5ExprConverter(ctx, cvc5Ctx, nativeSolver)
                val converted = with(internalizer) { expr.internalizeExpr() }
                val restored = with(converter) { converted.convertExpr<KSort>() }
                assertEquals(expr, restored, label)
            } finally {
                cvc5Ctx.close()
            }
        }
    }

    companion object {
        init {
            KCvc5Solver(KContext()).close()
        }

        private fun context() = KContext(simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY)

        private fun literals(): List<String> = listOf(
            "hello",
            "",
            "\n",
            "\t",
            "\u007F",
            "a\nb",
            "\"",
            "\\",
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
