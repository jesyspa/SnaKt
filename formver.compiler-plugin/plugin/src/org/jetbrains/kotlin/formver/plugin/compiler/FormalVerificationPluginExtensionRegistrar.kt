/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin.compiler

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.formver.common.PluginConfiguration
import org.jetbrains.kotlin.formver.core.diagnostics.ConversionErrors
import org.jetbrains.kotlin.formver.core.isSpecificationCall
import org.jetbrains.kotlin.formver.locality.contract.plugin.ExpressionLocalityContractResolver
import org.jetbrains.kotlin.formver.locality.contract.plugin.LocalityContractErrors
import org.jetbrains.kotlin.formver.locality.plugin.ExpressionLocalityResolver
import org.jetbrains.kotlin.formver.locality.plugin.GraphCapturedSymbolsResolver
import org.jetbrains.kotlin.formver.locality.plugin.GraphDeclaredSymbolsResolver
import org.jetbrains.kotlin.formver.locality.plugin.GraphScopeLocalityResolver
import org.jetbrains.kotlin.formver.locality.plugin.LocalityAttributeExtension
import org.jetbrains.kotlin.formver.locality.plugin.LocalityErrors
import org.jetbrains.kotlin.formver.uniqueness.plugin.ExpressionAccessStateResolver
import org.jetbrains.kotlin.formver.uniqueness.plugin.ExpressionUniquenessResolver
import org.jetbrains.kotlin.formver.uniqueness.plugin.GraphUniquenessStatesResolver
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessAttributeExtension
import org.jetbrains.kotlin.formver.uniqueness.plugin.UniquenessErrors

class FormalVerificationPluginExtensionRegistrar(private val config: PluginConfiguration) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        registerDiagnosticContainers(PluginErrors)
        registerDiagnosticContainers(VerificationErrors)
        registerDiagnosticContainers(ConversionErrors)

        // Locality resolvers
        +ExpressionLocalityContractResolver.getFactory()
        +ExpressionLocalityResolver.getFactory()
        +GraphCapturedSymbolsResolver.getFactory()
        +GraphDeclaredSymbolsResolver.getFactory()
        +GraphScopeLocalityResolver.getFactory()
        +LocalityAttributeExtension.getFactory()

        // Uniqueness resolvers
        +ExpressionAccessStateResolver.getFactory()
        +ExpressionUniquenessResolver.getFactory()
        +GraphUniquenessStatesResolver.getFactory { it.isSpecificationCall() }
        +UniquenessAttributeExtension.getFactory()

        if (config.checkLocality) {
            registerDiagnosticContainers(LocalityErrors)
            registerDiagnosticContainers(LocalityContractErrors)
        }

        if (config.checkUniqueness) {
            registerDiagnosticContainers(UniquenessErrors)
        }

        +PluginAdditionalCheckers.getFactory(config)
    }
}
