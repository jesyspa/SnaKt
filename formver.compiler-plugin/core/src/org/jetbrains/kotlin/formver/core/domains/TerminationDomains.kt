/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.domains

import org.jetbrains.kotlin.formver.core.names.DomainName
import org.jetbrains.kotlin.formver.core.names.QualifiedDomainFuncName
import org.jetbrains.kotlin.formver.core.names.UnqualifiedDomainFuncName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*

/*
 * Transcriptions of the parts of Viper's own termination library that we need.
 *
 * Silver ships these as `.vpr` files (`import/decreases/` and
 * `import/predicateinstance/pi.vpr`) that a hand-written Viper program pulls in with
 * `import`. We generate the program instead of parsing one, so the declarations have to
 * be built here.
 *
 * `TerminationPlugin` and `PredicateInstancePlugin` in silver look these declarations up
 * by their Viper identifier, so `WellFoundedOrder`, `PredicateInstance`, `decreasing`,
 * `bounded` and `nestedPredicates` have to reach the output unmangled. That holds as long
 * as nothing else in the program claims those identifiers first: the name resolver only
 * rewrites a name once it collides.
 *
 * The `PredicateInstance` type is the exception: PredicateInstanceType takes it from the
 * plugin directly and so never passes through the resolver at all. The domain declaration of
 * the same name still does, and is still required.
 */

const val WELL_FOUNDED_ORDER_DOMAIN_NAME = "WellFoundedOrder"
const val BOOL_WELL_FOUNDED_ORDER_DOMAIN_NAME = "BoolWellFoundedOrder"
const val PREDICATE_INSTANCE_DOMAIN_NAME = "PredicateInstance"
const val PREDICATE_INSTANCES_NESTED_RELATION_DOMAIN_NAME = "PredicateInstancesNestedRelation"
const val PREDICATE_INSTANCES_WELL_FOUNDED_ORDER_DOMAIN_NAME = "PredicateInstancesWellFoundedOrder"

private fun createDomainFunc(
    domainName: SymbolicName,
    funcName: String,
    args: List<Declaration.LocalVarDecl>,
    typeArgs: List<Type.TypeVar>,
    returnType: Type,
) = DomainFunc(
    QualifiedDomainFuncName(domainName, UnqualifiedDomainFuncName(funcName)),
    domainName,
    args,
    typeArgs,
    returnType,
    unique = false,
)

/**
 * ```viper
 * domain WellFoundedOrder[T] {
 *   function decreasing(arg1: T, arg2: T): Bool
 *   function bounded(arg1: T): Bool
 * }
 * ```
 *
 * The two functions are uninterpreted here; the orders for the individual types
 * (`PredicateInstancesWellFoundedOrder` and friends) supply the axioms.
 */
object WellFoundedOrder : BuiltinDomain(DomainName(WELL_FOUNDED_ORDER_DOMAIN_NAME)) {
    val elementType = Type.TypeVar("T")

    override val typeVars: List<Type.TypeVar> = listOf(elementType)

    private val arg1 = domainVar("arg1", elementType)
    private val arg2 = domainVar("arg2", elementType)

    /** `decreasing: (T, T) -> Bool`, read as "arg1 is smaller than arg2". */
    val decreasing: DomainFunc = createDomainFunc(
        name, "decreasing", listOf(arg1.decl(), arg2.decl()), typeVars, Type.Bool
    )

    /** `bounded: T -> Bool` */
    val bounded: DomainFunc = createDomainFunc(
        name, "bounded", listOf(arg1.decl()), typeVars, Type.Bool
    )

    override val functions: List<DomainFunc> = listOf(decreasing, bounded)
    override val axioms: List<DomainAxiom> = emptyList()
}

/**
 * ```viper
 * domain PredicateInstance {}
 * ```
 *
 * The empty domain that gives predicate instances their type. Silver's
 * `PredicateInstancePlugin` refuses to run on a program that does not declare it, and generates
 * the functions producing values of the type itself.
 *
 * The declaration is separate from the type: [PredicateInstanceType] is what to write where the
 * type is used, and this is what makes the plugin's search for the declaration succeed.
 */
object PredicateInstanceDomain : BuiltinDomain(DomainName(PREDICATE_INSTANCE_DOMAIN_NAME)) {
    override val typeVars: List<Type.TypeVar> = emptyList()
    override val functions: List<DomainFunc> = emptyList()
    override val axioms: List<DomainAxiom> = emptyList()
}

/**
 * ```viper
 * domain PredicateInstancesNestedRelation {
 *   function nestedPredicates(l1: PredicateInstance, l2: PredicateInstance): Bool
 *
 *   axiom nestedTrans {
 *     (forall l1: PredicateInstance, l2: PredicateInstance, l3: PredicateInstance ::
 *       { nestedPredicates(l1, l2), nestedPredicates(l2, l3) }
 *       nestedPredicates(l1, l2) && nestedPredicates(l2, l3) ==> nestedPredicates(l1, l3))
 *   }
 *
 *   axiom nestedReflex {
 *     (forall l1: PredicateInstance :: !nestedPredicates(l1, l1))
 *   }
 * }
 * ```
 *
 * `nestedPredicates(l1, l2)` holds when `l1` is unfolded from `l2`, so it is the strict
 * order that termination of predicate-recursive definitions is measured against.
 */
object PredicateInstancesNestedRelation :
    BuiltinDomain(DomainName(PREDICATE_INSTANCES_NESTED_RELATION_DOMAIN_NAME)) {
    override val typeVars: List<Type.TypeVar> = emptyList()

    private val l1 = domainVar("l1", PredicateInstanceType)
    private val l2 = domainVar("l2", PredicateInstanceType)
    private val l3 = domainVar("l3", PredicateInstanceType)

    /** `nestedPredicates: (PredicateInstance, PredicateInstance) -> Bool` */
    val nestedPredicates: DomainFunc = createDomainFunc(
        name, "nestedPredicates", listOf(l1.decl(), l2.decl()), emptyList(), Type.Bool
    )

    private fun nested(first: Exp, second: Exp): Exp =
        funcApp(nestedPredicates, listOf(first, second), emptyMap())

    override val functions: List<DomainFunc> = listOf(nestedPredicates)
    override val axioms: List<DomainAxiom> = AxiomListBuilder.build(this) {
        axiom("nestedTrans") {
            Exp.forall(l1, l2, l3) { l1, l2, l3 ->
                assumption {
                    compoundTrigger {
                        subTrigger { nested(l1, l2) }
                        subTrigger { nested(l2, l3) }
                    }
                }
                nested(l1, l3)
            }
        }
        // A predicate cannot be nested inside itself. Deliberately left without a trigger,
        // as in silver's own version.
        axiom("nestedReflex") {
            Exp.forall(l1) { l1 -> !nested(l1, l1) }
        }
    }
}

/**
 * ```viper
 * domain PredicateInstancesWellFoundedOrder {
 *   axiom predicate_instances_ax_dec {
 *     (forall l1: PredicateInstance, l2: PredicateInstance ::
 *       { nestedPredicates(l1, l2) }
 *       decreasing(l1, l2) <==> nestedPredicates(l1, l2))
 *   }
 *
 *   axiom predicate_instances_ax_bound {
 *     (forall l1: PredicateInstance :: { bounded(l1) } bounded(l1))
 *   }
 * }
 * ```
 *
 * This is the instantiation of [WellFoundedOrder] at [PredicateInstanceType]: it says the
 * well-founded order on predicate instances is the nesting relation, and that every
 * predicate instance is bounded.
 *
 * `<==>` and `==` are the same node in silver, so the first axiom is emitted with `==`.
 */
object PredicateInstancesWellFoundedOrder :
    BuiltinDomain(DomainName(PREDICATE_INSTANCES_WELL_FOUNDED_ORDER_DOMAIN_NAME)) {
    override val typeVars: List<Type.TypeVar> = emptyList()

    private val l1 = domainVar("l1", PredicateInstanceType)
    private val l2 = domainVar("l2", PredicateInstanceType)

    /**
     * The instantiation of [WellFoundedOrder]'s type parameter that every application in
     * this domain carries. Passing the identity map or an empty one instead type-checks and
     * verifies without complaint, but the axioms then constrain the wrong instance of
     * `decreasing`/`bounded` and every termination proof goes through vacuously.
     */
    private val atPredicateInstance = mapOf(WellFoundedOrder.elementType to PredicateInstanceType)

    private fun decreasing(smaller: Exp, larger: Exp): Exp =
        WellFoundedOrder.funcApp(WellFoundedOrder.decreasing, listOf(smaller, larger), atPredicateInstance)

    private fun bounded(arg: Exp): Exp =
        WellFoundedOrder.funcApp(WellFoundedOrder.bounded, listOf(arg), atPredicateInstance)

    private fun nested(first: Exp, second: Exp): Exp =
        PredicateInstancesNestedRelation.funcApp(
            PredicateInstancesNestedRelation.nestedPredicates, listOf(first, second), emptyMap()
        )

    override val functions: List<DomainFunc> = emptyList()
    override val axioms: List<DomainAxiom> = AxiomListBuilder.build(this) {
        axiom("predicate_instances_ax_dec") {
            Exp.forall(l1, l2) { l1, l2 ->
                decreasing(l1, l2) eq simpleTrigger { nested(l1, l2) }
            }
        }
        axiom("predicate_instances_ax_bound") {
            Exp.forall(l1) { l1 -> simpleTrigger { bounded(l1) } }
        }
    }
}

/**
 * ```viper
 * domain BoolWellFoundedOrder {
 *   axiom bool_ax_dec { decreasing(false, true) }
 *   axiom bool_ax_bound { (forall bool1: Bool :: { bounded(bool1) } bounded(bool1)) }
 * }
 * ```
 *
 * The instantiation of [WellFoundedOrder] at `Bool`, ordering `false` below `true`.
 *
 * A measure over a nullable structure needs this as much as it needs the order on predicate
 * instances. Where a recursive call reaches the end of the structure, the two predicate instances
 * being compared are unrelated — the unique predicate nests the next link only where that link is
 * non-null — and it is the leading nullity component that has to decrease, from `true` to `false`.
 */
object BoolWellFoundedOrder : BuiltinDomain(DomainName(BOOL_WELL_FOUNDED_ORDER_DOMAIN_NAME)) {
    override val typeVars: List<Type.TypeVar> = emptyList()

    private val bool1 = domainVar("bool1", Type.Bool)

    private val atBool = mapOf(WellFoundedOrder.elementType to Type.Bool)

    private fun decreasing(smaller: Exp, larger: Exp): Exp =
        WellFoundedOrder.funcApp(WellFoundedOrder.decreasing, listOf(smaller, larger), atBool)

    private fun bounded(arg: Exp): Exp =
        WellFoundedOrder.funcApp(WellFoundedOrder.bounded, listOf(arg), atBool)

    override val functions: List<DomainFunc> = emptyList()
    override val axioms: List<DomainAxiom> = AxiomListBuilder.build(this) {
        // Deliberately left without a trigger, as in silver's own version.
        axiom("bool_ax_dec") {
            decreasing(Exp.BoolLit(false), Exp.BoolLit(true))
        }
        axiom("bool_ax_bound") {
            Exp.forall(bool1) { bool1 -> simpleTrigger { bounded(bool1) } }
        }
    }
}

/**
 * The termination-library domains, in the order silver's `.vpr` sources declare them.
 *
 * A domain's name is registered with the name resolver only if the domain is in
 * `Program.domains`, so all of them have to be listed even though some are only
 * referred to by the others.
 */
val terminationDomains: List<Domain> = listOf(
    WellFoundedOrder,
    BoolWellFoundedOrder,
    PredicateInstanceDomain,
    PredicateInstancesNestedRelation,
    PredicateInstancesWellFoundedOrder,
)
