package io.ksmt.test

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.KSolverUnsupportedFeatureException
import io.ksmt.solver.bitwuzla.KBitwuzlaSolver
import io.ksmt.solver.cvc5.KCvc5Solver
import io.ksmt.solver.yices.KYicesSolver
import io.ksmt.solver.z3.KZ3SMTLibParser
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StringSmtLibSampleTest {
    @Test
    fun testZ3AndCvc5Agree() {
        assertStatus(satSample, KSolverStatus.SAT)
        assertStatus(unsatSample, KSolverStatus.UNSAT)
    }

    @Test
    fun testBitwuzlaAndYicesRejectTheSample() = with(KContext()) {
        val assertions = KZ3SMTLibParser(this).parse(satSample)
        listOf(
            { ctx: KContext -> KBitwuzlaSolver(ctx) },
            { ctx: KContext -> KYicesSolver(ctx) }
        ).forEach { mkSolver ->
            mkSolver(this).use { solver ->
                assertions.forEach { expr ->
                    val failure = assertFailsWith<KSolverUnsupportedFeatureException>(expr.toString()) {
                        solver.assert(expr)
                    }
                    assertTrue(
                        failure.message!!.contains("string theory is not supported"),
                        failure.message
                    )
                }
            }
        }
    }

    private fun assertStatus(sample: String, expected: KSolverStatus) = with(KContext()) {
        val assertions = KZ3SMTLibParser(this).parse(sample)
        val z3 = check(KZ3Solver(this), assertions)
        val cvc5 = check(KCvc5Solver(this), assertions)
        assertEquals(expected, z3)
        assertEquals(z3, cvc5)
    }

    private fun check(solver: KSolver<*>, assertions: List<KExpr<KBoolSort>>): KSolverStatus =
        solver.use {
            assertions.forEach { assertion -> it.assert(assertion) }
            it.check()
        }

    companion object {
        private val satSample = """
            (set-logic QF_S)
            (declare-fun s () String)
            (assert (str.contains s "a"))
        """.trimIndent()

        private val unsatSample = """
            (set-logic QF_S)
            (declare-fun s () String)
            (assert (str.contains s "a"))
            (assert (not (str.contains s "a")))
        """.trimIndent()
    }
}
