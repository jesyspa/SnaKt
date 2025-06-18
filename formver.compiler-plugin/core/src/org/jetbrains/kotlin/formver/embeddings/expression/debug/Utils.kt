/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression.debug

import org.jetbrains.kotlin.formver.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.embeddings.LabelEmbedding
import org.jetbrains.kotlin.formver.embeddings.LabelLink
import org.jetbrains.kotlin.formver.embeddings.callables.NamedFunctionSignature
import org.jetbrains.kotlin.formver.embeddings.toLink
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.mangled

val LabelLink.debugTreeView: TreeView
    get() = PlaintextLeaf(name.mangled)
val LabelEmbedding.debugTreeView: TreeView
    get() = this.toLink().debugTreeView
val NamedFunctionSignature.nameAsDebugTreeView: TreeView
    get() = PlaintextLeaf(name.mangled)
val FieldEmbedding.debugTreeView: TreeView
    get() = NamedBranchingNode("Field", PlaintextLeaf(name.mangled))
val PermExp.debugTreeView: TreeView
    get() = when (this) {
        is PermExp.WildcardPerm -> PlaintextLeaf("wildcard")
        is PermExp.FullPerm -> PlaintextLeaf("write")
    }

fun TreeView.withDesignation(name: String) = designatedNode(name, this)
