/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.formver.core.names

import NameScope
import org.jetbrains.kotlin.formver.viper.Join
import org.jetbrains.kotlin.formver.viper.Lit
import org.jetbrains.kotlin.formver.viper.NameExpr
import org.jetbrains.kotlin.formver.viper.SEPARATOR
import org.jetbrains.kotlin.formver.viper.SymbolVal
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.parseRequiredScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse

val SymbolicName.fullScope: NameExpr?
    get() = mangledScope?.fullSymbolName
val SymbolicName.requiredScope: NameExpr?
    get() = fullScope?.let {parseRequiredScope(it)}
// Includes the scope itself.
val NameScope.parentScopes: Sequence<NameScope>
    get() = sequence {
        parent?.parentScopes?.let { yieldAll(it) }
        yield(this@parentScopes)
    }

val NameScope.allParentScopes: Sequence<NameScope>
    get() = sequence {
        parent?.parentScopes?.let { yieldAll(it) }
        yield(this@allParentScopes)
    }

val NameScope.fullSymbolName: Join?
    get() {
        val scopes = parentScopes.mapNotNull { it.mangledScopeName }.toList()
        return if (scopes.isEmpty()) null else Join(scopes, SEPARATOR)
    }

val NameScope.packageNameIfAny: FqName?
    get() = allParentScopes.filterIsInstance<PackageScope>().lastOrNull()?.packageName

val NameScope.classNameIfAny: ScopedKotlinName?
    get() = allParentScopes.filterIsInstance<ClassScope>().lastOrNull()?.className

data class PackageScope(val packageName: FqName) : NameScope {
    override val parent = null

    override val mangledScopeName: NameExpr?
        get() = packageName.isRoot.ifFalse { Lit("pkg\$${packageName.asViperString()}") }
}

data class ClassScope(val className: ScopedKotlinName) : NameScope {
    override val parent: NameScope? = className.scope
    override val mangledScopeName: NameExpr
        get() = SymbolVal(className)
}

/**
 * We do not want to mangle field names with class and package, hence introducing
 * this special `NameScope`. Note that it still needs package and class for other purposes.
 */
data class PublicScope(override val parent: NameScope) : NameScope {
    override val mangledScopeName: NameExpr
        get() = Lit("public")
}

data class PrivateScope(override val parent: NameScope) : NameScope {
    override val mangledScopeName: NameExpr
        get() = Lit("private")
}

data object ParameterScope : NameScope {
    override val parent: NameScope? = null

    override val mangledScopeName: NameExpr
        get() = Lit("p")
}

data object BadScope : NameScope {
    override val parent: NameScope? = null

    override val mangledScopeName: NameExpr
        get() = Lit("<BAD>")
}

data class LocalScope(val level: Int) : NameScope {
    override val parent: NameScope? = null

    override val mangledScopeName: NameExpr
        get() = Lit("l$level")
}

/**
 * Scope to use in cases when we need a scoped name, but don't actually want to introduce one.
 */
data object FakeScope : NameScope {
    override val parent: NameScope? = null

    override val mangledScopeName: NameExpr?
        get() = null
}
