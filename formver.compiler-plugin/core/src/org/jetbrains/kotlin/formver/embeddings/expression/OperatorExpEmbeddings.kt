/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain.Companion.intInjection
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain.Companion.stringInjection
import org.jetbrains.kotlin.formver.embeddings.types.buildFunctionPretype
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.toConjunction

object OperatorExpEmbeddings {

    private val intIntToIntType
        get() = buildFunctionPretype {
            withParam { int() }
            withParam { int() }
            withReturnType { int() }
        }

    val AddIntInt: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("plusInts")
            setSignature(intIntToIntType)
            viperImplementation { Exp.Add(args[0], args[1], pos, info, trafos) }
        }

    val SubIntInt: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("minusInts")
            setSignature(intIntToIntType)
            viperImplementation { Exp.Sub(args[0], args[1], pos, info, trafos) }
        }

    val MulIntInt: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("timesInts")
            setSignature(intIntToIntType)
            viperImplementation { Exp.Mul(args[0], args[1], pos, info, trafos) }
        }


    val DivIntInt = buildBinaryOperator {
            setName("divInts")
            setSignature(intIntToIntType)
            viperImplementation { Exp.Div(args[0], args[1], pos, info, trafos) }
            additionalConditions {
                precondition {
                    intInjection.fromRef(args[1]) ne 0.toExp()
                }
            }
        }

    val RemIntInt = buildBinaryOperator {
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

    val LeIntInt: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("leInts")
            setSignature(intIntToBooleanType)
            viperImplementation { Exp.LeCmp(args[0], args[1], pos, info, trafos) }
        }

    val LtIntInt: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("ltInts")
            setSignature(intIntToBooleanType)
            viperImplementation { Exp.LtCmp(args[0], args[1], pos, info, trafos) }
        }

    val GeIntInt: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("geInts")
            setSignature(intIntToBooleanType)
            viperImplementation { Exp.GeCmp(args[0], args[1], pos, info, trafos) }
        }

    val GtIntInt: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("gtInts")
            setSignature(intIntToBooleanType)
            viperImplementation { Exp.GtCmp(args[0], args[1], pos, info, trafos) }
        }

    val Not: UnaryOperatorExpEmbeddingTemplate = buildUnaryOperator {
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

    val And: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("andBools")
            setSignature(booleanBooleanToBooleanType)
            viperImplementation { Exp.And(args[0], args[1], pos, info, trafos) }
        }

    val Or: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("orBools")
            setSignature(booleanBooleanToBooleanType)
            viperImplementation { Exp.Or(args[0], args[1], pos, info, trafos) }
        }

    val Implies: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("impliesBools")
            setSignature(booleanBooleanToBooleanType)
            viperImplementation { Exp.Implies(args[0], args[1], pos, info, trafos) }
        }

    val SubCharChar: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
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

    val AddCharInt: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("addCharInt")
            setSignature(charIntToCharType)
            viperImplementation { Exp.Add(args[0], args[1], pos, info, trafos) }
        }

    val SubCharInt: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("subCharInt")
            setSignature(charIntToCharType)
            viperImplementation { Exp.Sub(args[0], args[1], pos, info, trafos) }
        }

    private val charCharToBooleanType = buildFunctionPretype {
        withParam { char() }
        withParam { char() }
        withReturnType { boolean() }
    }

    val GeCharChar: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("geChars")
            setSignature(charCharToBooleanType)
            viperImplementation { Exp.GeCmp(args[0], args[1], pos, info, trafos) }
        }

    val GtCharChar: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("gtChars")
            setSignature(charCharToBooleanType)
            viperImplementation { Exp.GtCmp(args[0], args[1], pos, info, trafos) }
        }

    val LeCharChar: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("leChars")
            setSignature(charCharToBooleanType)
            viperImplementation { Exp.LeCmp(args[0], args[1], pos, info, trafos) }
        }

    val LtCharChar: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("ltChars")
            setSignature(charCharToBooleanType)
            viperImplementation { Exp.LtCmp(args[0], args[1], pos, info, trafos) }
        }
    val StringLength: UnaryOperatorExpEmbeddingTemplate = buildUnaryOperator {
            setName("stringLength")
            withSignature {
                withParam { string() }
                withReturnType { int() }
            }
            viperImplementation { Exp.SeqLength(args[0], pos, info, trafos) }
        }


    val StringGet: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
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

    val AddStringString: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
            setName("addStrings")
            withSignature {
                withParam { string() }
                withParam { string() }
                withReturnType { string() }
            }
            viperImplementation { Exp.SeqAppend(args[0], args[1], pos, info, trafos) }
        }

    val AddStringChar: BinaryOperatorExpEmbeddingTemplate = buildBinaryOperator {
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
