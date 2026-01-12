/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.properties

import org.jetbrains.kotlin.formver.core.conversion.AccessPolicy
import org.jetbrains.kotlin.formver.core.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FieldAccess
import org.jetbrains.kotlin.formver.core.embeddings.expression.FieldModification
import org.jetbrains.kotlin.formver.core.embeddings.expression.withInvariants

class BackingFieldGetter(val field: FieldEmbedding, val manual: Boolean) : GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding {
        return if (field.accessPolicy == AccessPolicy.ALWAYS_READABLE) {
            FieldAccess(receiver, field)
        } else {
            if (manual) {
                // TODO: figure out how to pass in programmer specified permissions
                FieldAccess(receiver, field).withInvariants {
                    proven = false
                    access = false
                }
            } else {
                FieldAccess(receiver, field).withInvariants {
                    proven = true
                    access = true
                }
            }
        }
    }
}

class BackingFieldSetter(val field: FieldEmbedding, val manual: Boolean) : SetterEmbedding {
    override fun setValue(receiver: ExpEmbedding, value: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding =
        FieldModification(receiver, field, value)
}

