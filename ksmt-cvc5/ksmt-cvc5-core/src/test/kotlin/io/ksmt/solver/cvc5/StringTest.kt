package io.ksmt.solver.cvc5

import io.github.cvc5.Solver
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
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
            mkStringReplaceNoSimplify(left, right, mkConst("u", stringSort)),
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
            KCvc5Solver(this).use { solver ->
                val variable = mkConst("x", stringSort)
                val literal = mkStringLiteral(text)
                solver.assert(variable eq literal)
                assertEquals(KSolverStatus.SAT, solver.check(), describe(text))
                assertEquals(literal, solver.model().eval(variable), describe(text))
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
