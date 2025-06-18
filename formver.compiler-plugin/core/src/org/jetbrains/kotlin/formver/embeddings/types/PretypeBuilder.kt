/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.types

import org.jetbrains.kotlin.formver.names.ScopedKotlinName

/**
 * We use "pretype" to refer to types that do not contain information on nullability or
 * other flags.
 */
interface PretypeBuilder {
    /**
     * Turn the builder into a `TypeEmbedding`.
     *
     * We allow this deferral so that the build can be done in any order.
     */
    fun complete(): PretypeEmbedding
}

object UnitPretypeBuilder : PretypeBuilder {
    override fun complete() = UnitTypeEmbedding
}

object NothingPretypeBuilder : PretypeBuilder {
    override fun complete() = NothingTypeEmbedding
}

object AnyPretypeBuilder : PretypeBuilder {
    override fun complete() = AnyTypeEmbedding
}

object IntPretypeBuilder : PretypeBuilder {
    override fun complete() = IntTypeEmbedding
}

object BooleanPretypeBuilder : PretypeBuilder {
    override fun complete() = BooleanTypeEmbedding
}

object CharPretypeBuilder : PretypeBuilder {
    override fun complete() = CharTypeEmbedding
}

object StringPretypeBuilder : PretypeBuilder {
    override fun complete() = StringTypeEmbedding
}

class FunctionPretypeBuilder : PretypeBuilder {
    private val paramTypes = mutableListOf<TypeEmbedding>()
    private var extensionReceiverType: TypeEmbedding? = null
    private var dispatchReceiverType: TypeEmbedding? = null
    private var returnType: TypeEmbedding? = null
    var returnsUnique: Boolean = false

    fun withParam(paramInit: TypeBuilder.() -> PretypeBuilder) {
        paramTypes.add(buildType { paramInit() })
    }

    fun withDispatchReceiver(receiverInit: TypeBuilder.() -> PretypeBuilder) {
        require(dispatchReceiverType == null) { "Receiver already set" }
        dispatchReceiverType = buildType { receiverInit() }
    }

    fun withExtensionReceiver(receiverInit: TypeBuilder.() -> PretypeBuilder) {
        require(extensionReceiverType == null) { "Receiver already set" }
        extensionReceiverType = buildType { receiverInit() }
    }

    fun withReturnType(returnTypeInit: TypeBuilder.() -> PretypeBuilder) {
        require(returnType == null) { "Return type already set" }
        returnType = buildType { returnTypeInit() }
    }

    override fun complete(): FunctionTypeEmbedding {
        require(returnType != null) { "Return type not set" }
        return FunctionTypeEmbedding(dispatchReceiverType, extensionReceiverType, paramTypes, returnType!!, returnsUnique)
    }
}

fun buildFunctionPretype(init: FunctionPretypeBuilder.() -> Unit): FunctionTypeEmbedding =
    FunctionPretypeBuilder().apply(init).complete()

class ClassPretypeBuilder : PretypeBuilder {
    private var className: ScopedKotlinName? = null

    fun withName(name: ScopedKotlinName) {
        require(className == null) { "Class name already set" }
        className = name
    }

    override fun complete(): ClassTypeEmbedding {
        require(className != null) { "Class name not set" }
        return ClassTypeEmbedding(className!!)
    }
}

fun buildClassPretype(init: ClassPretypeBuilder.() -> Unit): ClassTypeEmbedding =
    ClassPretypeBuilder().apply(init).complete()

// TODO: ensure we can build the types with the builders, without hacks like this.
class ExistingPretypeBuilder(val embedding: PretypeEmbedding) : PretypeBuilder {
    override fun complete() = embedding
}
