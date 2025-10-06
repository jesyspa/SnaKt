/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import NameScope
import SimpleScope
import org.jetbrains.kotlin.formver.viper.Lit
import org.jetbrains.kotlin.formver.viper.NameExpr
import org.jetbrains.kotlin.formver.viper.SymbolicName

/* This file contains mangled names for constructs introduced during the conversion to Viper.
 *
 * See the NameEmbeddings file for guidelines on good name choices.
 */

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(val n: Int) : SymbolicName {
    override val mangledType: String
        get() = "anon"

    override val mangledBaseName: NameExpr
        get() = Lit(n.toString())

    override val requiresType = true
}

data class AnonymousBuiltinName(val n: Int) : SymbolicName {
    override val mangledType: String
        get() = $$"anon$builtin"

    override val mangledBaseName: NameExpr
        get() = Lit(n.toString())

    override val requiresType = true
}

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
data object PlaceholderReturnVariableName : SymbolicName {
    override val mangledBaseName: NameExpr
        get() = Lit("ret")

    override val requiresType = true
}

data class ReturnVariableName(val n: Int) : SymbolicName {
    override val mangledType: String
        get() = "ret"

    override val mangledBaseName: NameExpr
        get() = Lit(n.toString())

    override val requiresType = true
}

data object DispatchReceiverName : SymbolicName {
    override val mangledBaseName: NameExpr
        get() = Lit($$"this$dispatch")
}

data object ExtensionReceiverName : SymbolicName {
    override val mangledBaseName: NameExpr
        get() = Lit($$"this$extension")
}

data class SpecialName(val baseName: String) : SymbolicName {
    override val mangledBaseName: NameExpr
        get() = Lit(baseName)
    override val mangledType: String
        get() = "sp"
    override val requiresType = true
}

abstract class NumberedLabelName(val scope: String, val originalN: Int) : SymbolicName {
    override val mangledType: String
        get() = "lbl"

    override val mangledBaseName: NameExpr
        get() = Lit(originalN.toString())

    override val mangledScope: NameScope
        get() = SimpleScope(Lit(scope))

    override val requiresType = true
}

data class ReturnLabelName(val scopeDepth: Int) : NumberedLabelName("ret", scopeDepth)
data class BreakLabelName(val n: Int) : NumberedLabelName("break", n)
data class ContinueLabelName(val n: Int) : NumberedLabelName("continue", n)
data class CatchLabelName(val n: Int) : NumberedLabelName("catch", n)
data class TryExitLabelName(val n: Int) : NumberedLabelName("try_exit", n)


data class PlaceholderArgumentName(val n: Int) : SymbolicName {
    override val mangledBaseName: NameExpr
        get() = Lit("arg$n")

    override val requiresType = true
}

data class DomainFuncParameterName(val baseName: String) : SymbolicName {
    override val mangledBaseName: NameExpr
        get() = Lit(baseName)
}