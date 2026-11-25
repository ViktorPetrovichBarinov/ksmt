package io.ksmt.solver.yices

import io.ksmt.KContext
import io.ksmt.solver.KSolverUnsupportedFeatureException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StringTest {
    @Test
    fun testStringTheoryIsRejected() = with(KContext(simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY)) {
        val left = mkConst("s", stringSort)
        val right = mkConst("t", stringSort)
        val pattern = mkStringToRegexNoSimplify(right)
        val formulas = listOf(
            mkStringContainsNoSimplify(left, right),
            mkStringLtNoSimplify(left, right),
            mkStringInRegexNoSimplify(left, pattern)
        )

        KYicesSolver(this).use { solver ->
            formulas.forEach { expr ->
                val failure = assertFailsWith<KSolverUnsupportedFeatureException>(expr.toString()) {
                    solver.assert(expr)
                }
                assertTrue(
                    failure.message!!.contains("string theory is not supported in Yices"),
                    failure.message
                )
            }
        }
    }
}
