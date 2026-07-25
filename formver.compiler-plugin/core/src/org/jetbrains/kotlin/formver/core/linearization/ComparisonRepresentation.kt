/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.linearization

import org.jetbrains.kotlin.formver.core.domains.Injection
import org.jetbrains.kotlin.formver.core.domains.viperType
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.injectionOrNull
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * The Viper representation in which the two operands of a comparison are compared.
 *
 * A Viper `==` requires both of its operands to have the same sort, so a comparison has to settle
 * on one representation and linearize both operands into it.
 */
sealed interface ComparisonRepresentation {
    /** Linearizes [operand], the embedding type of which is [type], into this representation. */
    fun operandToViper(operand: Linearizable, type: TypeEmbedding, ctx: LinearizationContext): Exp

    /**
     * Compare the operands as the builtin type [injection] injects into.
     *
     * Available only when both operands are represented by [injection], and mandatory whenever it
     * is: an injection is not required to be surjective, so two `Ref`s obtained by injecting equal
     * builtin values need not be equal themselves, and comparing two `Int`s as `Ref`s would leave
     * `1 == 1` unprovable.
     */
    data class Builtin(val injection: Injection) : ComparisonRepresentation {
        override fun operandToViper(operand: Linearizable, type: TypeEmbedding, ctx: LinearizationContext): Exp {
            // `toViperBuiltinType` unwraps the operand through the injection of its own type rather
            // than through ours; `shared` picks this representation exactly when the two coincide.
            check(type.injectionOrNull == injection) {
                "Comparison operand is represented as ${type.injectionOrNull.viperType}, expected ${injection.viperType}."
            }
            return operand.toViperBuiltinType(ctx)
        }
    }

    /**
     * Compare the operands as `Ref`s.
     *
     * The only representation available when the operands have no builtin one in common: class
     * types, `Any` and every nullable type are represented as `Ref` and nothing else. Reference
     * equality asks for this representation whatever its operand types are.
     */
    data object Refs : ComparisonRepresentation {
        override fun operandToViper(operand: Linearizable, type: TypeEmbedding, ctx: LinearizationContext): Exp =
            operand.toViper(ctx)
    }

    companion object {
        /** The representation that operands of types [left] and [right] have in common. */
        fun shared(left: TypeEmbedding, right: TypeEmbedding): ComparisonRepresentation {
            val injection = left.injectionOrNull ?: return Refs
            return if (injection == right.injectionOrNull) Builtin(injection) else Refs
        }
    }
}
