/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.embeddings.expression.debug

import org.jetbrains.kotlin.formver.core.embeddings.LabelEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.LabelLink
import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.toLink
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.mangled

context(nameResolver: NameResolver)
val LabelLink.debugTreeView: TreeView
    get() = PlaintextLeaf(name.mangled)

context(nameResolver: NameResolver)
val LabelEmbedding.debugTreeView: TreeView
    get() = this.toLink().debugTreeView

context(nameResolver: NameResolver)
val NamedFunctionSignature.nameAsDebugTreeView: TreeView
    get() = PlaintextLeaf(name.mangled)

context(nameResolver: NameResolver)
val FieldEmbedding.debugTreeView: TreeView
    get() = NamedBranchingNode("Field", PlaintextLeaf(name.mangled))
val PermExp.debugTreeView: TreeView
    get() = when (this) {
        is PermExp.WildcardPerm -> PlaintextLeaf("wildcard")
        is PermExp.FullPerm -> PlaintextLeaf("write")
        is PermExp.EpsilonPerm -> PlaintextLeaf("read")
    }

fun TreeView.withDesignation(name: String) = designatedNode(name, this)
