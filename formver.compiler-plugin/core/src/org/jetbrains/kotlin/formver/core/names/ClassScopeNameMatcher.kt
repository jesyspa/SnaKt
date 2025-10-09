/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal sealed class NameMatcher(val name: SymbolicName) {
    companion object {
        inline fun matchClassScope(name: SymbolicName, action: ClassScopeNameMatcher.() -> Nothing): Nothing {
            ClassScopeNameMatcher(name).action()
        }

        inline fun matchGlobalScope(name: SymbolicName, action: GlobalScopeNameMatcher.() -> Nothing): Nothing {
            GlobalScopeNameMatcher(name).action()
        }
    }

    protected val scopedName = name as? ScopedKotlinName
    protected val packageName = scopedName?.scope?.packageNameIfAny
    protected abstract val className: ScopedKotlinName?

    inline fun ifPackageName(matched: List<String>, action: NameMatcher.() -> Unit) {
        if (packageName == FqName.fromSegments(matched))
            this.action()
    }

    inline fun ifInCollectionsPkg(action: NameMatcher.() -> Unit) {
        ifPackageName(SpecialPackages.collections) { this.action() }
    }

    inline fun ifClassName(vararg segments: String, action: NameMatcher.() -> Unit) {
        if (className?.name == ClassKotlinName(segments.toList()))
            this.action()
    }

    inline fun ifIsCollectionInterface(action: NameMatcher.() -> Unit) {
        ifInCollectionsPkg {
            ifClassName("Collection") {
                this.action()
            }
        }
    }

}

internal class ClassScopeNameMatcher(name: SymbolicName) : NameMatcher(name) {
    override val className = if (scopedName?.name is ClassKotlinName) scopedName else null

    inline fun ifNoReceiver(action: NameMatcher.() -> Unit) {
        if (className == null)
            action()
    }

    inline fun ifFunctionName(name: String, action: ClassScopeNameMatcher.() -> Unit) {
        val functionName = scopedName?.name as? FunctionKotlinName
        if (functionName?.name == Name.identifier(name))
            this.action()
    }

    inline fun ifBackingFieldName(name: String, action: ClassScopeNameMatcher.() -> Unit) {
        if (scopedName?.name == BackingFieldKotlinName(Name.identifier(name)))
            this.action()
    }
}

internal class GlobalScopeNameMatcher(name: SymbolicName) : NameMatcher(name) {
    override val className = scopedName
}