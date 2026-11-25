package io.ksmt.solver.z3

import com.microsoft.z3.Context
import com.microsoft.z3.Native
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
    fun testThreeArgumentsBecomeNestedPairs() = with(context()) {
        val first = mkConst("a", stringSort)
        val second = mkConst("b", stringSort)
        val third = mkConst("c", stringSort)
        val firstPattern = mkStringToRegexNoSimplify(first)
        val secondPattern = mkStringToRegexNoSimplify(second)
        val thirdPattern = mkStringToRegexNoSimplify(third)

        Context().use { z3Native ->
            val internalizer = KZ3ExprInternalizer(this, KZ3Context(this, z3Native))
            val converter = KZ3ExprConverter(this, KZ3Context(this, z3Native))
            fun native(expr: KExpr<*>) = with(internalizer) { expr.internalizeExpr() }
            fun restore(nativeExpr: Long): KExpr<*> = with(converter) { nativeExpr.convertExpr<KSort>() }

            val strings = longArrayOf(native(first), native(second), native(third))
            val patterns = longArrayOf(native(firstPattern), native(secondPattern), native(thirdPattern))
            fun check(expected: KExpr<*>, nativeExpr: Long, label: String) {
                assertEquals(expected, restore(nativeExpr), label)
            }

            check(
                mkStringConcatNoSimplify(first, mkStringConcatNoSimplify(second, third)),
                Native.mkSeqConcat(internalizer.nCtx, 3, strings),
                "concat"
            )
            check(
                mkRegexConcatNoSimplify(
                    mkRegexConcatNoSimplify(firstPattern, secondPattern),
                    thirdPattern
                ),
                Native.mkReConcat(internalizer.nCtx, 3, patterns),
                "re.++"
            )
            check(
                mkRegexUnionNoSimplify(
                    mkRegexUnionNoSimplify(firstPattern, secondPattern),
                    thirdPattern
                ),
                Native.mkReUnion(internalizer.nCtx, 3, patterns),
                "re.union"
            )
            check(
                mkRegexIntersectionNoSimplify(
                    mkRegexIntersectionNoSimplify(firstPattern, secondPattern),
                    thirdPattern
                ),
                Native.mkReIntersect(internalizer.nCtx, 3, patterns),
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
