/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.embeddings.FunctionBodyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.LabelEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.core.embeddings.callables.NamedFunctionSignatureWithContract
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.properties.BackingFieldGetter
import org.jetbrains.kotlin.formver.core.embeddings.properties.ClassPropertyAccess
import org.jetbrains.kotlin.formver.core.embeddings.properties.PropertyAccessEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.PropertyEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.properties.asPropertyAccess
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.fillHoles
import org.jetbrains.kotlin.formver.core.isCustom
import org.jetbrains.kotlin.formver.core.isInvariantBuilderFunctionNamed
import org.jetbrains.kotlin.formver.core.linearization.*
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

/**
 * Interface for statement conversion.
 *
 * Naming convention:
 * - Functions that return a new `StmtConversionContext` should describe what change they make (`addResult`, `removeResult`...)
 * - Functions that take a lambda to execute should describe what extra state the lambda will have (`withResult`...)
 */
interface StmtConversionContext : MethodConversionContext {
    val whenSubject: VariableEmbedding?

    /**
     * In a safe call `callSubject?.foo()` we evaluate the call subject first to check for nullness.
     * In case it is not null, we evaluate the call to `callSubject.foo()`. Here we don't want to evaluate
     * the `callSubject` again to we store it in the `StmtConversionContext`.
     */
    val checkedSafeCallSubject: ExpEmbedding?
    val activeCatchLabels: List<LabelEmbedding>

    fun continueLabelName(targetName: String? = null): SymbolicName
    fun breakLabelName(targetName: String? = null): SymbolicName
    fun addLoopName(targetName: String)
    fun convert(stmt: FirStatement): ExpEmbedding

    fun <R> withNewScope(action: StmtConversionContext.() -> R): R
    fun <R> withNoScope(action: StmtConversionContext.() -> R): R
    fun <R> withMethodCtx(factory: MethodContextFactory, action: StmtConversionContext.() -> R): R

    fun <R> withFreshWhile(label: FirLabel?, action: StmtConversionContext.() -> R): R
    fun <R> withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext.() -> R): R
    fun <R> withCheckedSafeCallSubject(subject: ExpEmbedding?, action: StmtConversionContext.() -> R): R
    fun <R> withCatches(
        catches: List<FirCatch>,
        action: StmtConversionContext.(catchBlockListData: CatchBlockListData) -> R,
    ): Pair<CatchBlockListData, R>
}

fun StmtConversionContext.declareLocalProperty(symbol: FirPropertySymbol, initializer: ExpEmbedding?): Declare {
    registerLocalProperty(symbol)
    val variable = embedLocalProperty(symbol)
    return Declare(variable, initializer?.withType(variable.type))
}

fun StmtConversionContext.declareLocalVariable(symbol: FirVariableSymbol<*>, initializer: ExpEmbedding?): Declare {
    registerLocalVariable(symbol)
    val variable = embedLocalVariable(symbol)
    return Declare(variable, initializer?.withType(variable.type))
}

fun StmtConversionContext.declareAnonVar(type: TypeEmbedding, initializer: ExpEmbedding?): Declare {
    val variable = freshAnonVar(type)
    return Declare(variable, initializer?.withType(variable.type))
}


val FirIntersectionOverridePropertySymbol.propertyIntersections
    get() = intersections.filterIsInstanceAnd<FirPropertySymbol> { it.isVal == isVal }

/**
 * Tries to find final property symbol actually declared in some class instead of
 * (potentially) fake property symbol.
 * Note that if some property is found it is fixed since
 * 1. there can't be two non-abstract properties which don't subsume each other
 * in the hierarchy (kotlin disallows that) and final properties can't be abstract;
 * 2. final property can't subsume other final property as that means final property
 * is overridden.
 * //TODO: decide if we leave this lookup or consider it unsafe.
 */
fun FirPropertySymbol.findFinalParentProperty(): FirPropertySymbol? =
    if (this !is FirIntersectionOverridePropertySymbol)
        (isFinal && !isCustom).ifTrue { this }
    else propertyIntersections.firstNotNullOfOrNull { it.findFinalParentProperty() }


/**
 * This is a key function when looking up properties.
 * It translates a kotlin `receiver.field` expression to an `ExpEmbedding`.
 *
 * Note that in FIR this `field` may be represented as `FirIntersectionOverridePropertySymbol`
 * which is necessary when the property could hypothetically inherit from multiple sources.
 * However, we don't register such symbols in the context when traversing the class.
 * Hence, some advanced logic is needed here.
 *
 * First, we try to find an actual backing field somewhere in the parents of the field with a
 * dfs-like algorithm on `FirIntersectionOverridePropertySymbol`s (it also should be final).
 *
 * If final backing field is not found, we lazily create a getter/setter pair for this
 * `FirIntersectionOverrideProperty`.
 */
fun StmtConversionContext.embedPropertyAccess(accessExpression: FirPropertyAccessExpression): PropertyAccessEmbedding =
    when (val calleeSymbol = accessExpression.calleeReference.symbol) {
        is FirValueParameterSymbol -> embedParameter(calleeSymbol).asPropertyAccess()
        is FirPropertySymbol -> {
            val type = embedType(calleeSymbol.resolvedReturnType)
            when {
                accessExpression.dispatchReceiver != null -> {
                    val property = calleeSymbol.findFinalParentProperty()?.let {
                        embedProperty(it)
                    } ?: embedProperty(calleeSymbol)
                    ClassPropertyAccess(convert(accessExpression.dispatchReceiver!!), property, type)
                }

                accessExpression.extensionReceiver != null -> {
                    val property = embedProperty(calleeSymbol)
                    ClassPropertyAccess(convert(accessExpression.extensionReceiver!!), property, type)
                }

                else -> embedLocalProperty(calleeSymbol)
            }
        }

        else ->
            error("Property access symbol $calleeSymbol has unsupported type.")
    }


fun StmtConversionContext.argumentDeclaration(
    arg: ExpEmbedding,
    callType: TypeEmbedding
): Pair<Declare?, ExpEmbedding> =
    when (arg.ignoringMetaNodes()) {
        is LambdaExp -> null to arg
        else -> {
            val argWithInvariants = arg.withNewTypeInvariants(callType, typeResolver) {
                proven = true
                access = true
            }
            // If `argWithInvariants` is `Cast(...(Cast(someVariable))...)` it is fine to use it
            // since in Viper it will always be translated to `someVariable`.
            // On other hand, `TypeEmbedding` and invariants in Viper are guaranteed
            // via previous line.
            if (argWithInvariants.underlyingVariable != null) null to argWithInvariants
            else declareAnonVar(callType, argWithInvariants).let {
                it to it.variable
            }
        }
    }

fun StmtConversionContext.getInlineFunctionCallArgs(
    args: List<ExpEmbedding>,
    formalArgTypes: List<TypeEmbedding>,
): Pair<List<Declare>, List<ExpEmbedding>> {
    val declarations = mutableListOf<Declare>()
    val storedArgs = args.zip(formalArgTypes).map { (arg, callType) ->
        argumentDeclaration(arg, callType).let { (declaration, usage) ->
            declarations.addIfNotNull(declaration)
            usage
        }
    }
    return Pair(declarations, storedArgs)
}

fun StmtConversionContext.insertInlineFunctionCall(
    calleeSignature: FunctionSignature,
    paramNames: List<SubstitutedArgument>,
    args: List<ExpEmbedding>,
    body: FirBlock,
    returnTargetName: String?,
    parentCtx: MethodConversionContext? = null,
): ExpEmbedding {
    // TODO: It seems like it may be possible to avoid creating a local here, but it is not clear how.
    val returnTarget = returnTargetProducer.getFresh(calleeSignature.callableType.returnType)
    assert(returnTarget.label != null) {
        "Return target label not found for function ${calleeSignature.callableType.name}"
    }
    val (declarations, callArgs) = getInlineFunctionCallArgs(args, calleeSignature.callableType.formalArgTypes)
    val subs = paramNames.zip(callArgs).toMap()
    val methodCtxFactory = MethodContextFactory(
        calleeSignature,
        InlineParameterResolver(subs, returnTargetName, returnTarget),
        parent = parentCtx,
    )

    return withMethodCtx(methodCtxFactory) {
        Block {
            add(Declare(returnTarget.variable, null))
            addAll(declarations)
            add(FunctionExp(null, convert(body), returnTarget.label!!))
            // if unit is what we return we might not guarantee it yet
            add(returnTarget.variable.withIsUnitInvariantIfUnit(typeResolver))
        }
    }
}

/**
 * Lowers a call to the primary constructor of [symbol]'s class into the statements that build the object
 * here, rather than a call to the bodyless `con_C` method. Returns null if the constructed type has no
 * class embedding, and so no unique predicate to establish.
 *
 * `con_C` promises `acc(C_unique(ret), write)` and nothing about what is inside it, which leaves the
 * predicate's snapshot unconstrained: a heap-dependent function reading through a nested predicate is
 * then unrelated to the values the constructor was given. Allocating the object and folding the
 * predicate here ties the snapshot to the heap the arguments were written into.
 *
 * The predicate also demands facts that no argument establishes: the type of a backing field that no
 * constructor parameter writes (a Kotlin initializer in the class body is not converted today), and the
 * unique predicate of a `@Unique` property in the same position. Those are inhaled so the fold goes
 * through. `con_C` assumed all of this wholesale in its postcondition, so this assumes no more than the
 * call it replaces.
 */
fun StmtConversionContext.insertPrimaryConstructorCall(
    symbol: FirFunctionSymbol<*>,
    callable: CallableEmbedding,
    args: List<ExpEmbedding>,
): ExpEmbedding? {
    val classType = embedType(symbol.resolvedReturnType)
    val pretype = classType.pretype as? ClassTypeEmbedding ?: return null
    val parameterMatching = with(this) { symbol.constructedClassSymbol().primaryConstructorPropertyMatching() }

    val (declarations, callArgs) = getInlineFunctionCallArgs(args, callable.callableType.formalArgTypes)
    val valueArgs = callArgs.takeLast(symbol.valueParameterSymbols.size)
    val obj = freshAnonVar(classType)

    val initialized = mutableSetOf<PropertyEmbedding>()
    val initializers = symbol.valueParameterSymbols.zip(valueArgs).mapNotNull { (param, arg) ->
        val property = parameterMatching[param] ?: return@mapNotNull null
        initialized.add(property)
        when (val getter = property.getter!!) {
            is BackingFieldGetter -> InitField(obj, getter.field, arg.withType(getter.field.type))
            else -> InhaleDirect(EqCmp(getter.getValueSimple(obj, typeResolver), arg))
        }
    }

    // Innermost first: folding a predicate consumes the predicates of the supertypes it nests.
    val foldOrder = typeResolver.uniquePredicateFoldOrder(pretype)

    return Block {
        addAll(declarations)
        add(Declare(obj, null))
        add(AllocateObject(obj, pretype))
        obj.provenInvariants().forEach { add(InhaleDirect(it)) }
        addAll(initializers)
        // The permission part of the type's access invariants comes from the allocation; what remains are
        // the invariants over the field values, which have to be assumed of the values just written.
        typeResolver.flatMapUniqueFields(pretype.name) { it.extraAccessInvariantsForParameter() }
            .fillHoles(obj)
            .forEach { add(InhaleDirect(it)) }
        foldOrder.forEach { addAll(residualPredicateAssumptions(it, obj, initialized)) }
        foldOrder.forEach {
            add(Fold(PredicateAccessPermissions(it.uniquePredicateName, listOf(obj), PermExp.FullPerm())))
        }
        add(obj)
    }
}

/**
 * The conjuncts of the unique predicate of [classType] that a primary constructor call does not establish
 * from its arguments, as assumptions about [obj].
 *
 * [initialized] are the properties a constructor parameter was written into.
 */
private fun StmtConversionContext.residualPredicateAssumptions(
    classType: ClassTypeEmbedding,
    obj: VariableEmbedding,
    initialized: Set<PropertyEmbedding>,
): List<ExpEmbedding> = buildList {
    for (property in typeResolver.lookupClassProperties(classType.name)) {
        if (property in initialized) continue
        val field = (property.getter as? BackingFieldGetter)?.field
        if (field != null && field.accessPolicy != AccessPolicy.ALWAYS_WRITEABLE) {
            add(InhaleDirect(field.type.subTypeInvariant().fillHole(PrimitiveFieldAccess(obj, field))))
        }
        if (!property.isUnique) continue
        val value = field?.let { PrimitiveFieldAccess(obj, it) } ?: property.getter?.getValueSimple(obj, typeResolver)
        value?.let { plain ->
            property.type.uniquePredicateAccessInvariant(typeResolver)?.fillHole(plain)?.let { add(InhaleDirect(it)) }
        }
    }
}

/**
 * [classType] and its supertypes, ordered so that every class comes after the supertypes its unique
 * predicate nests.
 */
private fun TypeResolver.uniquePredicateFoldOrder(classType: ClassTypeEmbedding): List<ClassTypeEmbedding> =
    lookupSuperTypes(classType.name).flatMap { uniquePredicateFoldOrder(it) } + classType

/**
 * Insert `ForAllEmbedding` where `forAll` function call was encountered.
 */
fun StmtConversionContext.insertForAllFunctionCall(
    symbol: FirValueParameterSymbol,
    block: FirBlock,
): ExpEmbedding {
    val anonVar = freshAnonBuiltinVar(embedType(symbol.resolvedReturnType))
    val methodCtxFactory = MethodContextFactory(
        signature,
        InlineParameterResolver(
            substitutions = mapOf(SubstitutedArgument.ValueParameter(symbol) to anonVar),
            labelName = null,
            // TODO: ideally, there shouldn't be a return target since return is prohibited
            defaultResolvedReturnTarget = defaultResolvedReturnTarget,
        ),
        parent = this,
    )
    return withNoScope {
        withMethodCtx(methodCtxFactory) {
            val (invariants, triggers) = collectInvariantsAndTriggers(block)
            ForAllEmbedding(anonVar, invariants, triggers)
        }
    }
}

fun StmtConversionContext.convertImpureBody(
    declaration: FirSimpleFunction,
    signature: NamedFunctionSignatureWithContract,
    returnTarget: ReturnTarget,
): ConvertedMethodBody? {
    val firBody = declaration.body ?: return null
    val body = convert(firBody)
    val returnLabel = returnTarget.label ?: throw SnaktInternalException(
        declaration.source, "Return target label not found for method ${declaration.name}"
    )
    val bodyExp = FunctionExp(signature, body, returnLabel)
    return ConvertedMethodBody(bodyExp, returnTarget)
}

fun StmtConversionContext.convertPureBody(declaration: FirSimpleFunction): ExpEmbedding {
    val firBody = declaration.body ?: throw SnaktInternalException(
        declaration.source,
        "Pure functions expect a function body to exist"
    )
    return convert(firBody)
}

fun ProgramConversionContext.linearizeImpureBody(
    source: KtSourceElement?,
    converted: ConvertedMethodBody,
): FunctionBodyEmbedding {
    val seqnBuilder = SeqnBuilder(source)
    val linearizer =
        Linearizer(SharedLinearizationState(anonVarProducer), seqnBuilder, source, typeResolver)
    converted.bodyExp.toLinearizable(source).toViperUnusedResult(linearizer)
    // note: we must guarantee somewhere that returned value is Unit
    // as we may not encounter any `return` statement in the body
    converted.returnTarget.variable.withIsUnitInvariantIfUnit(typeResolver)
        .toLinearizable(source).toViperUnusedResult(linearizer)
    return FunctionBodyEmbedding(seqnBuilder.block)
}

fun ProgramConversionContext.linearizePureBody(
    source: KtSourceElement?,
    body: ExpEmbedding,
): Exp {
    val pureFunBodyLinearizer = PureFunBodyLinearizer(
        source,
        SharedLinearizationState(anonVarProducer),
        SsaConverter(source),
        typeResolver
    )
    body.toLinearizable(source).toViperUnusedResult(pureFunBodyLinearizer)
    return pureFunBodyLinearizer.constructExpression()
}

private const val INVALID_STATEMENT_MSG =
    "Every statement in invariant block must be a pure boolean invariant."

data class InvariantsAndTriggers(
    val invariants: List<ExpEmbedding>,
    val triggers: List<ExpEmbedding>
)

private fun FirBlock.isEmptyLambdaBody(): Boolean {
    if (statements.isEmpty()) return false
    return (statements.size == 1 && (statements.first() as? FirReturnExpression)?.result?.resolvedType?.isUnit ?: false)
}

fun StmtConversionContext.collectInvariants(block: FirBlock) = buildList {
    if (block.isEmptyLambdaBody()) {
        return@buildList
    }
    block.statements.forEach { stmt ->
        check(stmt is FirExpression && stmt.resolvedType.isBoolean) {
            INVALID_STATEMENT_MSG
        }
        add(stmt.accept(StmtConversionVisitor, this@collectInvariants))
    }
}

/**
 * Attempts to extract trigger expressions from a triggers() function call.
 * Returns the list of trigger expressions if this is a triggers() call, or null otherwise.
 */
private fun StmtConversionContext.tryExtractTriggers(stmt: FirStatement): List<ExpEmbedding>? {
    if (stmt !is FirFunctionCall) return null

    val symbol = stmt.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
    if (symbol?.isInvariantBuilderFunctionNamed("triggers") != true) return null

    val varargs = stmt.arguments.firstOrNull() as? FirVarargArgumentsExpression
        ?: throw IllegalArgumentException("triggers() function must have a single varargs parameter.")

    // TODO: check whether trigger is valid in Viper.
    return varargs.arguments.map { expr ->
        expr.accept(StmtConversionVisitor, this)
    }
}

fun StmtConversionContext.collectInvariantsAndTriggers(block: FirBlock): InvariantsAndTriggers {
    val invariants = mutableListOf<ExpEmbedding>()
    val triggers = mutableListOf<ExpEmbedding>()

    block.statements.forEach { stmt ->
        val extractedTriggers = tryExtractTriggers(stmt)
        if (extractedTriggers != null) {
            triggers.addAll(extractedTriggers)
            return@forEach
        }

        // Otherwise, treat as invariant
        check(stmt is FirExpression && stmt.resolvedType.isBoolean) {
            INVALID_STATEMENT_MSG
        }
        invariants.add(stmt.accept(StmtConversionVisitor, this))
    }

    return InvariantsAndTriggers(invariants, triggers)
}
