/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.uniqueness.plugin

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class UniquenessExtensionRegistrar(
    private val uniquenessAnnotationId: ClassId = defaultUniquenessAnnotationId
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        registerDiagnosticContainers(UniquenessErrors)
        +ExpressionAccessStateResolver.getFactory()
        +ExpressionUniquenessResolver.getFactory()
        +GraphUniquenessStatesResolver.getFactory()
        +SymbolUniquenessResolver.getFactory()
        +UniquenessAdditionalCheckers.getFactory()
        +UniquenessAttributeExtension.getFactory(uniquenessAnnotationId)
    }
}
