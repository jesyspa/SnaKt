/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.core.domains.domainVar
import org.jetbrains.kotlin.formver.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.types.embedClassTypeFunc
import org.jetbrains.kotlin.formver.names.FunctionKotlinName
import org.jetbrains.kotlin.formver.names.SimpleKotlinName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.NameResolver
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.name.Name


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
class RuntimeTypeDomain(private val classes: List<ClassTypeEmbedding>) : BuiltinDomain(RUNTIME_TYPE_DOMAIN_NAME) {
    override val typeVars: List<Type.TypeVar> = emptyList()

    // Define types that are not dependent on the user defined classes in a companion object.
    // That way other classes can refer to them without having an explicit reference to the concrete TypeDomain.
    companion object {

        val RuntimeType: Type.Domain
            get() = Type.Domain(DomainName(RUNTIME_TYPE_DOMAIN_NAME), emptyList())
        val Ref = Type.Ref

        fun createDomainFunc(funcName: MangledName, args: List<Declaration.LocalVarDecl>, type: Type, unique: Boolean = false) =
            DomainFunc(DomainFuncName(DomainName(RUNTIME_TYPE_DOMAIN_NAME), funcName), args, emptyList(), type, unique)

        private fun createNewTypeDomainFunc(funcName: MangledName) = createDomainFunc(
            funcName,
            emptyList(),
            RuntimeType,
            true,
        )

        // variables for readability improving

        private val t = domainVar("t", RuntimeType)

        private val t1 = domainVar("t1", RuntimeType)


        private val t2 = domainVar("t2", RuntimeType)

        private val t3 = domainVar("t3", RuntimeType)
        private val r = domainVar("r", Ref)

        // three basic functions
        /** `isSubtype: (Type, Type) -> Bool` */

        val isSubtype: DomainFunc
            get() = createDomainFunc(SimpleKotlinName(Name.identifier("isSubtype")), listOf(t1.decl(), t2.decl()), Type.Bool)

        infix fun Exp.subtype(otherType: Exp) = isSubtype(this, otherType)

        /** `typeOf: Ref -> Type` */

        val typeOf: DomainFunc
            get() = createDomainFunc(SimpleKotlinName(Name.identifier("typeOf")), listOf(r.decl()), RuntimeType)

        /** `nullable: Type -> Type` */

        val nullable: DomainFunc
            get() = createDomainFunc(SimpleKotlinName(Name.identifier("nullable")), listOf(t.decl()), RuntimeType)


        // many axioms will use `is` which can be represented as composition of `isSubtype` and `typeOf`
        /** `is: (Ref, Type) -> Bool` */

        infix fun Exp.isOf(elemType: Exp) = isSubtype(typeOf(this), elemType)

        // built-in types function

        val charType: DomainFunc
            get() = createNewTypeDomainFunc(SimpleKotlinName(Name.identifier("charType")))

        val intType: DomainFunc
            get() = createNewTypeDomainFunc(SimpleKotlinName(Name.identifier("intType")))


        val boolType: DomainFunc
            get() = createNewTypeDomainFunc(SimpleKotlinName(Name.identifier("boolType")))


        val unitType: DomainFunc
            get() = createNewTypeDomainFunc(SimpleKotlinName(Name.identifier("unitType")))


        val stringType: DomainFunc
            get() = createNewTypeDomainFunc(SimpleKotlinName(Name.identifier("stringType")))


        val nothingType: DomainFunc
            get() = createNewTypeDomainFunc(SimpleKotlinName(Name.identifier("nothingType")))


        val anyType: DomainFunc
            get() = createNewTypeDomainFunc(SimpleKotlinName(Name.identifier("anyType")))


        val functionType: DomainFunc
            get() = createNewTypeDomainFunc(SimpleKotlinName(Name.identifier("functionType")))

        // for creation of user types
        fun classTypeFunc(name: MangledName) = createDomainFunc(name, emptyList(), RuntimeType, true)

        // bijections to primitive types

        val intInjection: Injection
            get() = Injection("int", Type.Int, intType)

        val boolInjection: Injection
            get() = Injection("bool", Type.Bool, boolType)


        val charInjection: Injection
            get() = Injection("char", Type.Int, charType)

        val stringInjection: Injection
            get() = Injection("string", Type.Seq(Type.Int), stringType)

        val allInjections: List<Injection>
            get() = listOf(intInjection, boolInjection, charInjection, stringInjection)

        // special values
        val nullValue = createDomainFunc(SimpleKotlinName(Name.identifier("nullValue")), emptyList(), Ref)
        val unitValue = createDomainFunc(SimpleKotlinName(Name.identifier("unitValue")), emptyList(), Ref)

    }
    val classTypes:Map<ClassTypeEmbedding, DomainFunc>
        get() = classes.associateWith { it.embedClassTypeFunc() }
    val builtinTypes: List<DomainFunc>
        get() = listOf(intType, boolType, charType, unitType, nothingType, anyType, functionType, stringType)

    val nonNullableTypes: List<DomainFunc>
        get() = buildList {
            addAll(builtinTypes)
            addAll(classTypes.values)
        }.distinctBy { it.name }
    override val functions: List<DomainFunc>
        get() = nonNullableTypes + listOf(nullValue, unitValue, isSubtype, typeOf, nullable) +
                allInjections.flatMap { listOf(it.toRef, it.fromRef) }
    override val axioms: List<DomainAxiom>
        get() = AxiomListBuilder.build(this) {
            axiom("subtype_reflexive") {
                Exp.forall(t) { t -> t subtype t }
            }
            axiom("subtype_transitive") {
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
                        subTrigger { t2 subtype t3}
                        subTrigger { t1 subtype t3}
                    }
                    t1 subtype t3
                }
            }
            axiom("subtype_antisymmetric") {
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
            axiom("nullable_idempotent") {
                Exp.forall(t) { t ->
                    simpleTrigger { nullable(nullable(t)) } eq nullable(t)
                }
            }
            axiom("nullable_supertype") {
                Exp.forall(t) { t ->
                    t subtype simpleTrigger { nullable(t) }
                }
            }
            axiom("nullable_preserves_subtype") {
                Exp.forall(t1, t2) { t1, t2 ->
                    assumption { t1 subtype t2 }
                    simpleTrigger { nullable(t1) subtype nullable(t2) }
                }
            }
            axiom("nullable_any_supertype") {
                Exp.forall(t) { t ->
                    t subtype nullable(anyType())
                }
            }
            nonNullableTypes.forEach {
                axiom { it() subtype anyType() }
            }
            axiom("supertype_of_nothing") {
                Exp.forall(t) { t ->
                    nothingType() subtype t
                }
            }
            axiom("any_not_nullable_type_level") {
                Exp.forall(t) { t ->
                    !isSubtype(nullable(t), anyType())
                }
            }
            axiom("null_smartcast_value_level") {
                Exp.forall(r, t) { r, t ->
                    assumption {
                        simpleTrigger { r isOf nullable(t) }
                    }
                    (r eq nullValue()) or (r isOf t)
                }
            }
            axiom("nothing_empty") {
                Exp.forall(r) { r ->
                    !(r isOf nothingType())
                }
            }
            axiom("null_smartcast_type_level") {
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
            axiom("type_of_null") {
                nullValue() isOf nullable(nothingType())
            }
            axiom("any_not_nullable_value_level") {
                !(nullValue() isOf anyType())
            }
            axiom("type_of_unit") {
                unitValue() isOf unitType()
            }
            axiom("uniqueness_of_unit") {
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
            classTypes.forEach { (typeEmbedding, typeFunction) ->
                typeEmbedding.details.superTypes.forEach {
                    classTypes[it]?.let { supertypeFunction ->
                        axiom {
                            typeFunction() subtype supertypeFunction()
                        }
                    }
                }
            }
        }
}