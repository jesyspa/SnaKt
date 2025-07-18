/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.asTypeEmbedding
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Name corresponding to an entity in the original Kotlin code.
 *
 * This is a little more general than the `Name` type in Kotlin: we also use this
 * to represent getters and setters, for example.
 */
sealed interface KotlinName : MangledName

data class SimpleKotlinName(val name: Name) : KotlinName {
    override val mangledBaseName: String = name.asStringStripSpecialMarkers()
}

abstract class TypedKotlinName(override val mangledType: String, name: Name) : KotlinName {
    override val mangledBaseName: String = name.asStringStripSpecialMarkers()
}

abstract class TypedKotlinNameWithType(override val mangledType: String, name: Name, type: TypeEmbedding) : KotlinName {
    override val mangledBaseName: String = "${name.asStringStripSpecialMarkers()}\$${type.name.mangled}"
}

data class FunctionKotlinName(val name: Name, val type: FunctionTypeEmbedding) : TypedKotlinNameWithType(
    "f", name,
    type.asTypeEmbedding()
)

/**
 * This name will never occur in the viper output, but rather is used to lookup properties.
 */
data class PropertyKotlinName(val name: Name) : TypedKotlinName("pp", name)
data class BackingFieldKotlinName(val name: Name) : TypedKotlinName("bf", name)
data class GetterKotlinName(val name: Name) : TypedKotlinName("pg", name)
data class SetterKotlinName(val name: Name) : TypedKotlinName("ps", name)
data class ExtensionSetterKotlinName(val name: Name, val type: FunctionTypeEmbedding) :
    TypedKotlinNameWithType("es", name, type.asTypeEmbedding())

data class ExtensionGetterKotlinName(val name: Name, val type: FunctionTypeEmbedding) :
    TypedKotlinNameWithType("eg", name, type.asTypeEmbedding())

data class ClassKotlinName(val name: FqName) : KotlinName {
    override val mangledType: String
        get() = "c"
    override val mangledBaseName: String = name.asViperString()

    constructor(classSegments: List<String>) : this(FqName.fromSegments(classSegments))
}

data class ConstructorKotlinName(val type: FunctionTypeEmbedding) : KotlinName {
    override val mangledType: String
        get() = "con"
    override val mangledBaseName: String
        get() = type.name.mangledBaseName
}

// It's a bit of a hack to make this as KotlinName, it should really just be any old name, but right now our scoped
// names are KotlinNames and changing that could be messy.
data class PredicateKotlinName(override val mangledBaseName: String) : KotlinName {
    override val mangledType: String
        get() = "p"
}

data class PretypeName(override val mangledBaseName: String) : KotlinName

data class TypeName(val pretype: PretypeEmbedding, val nullable: Boolean) : KotlinName {
    override val mangledBaseName: String
        get() = pretype.name.mangledBaseName
    override val mangledType: String
        get() = listOfNotNull(
            if (nullable) "N" else null,
            "T",
            if (pretype is FunctionTypeEmbedding) "F" else null
        ).joinToString("")
}