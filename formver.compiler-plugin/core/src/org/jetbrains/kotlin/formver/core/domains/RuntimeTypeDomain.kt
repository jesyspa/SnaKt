/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.domains

import org.jetbrains.kotlin.formver.core.conversion.TypeResolver
import org.jetbrains.kotlin.formver.core.embeddings.types.embedClassTypeFunc
import org.jetbrains.kotlin.formver.core.names.DomainName
import org.jetbrains.kotlin.formver.core.names.QualifiedDomainFuncName
import org.jetbrains.kotlin.formver.core.embeddings.types.CharTypeEmbedding
import org.jetbrains.kotlin.formver.core.names.UnqualifiedDomainFuncName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*


const val RUNTIME_TYPE_DOMAIN_NAME = "rt"


/**
 * This new domain is designed to replace `NullableDomain`, `TypeDomain` and `CastingDomain` and it is not yet integrated.
 * To enable its generation in viper output uncomment corresponding lines in
 * [ProgramConverter](jetbrains://idea/navigate/reference?project=kotlin&path=org/jetbrains/kotlin/formver/conversion/ProgramConverter.kt:70)
 * and [SpecialFunctions.kt](jetbrains://idea/navigate/reference?project=kotlin&path=org/jetbrains/kotlin/formver/embeddings/callables/SpecialFunctions.kt:58)
 *
 * Viper code:
 * ```viper
 *
 * domain RuntimeType  {
 *
 *
 *  unique function intType(): RuntimeType
 *  unique function boolType(): RuntimeType
 *  unique function unitType(): RuntimeType
 *  unique function nothingType(): RuntimeType
 *  unique function anyType(): RuntimeType
 *  unique function functionType(): RuntimeType
 *
 *  // unique *Type() : RuntimeType for each user type
 *
 *  function nullValue(): Ref
 *  function unitValue(): Ref
 *
 *  function isSubtype(t1: RuntimeType, t2: RuntimeType): Bool
 *  function typeOf(r: Ref): RuntimeType
 *  function nullable(t: RuntimeType): RuntimeType
 *
 *
 *  function intToRef(v: Int): Ref
 *  function intFromRef(r: Ref): Int
 *  function boolToRef(v: Bool): Ref
 *  function boolFromRef(r: Ref): Bool
 *
 *
 *  axiom subtype_reflexive {
 *    (forall t: RuntimeType ::isSubtype(t, t))
 *  }
 *
 *  axiom subtype_transitive {
 *    (forall t1: RuntimeType, t2: RuntimeType, t3: RuntimeType ::
 *      { isSubtype(t1, t2), isSubtype(t2, t3) }
 *      isSubtype(t1, t2) &&
 *      isSubtype(t2, t3) ==>
 *      isSubtype(t1, t3))
 *  }
 *
 *  axiom subtype_antisymmetric {
 *    (forall t1: RuntimeType, t2: RuntimeType ::
 *      { isSubtype(t1, t2), isSubtype(t2, t1) }
 *      isSubtype(t1, t2) &&
 *      isSubtype(t2, t1) ==>
 *      t1 == t2)
 *  }
 *
 *  axiom nullable_idempotent {
 *    (forall t: RuntimeType ::
 *      { nullable(nullable(t)) }
 *      nullable(nullable(t)) ==
 *      nullable(t))
 *  }
 *
 *  axiom nullable_supertype {
 *    (forall t: RuntimeType ::
 *      { nullable(t) }
 *      isSubtype(t, nullable(t)))
 *  }
 *
 *  axiom nullable_preserves_subtype {
 *    (forall t1: RuntimeType, t2: RuntimeType ::
 *      { isSubtype(nullable(t1), nullable(t2)) }
 *      isSubtype(t1, t2) ==>
 *      isSubtype(nullable(t1), nullable(t2)))
 *  }
 *
 *  axiom nullable_any_supertype {
 *    (forall t: RuntimeType ::isSubtype(t, nullable(anyType())))
 *  }
 *
 *  axiom {
 *    isSubtype(intType(), anyType())
 *  }
 *
 *  axiom {
 *    isSubtype(boolType(), anyType())
 *  }
 *
 *  axiom {
 *    isSubtype(unitType(), anyType())
 *  }
 *
 *  axiom {
 *    isSubtype(nothingType(), anyType())
 *  }
 *
 *  axiom {
 *    isSubtype(anyType(), anyType())
 *  }
 *
 *  axiom {
 *    isSubtype(functionType(), anyType())
 *  }
 *
 *  // isSubtype(*Type(), anyType()) for each user type
 *
 *  axiom supertype_of_nullable_nothing {
 *    (forall t: RuntimeType ::isSubtype(nullable(nothingType()),
 *      t))
 *  }
 *
 *  axiom any_not_nullable {
 *    (forall t: RuntimeType ::!isSubtype(nullable(t),
 *      anyType()))
 *  }
 *
 *  axiom null_smartcast_value_level {
 *    (forall r: Ref, t: RuntimeType ::
 *      { isSubtype(typeOf(r), nullable(t)) }
 *      isSubtype(typeOf(r), nullable(t)) ==>
 *      r == nullValue() ||
 *      isSubtype(typeOf(r), t))
 *  }
 *
 *  axiom nothing_empty {
 *    (forall r: Ref ::!isSubtype(typeOf(r), nothingType()))
 *  }
 *
 *  axiom null_smartcast_type_level {
 *    (forall t1: RuntimeType, t2: RuntimeType ::
 *      { isSubtype(t1, anyType()), isSubtype(t1,
 *      nullable(t2)) }
 *      isSubtype(t1, anyType()) &&
 *      isSubtype(t1, nullable(t2)) ==>
 *      isSubtype(t1, t2))
 *  }
 *
 *  axiom type_of_null {
 *    isSubtype(typeOf(nullValue()),
 *    nullable(nothingType()))
 *  }
 *
 *  axiom type_of_unit {
 *    isSubtype(typeOf(unitValue()),
 *    unitType())
 *  }
 *
 *  axiom uniqueness_of_unit {
 *    (forall r: Ref ::
 *      { isSubtype(typeOf(r), unitType()) }
 *      isSubtype(typeOf(r), unitType()) ==>
 *      r == unitValue())
 *  }
 *
 *  axiom {
 *    (forall v: Int ::
 *      { isSubtype(typeOf(intToRef(v)),
 *      intType()) }
 *      isSubtype(typeOf(intToRef(v)),
 *      intType()))
 *  }
 *
 *  axiom {
 *    (forall v: Int ::
 *      { intFromRef(intToRef(v)) }
 *      intFromRef(intToRef(v)) == v)
 *  }
 *
 *  axiom {
 *    (forall r: Ref ::
 *      { intToRef(intFromRef(r)) }
 *      isSubtype(typeOf(r), intType()) ==>
 *      intToRef(intFromRef(r)) == r)
 *  }
 *
 *  // same for bool2ref and ref2bool
 *
 *  // isSubtype(*Type(), *Type()) for each pair of user type and its supertype()
 * }
 *
 * function addInts(arg1: Ref, arg2: Ref): Ref
 *   requires isSubtype(typeOf(arg1), intType())
 *   requires isSubtype(typeOf(arg2), intType())
 *   ensures isSubtype(typeOf(result), intType())
 *   ensures intFromRef(result) == intFromRef(arg1) + intFromRef(arg2)
 * {
 *   intToRef(intFromRef(arg1) + intFromRef(arg2))
 * }
 *
 * // same for subtraction, multiplication and so on
 * ```
 */
class RuntimeTypeDomain(typeResolver: TypeResolver) : BuiltinDomain(DomainName(RUNTIME_TYPE_DOMAIN_NAME)) {
    override val typeVars: List<Type.TypeVar> = emptyList()

    // Define types that are not dependent on the user defined classes in a companion object.
    // That way other classes can refer to them without having an explicit reference to the concrete TypeDomain.
    companion object {
        private val domainName = DomainName(RUNTIME_TYPE_DOMAIN_NAME)
        val RuntimeType: Type.Domain = Type.Domain(domainName, emptyList())
        val Ref = Type.Ref

        fun createDomainFunc(
            funcName: SymbolicName, args: List<Declaration.LocalVarDecl>, type: Type, unique: Boolean = false
        ) = DomainFunc(
            QualifiedDomainFuncName(domainName, funcName), domainName, args, emptyList(), type, unique
        )

        private fun createNewTypeDomainFunc(funcName: SymbolicName) = createDomainFunc(
            funcName,
            emptyList(),
            RuntimeType,
            true,
        )

        private fun createNewTypeDomainFunc(funcName: String) = createNewTypeDomainFunc(
            UnqualifiedDomainFuncName(funcName)
        )
        // variables for readability improving

        private val t = domainVar("t", RuntimeType)
        private val t1 = domainVar("t1", RuntimeType)
        private val t2 = domainVar("t2", RuntimeType)
        private val t3 = domainVar("t3", RuntimeType)

        private val r = domainVar("r", Ref)
        private val codePoint = domainVar("codePoint", Type.Int)
        private val index = domainVar("index", Type.Int)

        // three basic functions
        /** `isSubtype: (Type, Type) -> Bool` */
        //val isSubtype: DomainFunc = createDomainFunc(SimpleKotlinName(Name.identifier("isSubtype")), listOf(t1.decl(), t2.decl()), Type.Bool)
        val isSubtype: DomainFunc =
            createDomainFunc(UnqualifiedDomainFuncName("isSubtype"), listOf(t1.decl(), t2.decl()), Type.Bool)
        infix fun Exp.subtype(otherType: Exp) = isSubtype(this, otherType)
        /** `typeOf: Ref -> Type` */
        val typeOf: DomainFunc = createDomainFunc(UnqualifiedDomainFuncName("typeOf"), listOf(r.decl()), RuntimeType)
        /** `nullable: Type -> Type` */
        val nullable: DomainFunc =
            createDomainFunc(UnqualifiedDomainFuncName("nullable"), listOf(t.decl()), RuntimeType)
        // many axioms will use `is` which can be represented as composition of `isSubtype` and `typeOf`
        /** `is: (Ref, Type) -> Bool` */
        infix fun Exp.isOf(elemType: Exp) = isSubtype(typeOf(this), elemType)

        // built-in types function
        val charType: DomainFunc = createNewTypeDomainFunc("charType")
        val intType: DomainFunc = createNewTypeDomainFunc("intType")
        val boolType: DomainFunc = createNewTypeDomainFunc("boolType")
        val unitType: DomainFunc = createNewTypeDomainFunc("unitType")
        val stringType: DomainFunc = createNewTypeDomainFunc("stringType")
        val nothingType: DomainFunc = createNewTypeDomainFunc("nothingType")
        val anyType: DomainFunc = createNewTypeDomainFunc("anyType")
        val functionType: DomainFunc = createNewTypeDomainFunc("functionType")

        // for creation of user types
        fun classTypeFunc(name: SymbolicName) = createDomainFunc(name, emptyList(), RuntimeType, true)

        // bijections to primitive types
        val intInjection = Injection(UnqualifiedDomainFuncName("int"), Type.Int, intType)
        val boolInjection = Injection(UnqualifiedDomainFuncName("bool"), Type.Bool, boolType)
        val charInjection = Injection(UnqualifiedDomainFuncName("char"), Type.Int, charType)
        val stringInjection = Injection(UnqualifiedDomainFuncName("string"), Type.Seq(Type.Int), stringType)
        val primitiveTypeInjections = listOf(intInjection, boolInjection, charInjection, stringInjection)
        /**
         * `truncateToChar: Int -> Int`
         *
         * Reduction of an arbitrary integer to the code range of a Kotlin `Char`. A `Char` is
         * embedded as an unbounded `Int`, so operations producing one have to say which value in
         * the code range they produce; naming that reduction keeps it out of the expressions
         * themselves, which matters because Viper does not accept arithmetic in a trigger.
         */
        val truncateToChar: DomainFunc =
            createDomainFunc(UnqualifiedDomainFuncName("truncateToChar"), listOf(codePoint.decl()), Type.Int)

        private val charCodeRangeSize = Exp.IntLit(CharTypeEmbedding.CODE_RANGE_SIZE)

        // special values
        val nullValue = createDomainFunc(UnqualifiedDomainFuncName("nullValue"), emptyList(), Ref)
        val unitValue = createDomainFunc(UnqualifiedDomainFuncName("unitValue"), emptyList(), Ref)
    }

    private val allInjections: List<Injection> = primitiveTypeInjections
    val builtinTypes: List<DomainFunc> =
        listOf(intType, boolType, charType, unitType, nothingType, anyType, functionType, stringType)
    private val userTypes: List<DomainFunc> =
        typeResolver.classTypeEmbeddings().map { it.embedClassTypeFunc() }
    val nonNullableTypes: List<DomainFunc> = (builtinTypes + userTypes).distinctBy { it.name }
    override val functions: List<DomainFunc> = nonNullableTypes + listOf(
        nullValue, unitValue, isSubtype, typeOf, nullable, truncateToChar
    ) + allInjections.flatMap { listOf(it.toRef, it.fromRef) }
    override val axioms: List<DomainAxiom> = AxiomListBuilder.build(this) {
        axiom("truncateToCharInCodeRange") {
            Exp.forall(codePoint) { code ->
                val truncated = simpleTrigger { truncateToChar.toFuncApp(listOf(code)) }
                Exp.And(Exp.LeCmp(Exp.IntLit(0), truncated), Exp.LtCmp(truncated, charCodeRangeSize))
            }
        }
        // A value already in the code range is its own truncation. This follows from the
        // remainder characterization below, but reaching it that way costs a nonlinear step;
        // here the antecedent is linear, which is the form a contract states it in.
        axiom("truncateToCharIdentityInCodeRange") {
            Exp.forall(codePoint) { code ->
                val truncated = simpleTrigger { truncateToChar.toFuncApp(listOf(code)) }
                assumption { Exp.LeCmp(Exp.IntLit(0), code) }
                assumption { Exp.LtCmp(code, charCodeRangeSize) }
                Exp.EqCmp(truncated, code)
            }
        }
        // The reduction is the non-negative remainder, which is what truncating to the low 16 bits
        // comes to. Viper's `%` takes the sign of its left operand, hence the second reduction.
        axiom("truncateToCharIsNonNegativeRemainder") {
            Exp.forall(codePoint) { code ->
                Exp.EqCmp(
                    simpleTrigger { truncateToChar.toFuncApp(listOf(code)) },
                    Exp.Mod(Exp.Add(Exp.Mod(code, charCodeRangeSize), charCodeRangeSize), charCodeRangeSize)
                )
            }
        }
        // A string is embedded as a `Seq[Int]`, whose elements Viper knows nothing about. Only a
        // real Kotlin string is in the image of the injection, and its elements are chars, so an
        // element read out of one is in the code range without any reduction being applied to it.
        axiom("stringElementInCodeRange") {
            Exp.forall(r, index) { r, index ->
                assumption { r isOf stringType() }
                assumption { Exp.LeCmp(Exp.IntLit(0), index) }
                assumption { Exp.LtCmp(index, Exp.SeqLength(stringInjection.fromRef(r))) }
                val element = simpleTrigger { Exp.SeqIndex(stringInjection.fromRef(r), index) }
                Exp.And(Exp.LeCmp(Exp.IntLit(0), element), Exp.LtCmp(element, charCodeRangeSize))
            }
        }
        axiom("subtypeReflexive") {
            Exp.forall(t) { t -> t subtype t }
        }
        axiom("subtypeTransitive") {
            Exp.forall(t1, t2, t3) { t1, t2, t3 ->
                assumption {
                    compoundTrigger {
                        subTrigger { t1 subtype t2 }
                        subTrigger { t2 subtype t3 }
                    }
                }
                compoundTrigger {
                    subTrigger { t1 subtype t2 }
                    subTrigger { t1 subtype t3 }
                }
                compoundTrigger {
                    subTrigger { t2 subtype t3 }
                    subTrigger { t1 subtype t3 }
                }
                t1 subtype t3
            }
        }
        axiom("subtypeAntisymmetric") {
            Exp.forall(t1, t2) { t1, t2 ->
                assumption {
                    compoundTrigger {
                        subTrigger { t1 subtype t2 }
                        subTrigger { t2 subtype t1 }
                    }
                }
                t1 eq t2
            }
        }
        axiom("nullableIdempotent") {
            Exp.forall(t) { t ->
                simpleTrigger { nullable(nullable(t)) } eq nullable(t)
            }
        }
        axiom("nullableSupertype") {
            Exp.forall(t) { t ->
                t subtype simpleTrigger { nullable(t) }
            }
        }
        axiom("nullablePreservesSubtype") {
            Exp.forall(t1, t2) { t1, t2 ->
                assumption { t1 subtype t2 }
                simpleTrigger { nullable(t1) subtype nullable(t2) }
            }
        }
        axiom("nullableAnySupertype") {
            Exp.forall(t) { t ->
                t subtype nullable(anyType())
            }
        }
        nonNullableTypes.forEach {
            axiom { it() subtype anyType() }
        }
        axiom("supertypeOfNothing") {
            Exp.forall(t) { t ->
                nothingType() subtype t
            }
        }
        axiom("anyNotNullableTypeLevel") {
            Exp.forall(t) { t ->
                !isSubtype(nullable(t), anyType())
            }
        }
        axiom("nullSmartcastValueLevel") {
            Exp.forall(r, t) { r, t ->
                assumption {
                    simpleTrigger { r isOf nullable(t) }
                }
                (r eq nullValue()) or (r isOf t)
            }
        }
        axiom("nothingEmpty") {
            Exp.forall(r) { r ->
                !(r isOf nothingType())
            }
        }
        axiom("nullSmartcastTypeLevel") {
            Exp.forall(t1, t2) { t1, t2 ->
                assumption {
                    compoundTrigger {
                        subTrigger { t1 subtype anyType() }
                        subTrigger { t1 subtype nullable(t2) }
                    }
                }
                t1 subtype t2
            }
        }
        axiom("typeOfNull") {
            nullValue() isOf nullable(nothingType())
        }
        axiom("anyNotNullableValueLevel") {
            !(nullValue() isOf anyType())
        }
        axiom("typeOfUnit") {
            unitValue() isOf unitType()
        }
        axiom("uniquenessOfUnit") {
            Exp.forall(r) { r ->
                assumption {
                    simpleTrigger { r isOf unitType() }
                }
                r eq unitValue()
            }
        }
        allInjections.forEach {
            it.apply { injectionAxioms() }
        }
        typeResolver.classTypeEmbeddings().forEach { type ->
            typeResolver.lookupSuperTypes(type.name).forEach { superType ->
                axiom {
                    type.runtimeType subtype superType.runtimeType
                }
            }
        }
    }
}
