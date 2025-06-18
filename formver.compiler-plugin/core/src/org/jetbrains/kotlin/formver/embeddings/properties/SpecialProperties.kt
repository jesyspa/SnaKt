/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.properties

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.formver.kotlinCallableId
import org.jetbrains.kotlin.name.CallableId

object SpecialProperties {
    val byCallableId: Map<CallableId, PropertyEmbedding> = mapOf(
        kotlinCallableId("String", "length") to PropertyEmbedding(LengthFieldGetter, setter = null)
    )

    fun isSpecial(symbol: FirPropertySymbol) = symbol.callableId in byCallableId.keys
}