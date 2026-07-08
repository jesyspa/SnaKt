package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.INVALID_DUPLICATE_UNIQUE_ARGUMENT
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors.INVALID_OVERLAPPING_UNIQUE_ARGUMENTS

/**
 * Maps expressions that act as arguments to the uniqueness required by the corresponding parameter-like declaration.
 */
fun interface ArgumentUniquenessesMapper<Statement : FirStatement> {
    context(context: CheckerContext)
    fun mapArgumentUniquenessesOf(statement: Statement): List<Pair<FirExpression, Uniqueness>>
}

private fun Path.computeCommonPrefix(other: Path): Int {
    var commonPrefix = 0

    while (commonPrefix < size && commonPrefix < other.size) {
        if (this[commonPrefix] == other[commonPrefix]) {
            commonPrefix++
        } else {
            break
        }
    }

    return commonPrefix
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun reportUniquenessCollision(
    ownerElement: FirElement,
    leftArgument: FirExpression,
    rightArgument: FirExpression
) {
    for (leftPath in leftArgument.resolveAccessState().enumeratePaths()) {
        for (rightPath in rightArgument.resolveAccessState().enumeratePaths()) {
            val commonPrefix = leftPath.computeCommonPrefix(rightPath)

            if (commonPrefix == 0) continue

            val leftSource = leftArgument.source ?: ownerElement.source
            val rightSource = rightArgument.source ?: ownerElement.source

            if (commonPrefix == leftPath.size && commonPrefix == rightPath.size) {
                if (leftSource != rightSource) {
                    reporter.reportOn(leftSource, INVALID_DUPLICATE_UNIQUE_ARGUMENT, leftPath)
                }
                reporter.reportOn(rightSource, INVALID_DUPLICATE_UNIQUE_ARGUMENT, rightPath)
            } else {
                if (leftSource != rightSource) {
                    reporter.reportOn(leftArgument.source, INVALID_OVERLAPPING_UNIQUE_ARGUMENTS, leftPath, rightPath)
                }
                reporter.reportOn(rightArgument.source, INVALID_OVERLAPPING_UNIQUE_ARGUMENTS, rightPath, leftPath)
            }
        }
    }
}

/**
 * Checker for detecting collisions between expressions passed to unique arguments of the same expression.
 *
 * Two unique arguments collide when their access paths are identical, or when one access path is a prefix of the other.
 * For example, passing both `x` and `x.f` to unique parameters is invalid because the accesses overlap.
 *
 * @param Statement the FIR expression kind handled by this checker.
 * @param argumentUniquenessMapper the mapper for resolving required uniqueness of argument-like expressions.
 * @param checks the predicate deciding whether this checker handles a particular expression instance.
 */
class ExpressionArgumentUniquenessCollisionChecker<Statement : FirStatement>(
    private val argumentUniquenessMapper: ArgumentUniquenessesMapper<Statement>,
    private val checks: (Statement) -> Boolean = { true }
) : FirExpressionChecker<Statement>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: Statement) {
        if (!checks(expression)) return

        val argumentUniquenesses = argumentUniquenessMapper.mapArgumentUniquenessesOf(expression)
        val uniqueArguments =
            argumentUniquenesses
                .filter { (_, uniqueness) -> uniqueness == Uniqueness.Unique }
                .map { (expression, _) -> expression }

        for ((index, leftArgument) in uniqueArguments.withIndex()) {
            for (rightArgument in uniqueArguments.subList(index + 1, uniqueArguments.size)) {
                reportUniquenessCollision(expression, leftArgument, rightArgument)
            }
        }
    }
}

/**
 * Checks collisions between value arguments, receivers, and context arguments of [FirFunctionCall]s.
 */
val FunctionCallArgumentUniquenessCollisionChecker =
    ExpressionArgumentUniquenessCollisionChecker<FirFunctionCall>(
        { expression ->
            QualifiedAccessArgumentUniquenessMapper.mapArgumentTypeFactsOf(expression) +
                    CallArgumentUniquenessesMapper.mapArgumentTypeFactsOf(expression)
        }
    )

/**
 * Checks collisions between receivers and context arguments of qualified accesses that are not [FirFunctionCall]s.
 */
val QualifiedAccessArgumentUniquenessCollisionChecker =
    ExpressionArgumentUniquenessCollisionChecker<FirQualifiedAccessExpression>(
        { expression ->
            QualifiedAccessArgumentUniquenessMapper.mapArgumentTypeFactsOf(expression)
        },
        { statement -> statement !is FirFunctionCall }
    )
