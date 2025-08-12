/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain.Companion.intInjection
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain.Companion.stringInjection
import org.jetbrains.kotlin.formver.embeddings.types.buildFunctionPretype
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction

object OperatorExpEmbeddings {

    private val intIntToIntType
        get() = buildFunctionPretype {
            withParam { int() }
            withParam { int() }
            withReturnType { int() }
        }

    val AddIntInt: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("plusInts")
            setSignature(intIntToIntType)
            viperImplementation { Exp.Add(args[0], args[1], pos, info, trafos) }
        }

    val SubIntInt: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("minusInts")
            setSignature(intIntToIntType)
            viperImplementation { Exp.Sub(args[0], args[1], pos, info, trafos) }
        }

    val MulIntInt: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("timesInts")
            setSignature(intIntToIntType)
            viperImplementation { Exp.Mul(args[0], args[1], pos, info, trafos) }
        }


    val DivIntInt
        get() = buildBinaryOperator {
            setName("divInts")
            setSignature(intIntToIntType)
            viperImplementation { Exp.Div(args[0], args[1], pos, info, trafos) }
            additionalConditions {
                precondition {
                    intInjection.fromRef(args[1]) ne 0.toExp()
                }
            }
        }

    val RemIntInt
        get() = buildBinaryOperator {
            setName("remInts")
            setSignature(intIntToIntType)
            viperImplementation { Exp.Mod(args[0], args[1], pos, info, trafos) }
            additionalConditions {
                precondition {
                    intInjection.fromRef(args[1]) ne 0.toExp()
                }
            }
        }

    private val intIntToBooleanType
        get() = buildFunctionPretype {
            withParam { int() }
            withParam { int() }
            withReturnType { boolean() }
        }

    val LeIntInt: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("leInts")
            setSignature(intIntToBooleanType)
            viperImplementation { Exp.LeCmp(args[0], args[1], pos, info, trafos) }
        }

    val LtIntInt: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("ltInts")
            setSignature(intIntToBooleanType)
            viperImplementation { Exp.LtCmp(args[0], args[1], pos, info, trafos) }
        }

    val GeIntInt: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("geInts")
            setSignature(intIntToBooleanType)
            viperImplementation { Exp.GeCmp(args[0], args[1], pos, info, trafos) }
        }

    val GtIntInt: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("gtInts")
            setSignature(intIntToBooleanType)
            viperImplementation { Exp.GtCmp(args[0], args[1], pos, info, trafos) }
        }

    val Not: UnaryOperatorExpEmbeddingTemplate
        get() = buildUnaryOperator {
            setName("notBool")
            withSignature {
                withParam { boolean() }
                withReturnType { boolean() }
            }
            viperImplementation { Exp.Not(args[0], pos, info, trafos) }
        }

    private val booleanBooleanToBooleanType
        get() = buildFunctionPretype {
            withParam { boolean() }
            withParam { boolean() }
            withReturnType { boolean() }
        }

    val And: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("andBools")
            setSignature(booleanBooleanToBooleanType)
            viperImplementation { Exp.And(args[0], args[1], pos, info, trafos) }
        }

    val Or: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("orBools")
            setSignature(booleanBooleanToBooleanType)
            viperImplementation { Exp.Or(args[0], args[1], pos, info, trafos) }
        }

    val Implies: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("impliesBools")
            setSignature(booleanBooleanToBooleanType)
            viperImplementation { Exp.Implies(args[0], args[1], pos, info, trafos) }
        }

    val SubCharChar: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("subChars")
            withSignature {
                withParam { char() }
                withParam { char() }
                withReturnType { int() }
            }
            viperImplementation { Exp.Sub(args[0], args[1], pos, info, trafos) }
        }

    private val charIntToCharType = buildFunctionPretype {
        withParam { char() }
        withParam { int() }
        withReturnType { char() }
    }

    val AddCharInt: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("addCharInt")
            setSignature(charIntToCharType)
            viperImplementation { Exp.Add(args[0], args[1], pos, info, trafos) }
        }

    val SubCharInt: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("subCharInt")
            setSignature(charIntToCharType)
            viperImplementation { Exp.Sub(args[0], args[1], pos, info, trafos) }
        }

    private val charCharToBooleanType = buildFunctionPretype {
        withParam { char() }
        withParam { char() }
        withReturnType { boolean() }
    }

    val GeCharChar: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("geChars")
            setSignature(charCharToBooleanType)
            viperImplementation { Exp.GeCmp(args[0], args[1], pos, info, trafos) }
        }

    val GtCharChar: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("gtChars")
            setSignature(charCharToBooleanType)
            viperImplementation { Exp.GtCmp(args[0], args[1], pos, info, trafos) }
        }

    val LeCharChar: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("leChars")
            setSignature(charCharToBooleanType)
            viperImplementation { Exp.LeCmp(args[0], args[1], pos, info, trafos) }
        }

    val LtCharChar: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("ltChars")
            setSignature(charCharToBooleanType)
            viperImplementation { Exp.LtCmp(args[0], args[1], pos, info, trafos) }
        }
    val StringLength: UnaryOperatorExpEmbeddingTemplate
        get() = buildUnaryOperator {
            setName("stringLength")
            withSignature {
                withParam { string() }
                withReturnType { int() }
            }
            viperImplementation { Exp.SeqLength(args[0], pos, info, trafos) }
        }


    val StringGet: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("stringGet")
            withSignature {
                withParam { string() }
                withParam { int() }
                withReturnType { char() }
            }
            viperImplementation { Exp.SeqIndex(args[0], args[1], pos, info, trafos) }
            additionalConditions {
                precondition {
                    listOf(
                        intInjection.fromRef(args[1]) ge 0.toExp(),
                        intInjection.fromRef(args[1]) lt Exp.SeqLength(stringInjection.fromRef(args[0]))
                    ).toConjunction()
                }
            }
        }

    val AddStringString: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("addStrings")
            withSignature {
                withParam { string() }
                withParam { string() }
                withReturnType { string() }
            }
            viperImplementation { Exp.SeqAppend(args[0], args[1], pos, info, trafos) }
        }

    val AddStringChar: BinaryOperatorExpEmbeddingTemplate
        get() = buildBinaryOperator {
            setName("addStringChar")
            withSignature {
                withParam { string() }
                withParam { char() }
                withReturnType { string() }
            }
            viperImplementation { Exp.SeqAppend(args[0], Exp.ExplicitSeq(listOf(args[1])), pos, info, trafos) }
        }

    val allTemplates
        get() = listOf(
            AddIntInt, SubIntInt, MulIntInt, DivIntInt, RemIntInt,
            LeIntInt, GeIntInt, LtIntInt, GtIntInt,
            Not, And, Or, Implies,
            AddCharInt, SubCharChar, SubCharInt,
            LeCharChar, GeCharChar, LtCharChar, GtCharChar,
            StringLength, StringGet, AddStringString, AddStringChar,
        )
}
