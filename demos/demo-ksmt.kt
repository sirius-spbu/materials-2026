package ksmt

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInt32NumExpr
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KIntSort
import io.ksmt.utils.mkConst

fun main() {
    val ctx = KContext()

    val n = 8

    var numberOfPermutaitons = 0

    with(ctx) {
        val rowVars: List<KExpr<KIntSort>> =
            List(8) { intSort.mkConst("row$it") }


        val solver = KZ3Solver(this)

        solver.assert(mkDistinct(rowVars))

        val diagValues1 = rowVars.withIndex()
            .map { (idx, expr) -> idx.expr + expr }
        val diagValues2 = rowVars.withIndex()
            .map { (idx, expr) -> idx.expr - expr }

        solver.assert(mkDistinct(diagValues1))
        solver.assert(mkDistinct(diagValues2))

        val rangeConstraints = rowVars.map { rowVar ->
            (0.expr le rowVar) and (rowVar lt n.expr)
        }

        solver.assert(rangeConstraints)

        while (true) {
            val status = solver.check()

            when (status) {
                KSolverStatus.SAT -> {
                    numberOfPermutaitons++

                    val model = solver.model()

                    val table = Array(8) { Array(8) { '.' } }

                    val banConstraints = mutableListOf<KExpr<KBoolSort>>()

                    for ((row, rowVar) in rowVars.withIndex()) {
                        val evaluatedRowVar = (model.eval(rowVar, isComplete = true) as KInt32NumExpr)
                            .value

                        banConstraints += rowVar eq evaluatedRowVar.expr


                        table[row][evaluatedRowVar] = 'q'
                    }

                    val banConstraint = mkAnd(banConstraints).not()

                    solver.assert(banConstraint)

                    println(table.joinToString("\n") { it.joinToString("") })
                }

                KSolverStatus.UNSAT -> {
                    println("No solution")
                    break
                }

                KSolverStatus.UNKNOWN -> {
                    println("Unknown")
                    break
                }
            }
        }


        solver.close()
    }

    println("Total number of permuations: $numberOfPermutaitons")
}