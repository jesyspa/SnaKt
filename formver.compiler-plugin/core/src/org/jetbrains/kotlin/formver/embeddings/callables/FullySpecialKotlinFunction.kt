/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.AddCharInt
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.AddIntInt
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.DivIntInt
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.Implies
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.MulIntInt
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.Not
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.StringGet
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.SubCharChar
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.SubCharInt
import org.jetbrains.kotlin.formver.embeddings.expression.OperatorExpEmbeddings.SubIntInt
import org.jetbrains.kotlin.formver.embeddings.types.buildFunctionPretype
import org.jetbrains.kotlin.formver.embeddings.types.nullableAny
import org.jetbrains.kotlin.formver.names.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


val SpecialKotlinFunction.callableId: CallableId
    get() = CallableId(FqName.fromSegments(packageName), className?.let { FqName(it) }, Name.identifier(name))

fun SpecialKotlinFunction.embedName(): ScopedKotlinName = callableId.embedFunctionName(callableType)

/**
 * We store here all the __Kotlin__ functions that need a (fully) special `ExpEmbedding`.
 * `byName` is stateless - it always stores the same Kotlin functions
 * and corresponding embeddings.
 */
object SpecialKotlinFunctions {
    private val contractBuilderTypeName = buildName {
        packageScope(SpecialPackages.contracts)
        ClassKotlinName(listOf("ContractBuilder"))
    }
    private val booleanArrayTypeName = buildName {
        packageScope(SpecialPackages.kotlin)
        ClassKotlinName(listOf("BooleanArray"))
    }
    private val invariantBuilderTypeName = buildName {
        packageScope(SpecialPackages.formver)
        ClassKotlinName(listOf("InvariantBuilder"))
    }

    val byName: Map<MangledName, FunctionEmbedding> = buildFullySpecialFunctions {
        val intIntToIntType = buildFunctionPretype {
            withDispatchReceiver { int() }
            withParam { int() }
            withReturnType { int() }
        }
        withCallableType(intIntToIntType) {
            addFunction(SpecialPackages.kotlin, className = "Int", name = "plus") { args, _ ->
                AddIntInt(args[0], args[1])
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "minus") { args, _ ->
                SubIntInt(args[0], args[1])
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "times") { args, _ ->
                MulIntInt(args[0], args[1])
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "div") { args, _ ->
                DivIntInt(args[0], args[1])
            }
        }

        val intToIntType = buildFunctionPretype {
            withDispatchReceiver { int() }
            withReturnType { int() }
        }

        withCallableType(intToIntType) {
            addFunction(SpecialPackages.kotlin, className = "Int", name = "inc") { args, _ ->
                AddIntInt(args[0], IntLit(1))
            }
            addFunction(SpecialPackages.kotlin, className = "Int", name = "dec") { args, _ ->
                SubIntInt(args[0], IntLit(1))
            }
        }

        val booleanToBooleanType = buildFunctionPretype {
            withDispatchReceiver { boolean() }
            withReturnType { boolean() }
        }

        addFunction(booleanToBooleanType, SpecialPackages.kotlin, className = "Boolean", name = "not") { args, _ ->
            Not(args[0])
        }

        val extBooleanBooleanToBooleanType = buildFunctionPretype {
            withExtensionReceiver { boolean() }
            withParam { boolean() }
            withReturnType { boolean() }
        }

        addFunction(extBooleanBooleanToBooleanType, SpecialPackages.formver, name = "implies") { args, _ ->
            Implies(args[0], args[1])
        }

        val verifyCallableType = buildFunctionPretype {
            withParam {
                klass {
                    withName(booleanArrayTypeName)
                }
            }
            withReturnType { unit() }
        }
        addFunction(verifyCallableType, SpecialPackages.formver, name = "verify") { args, _ ->
            args.map { Assert(it) }.toBlock()
        }

        val invariantsBuilderCallableType = buildFunctionPretype {
            withParam {
                function {
                    withDispatchReceiver {
                        klass {
                            withName(invariantBuilderTypeName)
                        }
                    }
                    withReturnType { unit() }
                }
            }
            withReturnType { unit() }
        }
        withCallableType(invariantsBuilderCallableType) {
            addNoOpFunction(SpecialPackages.formver, name = "loopInvariants")
            addNoOpFunction(SpecialPackages.formver, name = "preconditions")
        }

        val postconditionsBuilderCallableType = buildFunctionPretype {
            withParam {
                function {
                    withDispatchReceiver {
                        klass {
                            withName(invariantBuilderTypeName)
                        }
                    }
                    withParam { nullableAny() }
                    withReturnType { unit() }
                }
            }
            withReturnType { unit() }
        }

        withCallableType(postconditionsBuilderCallableType) {
            addNoOpFunction(SpecialPackages.formver, name = "postconditions")
        }

        val contractCallableType = buildFunctionPretype {
            withParam {
                function {
                    withDispatchReceiver {
                        klass {
                            withName(contractBuilderTypeName)
                        }
                    }
                    withReturnType { unit() }
                }
            }
            withReturnType { unit() }
        }

        addFunction(contractCallableType, SpecialPackages.contracts, name = "contract") { _, _ ->
            UnitLit
        }

        val charCharToIntType = buildFunctionPretype {
            withDispatchReceiver { char() }
            withParam { char() }
            withReturnType { int() }
        }

        addFunction(charCharToIntType, SpecialPackages.kotlin, className = "Char", name = "minus") { args, _ ->
            SubCharChar(args[0], args[1])
        }

        val charIntToCharType = buildFunctionPretype {
            withDispatchReceiver { char() }
            withParam { int() }
            withReturnType { char() }
        }

        withCallableType(charIntToCharType) {
            addFunction(SpecialPackages.kotlin, className = "Char", name = "plus") { args, _ ->
                AddCharInt(args[0], args[1])
            }
            addFunction(SpecialPackages.kotlin, className = "Char", name = "minus") { args, _ ->
                SubCharInt(args[0], args[1])
            }
        }

        val stringIntToCharType = buildFunctionPretype {
            withDispatchReceiver { string() }
            withParam { int() }
            withReturnType { char() }
        }

        addFunction(stringIntToCharType, SpecialPackages.kotlin, className = "String", name = "get") { args, _ ->
            StringGet(args[0], args[1])
        }
    }
}

val FunctionEmbedding.isVerifyFunction: Boolean
    get() = isFormverPluginFunctionNamed(name = "verify")

fun FunctionEmbedding.isFormverPluginFunctionNamed(className: String? = null, name: String): Boolean =
    this is FullySpecialKotlinFunction && NameMatcher.matchClassScope(this.embedName()) {
        ifPackageName(SpecialPackages.formver) {
            if (className == null) {
                ifNoReceiver {
                    ifFunctionName(name) {
                        return true
                    }
                }
            } else {
                ifClassName(className) {
                    ifFunctionName(name) {
                        return true
                    }
                }
            }
        }
        return false
    }
