/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.formver.core.embeddings.LabelEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.VariableEmbedding
import org.jetbrains.kotlin.formver.viper.SymbolicName

/**
 * Interface for statement conversion.
 *
 * Naming convention:
 * - Functions that return a new `StmtConversionContext` should describe what change they make (`addResult`, `removeResult`...)
 * - Functions that take a lambda to execute should describe what extra state the lambda will have (`withResult`...)
 */
interface StmtConversionContext : MethodConversionContext {
    val whenSubject: VariableEmbedding?

    /**
     * In a safe call `callSubject?.foo()` we evaluate the call subject first to check for nullness.
     * In case it is not null, we evaluate the call to `callSubject.foo()`. Here we don't want to evaluate
     * the `callSubject` again to we store it in the `StmtConversionContext`.
     */
    val checkedSafeCallSubject: ExpEmbedding?
    val activeCatchLabels: List<LabelEmbedding>

    fun continueLabelName(targetName: String? = null): SymbolicName
    fun breakLabelName(targetName: String? = null): SymbolicName
    fun addLoopName(targetName: String)
    fun convert(stmt: FirStatement): ExpEmbedding

    fun <R> withNewScope(action: StmtConversionContext.() -> R): R
    fun <R> withNoScope(action: StmtConversionContext.() -> R): R
    fun <R> withMethodCtx(factory: MethodContextFactory, action: StmtConversionContext.() -> R): R

    fun <R> withFreshWhile(label: FirLabel?, action: StmtConversionContext.() -> R): R
    fun <R> withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext.() -> R): R
    fun <R> withCheckedSafeCallSubject(subject: ExpEmbedding?, action: StmtConversionContext.() -> R): R
    fun <R> withCatches(
        catches: List<FirCatch>,
        action: StmtConversionContext.(catchBlockListData: CatchBlockListData) -> R,
    ): Pair<CatchBlockListData, R>
}
