/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.NameResolver

/* This file contains mangled names for constructs introduced during the conversion to Viper.
 *
 * See the NameEmbeddings file for guidelines on good name choices.
 */

/**
 * Representation for names not present in the original source,
 * e.g. storage for the result of subexpressions.
 */
data class AnonymousName(val n: Int) : MangledName {
    override val mangledType: String
        get() = "anon"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = n.toString()
}

data class AnonymousBuiltinName(val n: Int) : MangledName {

    override val mangledType: String
        get() = $$"anon$builtin"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = n.toString()
}

/**
 * Name for return variable that should *only* be used in signatures of methods without a body.
 */
data object PlaceholderReturnVariableName : MangledName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "ret"
}

data class ReturnVariableName(val n: Int) : MangledName {
    override val mangledType: String
        get() = "ret"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = n.toString()
}

data object DispatchReceiverName : MangledName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"this$dispatch"
}

data object ExtensionReceiverName : MangledName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = $$"this$extension"
}

data class SpecialName(val BaseName: String) : MangledName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = BaseName
    override val mangledType: String
        get() = "sp"
}

abstract class NumberedLabelName(val Scope: String, val originalN: Int) : MangledName {
    override val mangledType: String
        get() = "lbl"

    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = originalN.toString()

    context(nameResolver: NameResolver)
    override val mangledScope: String?
        get() = Scope
}

data class ReturnLabelName(val scopeDepth: Int) : NumberedLabelName("ret", scopeDepth)
data class BreakLabelName(val n: Int) : NumberedLabelName("break", n)
data class ContinueLabelName(val n: Int) : NumberedLabelName("continue", n)
data class CatchLabelName(val n: Int) : NumberedLabelName("catch", n)
data class TryExitLabelName(val n: Int) : NumberedLabelName("try_exit", n)


data class PlaceholderArgumentName(val n: Int) : MangledName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = "arg$n"
}

data class DomainFuncParameterName(val BaseName: String) : MangledName {
    context(nameResolver: NameResolver)
    override val mangledBaseName: String
        get() = BaseName
}