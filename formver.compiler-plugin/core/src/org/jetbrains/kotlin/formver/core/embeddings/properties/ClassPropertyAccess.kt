/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties

import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.withNewTypeInvariants
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding

// We assume that thanks to the checks done by the Kotlin compiler, a property with a
// missing getter or setter will never be accessed.
class ClassPropertyAccess(val receiver: ExpEmbedding, val property: PropertyEmbedding, val type: TypeEmbedding) :
    PropertyAccessEmbedding {
    override fun getValue(ctx: StmtConversionContext): ExpEmbedding =
        property.getter!!.getValue(receiver, ctx).withNewTypeInvariants(type) {
            proven = true
            access = true
        }

    // set value must already have correct type so no need to worry
    override fun setValue(value: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding =
        property.setter!!.setValue(receiver, value, ctx)
}
