/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.errors

import org.jetbrains.kotlin.formver.viper.ast.AstWrapper
import org.jetbrains.kotlin.formver.viper.ast.Position

class VerificationError(
    val result: viper.silver.verifier.VerificationError,
) : VerifierError {

    /**
     * The [locationNode] represents the node in Viper's AST where the error occurred.
     * This is useful with error reporting, when we want to fetch missing information from the error,
     * and we need to reconstruct the original Kotlin's code context.
     */
    val locationNode: AstWrapper.Node
        get() = AstWrapper.Node(result.offendingNode())

    /**
     * The proposition that could not be verified, or null when the reason does not blame one.
     *
     * A termination error blames the callee's declaration rather than an expression, so the reason's
     * offending node is not always an [viper.silver.ast.Exp].
     */
    val unverifiableProposition: AstWrapper.Exp?
        get() = (result.reason().offendingNode() as? viper.silver.ast.Exp)?.let { AstWrapper.Exp(it) }
    override val id: String
        get() = result.id()
    override val msg: String
        get() = result.readableMessage(false, false)
    override val position: Position
        get() = Position.fromSilver(result.pos())
}
