/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignatureWithContract
import org.jetbrains.kotlin.formver.core.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.expression.FunctionExp

fun StmtConversionContext.convertImpureBody(
    declaration: FirSimpleFunction,
    signature: NamedFunctionSignatureWithContract,
    returnTarget: ReturnTarget,
): ConvertedMethodBody? {
    val firBody = declaration.body ?: return null
    val body = convert(firBody)
    val returnLabel = returnTarget.label ?: throw SnaktInternalException(
        declaration.source, "Return target label not found for method ${declaration.name}"
    )
    val bodyExp = FunctionExp(signature, body, returnLabel)
    return ConvertedMethodBody(bodyExp, returnTarget)
}

fun StmtConversionContext.convertPureBody(declaration: FirSimpleFunction): ExpEmbedding {
    val firBody = declaration.body ?: throw SnaktInternalException(
        declaration.source,
        "Pure functions expect a function body to exist"
    )
    return convert(firBody)
}
