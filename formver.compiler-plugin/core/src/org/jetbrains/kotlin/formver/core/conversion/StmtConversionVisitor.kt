/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.common.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.core.embeddings.LabelLink
import org.jetbrains.kotlin.formver.core.embeddings.callables.FunctionEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.callables.insertCall
import org.jetbrains.kotlin.formver.core.embeddings.callables.isVerifyFunction
import org.jetbrains.kotlin.formver.core.embeddings.expression.*
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GeCharChar
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GeIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GtCharChar
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.GtIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.LeCharChar
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.LeIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.LtCharChar
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.LtIntInt
import org.jetbrains.kotlin.formver.core.embeddings.expression.OperatorExpEmbeddings.Not
import org.jetbrains.kotlin.formver.core.embeddings.toLink
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.equalToType
import org.jetbrains.kotlin.formver.core.functionCallArguments
import org.jetbrains.kotlin.formver.core.isInvariantBuilderFunctionNamed
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Convert a statement, emitting the resulting Viper statements and
 * declarations into the context, returning a reference to the
 * expression containing the result.  Note that in the FIR, expressions
 * are a subtype of statements.
 *
 * In many cases, we introduce a temporary variable to represent this
 * result (since, for example, a method call is not an expression).
 * When the result is an lvalue, it is important to return an expression
 * that refers to location, not just the same value, and so introducing
 * a temporary variable for the result is not acceptable in those cases.
 */
object StmtConversionVisitor : FirVisitor<ExpEmbedding, StmtConversionContext>() {
    // Note that in some cases we don't expect to ever implement it: we are only
    // translating statements here, after all.  It isn't 100% clear how best to
    // communicate this.
    override fun visitElement(element: FirElement, data: StmtConversionContext): ExpEmbedding =
        handleUnimplementedElement("Not yet implemented for $element (${element.source.text})", data)

    override fun visitReturnExpression(
        returnExpression: FirReturnExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val expr = when (returnExpression.result) {
            is FirUnitExpression -> UnitLit
            else -> data.convert(returnExpression.result)
        }
        // returnTarget is null when it is the implicit return of a lambda
        val returnTargetName = returnExpression.target.labelName
        val target = data.resolveReturnTarget(returnTargetName)
        return blockOf(
            Assign(target.variable, expr),
            Goto(target.label.toLink())
        )
    }

    override fun visitResolvedQualifier(
        resolvedQualifier: FirResolvedQualifier,
        data: StmtConversionContext
    ): ExpEmbedding {
        check(resolvedQualifier.resolvedType.isUnit) {
            "Only `Unit` is supported among resolved qualifiers currently."
        }
        return UnitLit
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext): ExpEmbedding =
        block.statements.map(data::convert).toBlock()

    override fun visitLiteralExpression(
        literalExpression: FirLiteralExpression,
        data: StmtConversionContext,
    ): ExpEmbedding =
        when (literalExpression.kind) {
            ConstantValueKind.Int -> IntLit((literalExpression.value as Long).toInt())
            ConstantValueKind.Boolean -> BooleanLit(literalExpression.value as Boolean)
            ConstantValueKind.Char -> CharLit(literalExpression.value as Char)
            ConstantValueKind.String -> StringLit(literalExpression.value as String)
            ConstantValueKind.Null -> NullLit
            else -> handleUnimplementedElement(
                "Constant Expression of type ${literalExpression.kind} is not yet implemented.",
                data
            )
        }

    private val FirLiteralExpression.stringValue: String
        get() = value.toString()

    override fun visitStringConcatenationCall(
        stringConcatenationCall: FirStringConcatenationCall,
        data: StmtConversionContext
    ): ExpEmbedding {
        val combinedLiteral = stringConcatenationCall.arguments.joinToString("") { arg ->
            check(arg is FirLiteralExpression) {
                "${arg::class.simpleName} is not supported as an element of string concatenation."
            }
            arg.stringValue
        }
        return StringLit(combinedLiteral)
    }

    override fun visitIntegerLiteralOperatorCall(
        integerLiteralOperatorCall: FirIntegerLiteralOperatorCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        return visitFunctionCall(integerLiteralOperatorCall, data)
    }

    override fun visitWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: StmtConversionContext,
    ): ExpEmbedding = data.whenSubject!!

    private fun convertWhenBranches(
        whenBranches: Iterator<FirWhenBranch>,
        type: TypeEmbedding,
        data: StmtConversionContext,
    ): ExpEmbedding {
        if (!whenBranches.hasNext()) return UnitLit

        val branch = whenBranches.next()

        // Note that only the last condition can be a FirElseIfTrue
        return if (branch.condition is FirElseIfTrueCondition) {
            data.withNewScope { convert(branch.result) }
        } else {
            val cond = data.convert(branch.condition).withType { boolean() }
            val thenExp = data.withNewScope { convert(branch.result) }
            val elseExp = convertWhenBranches(whenBranches, type, data)
            If(cond, thenExp, elseExp, type)
        }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: StmtConversionContext): ExpEmbedding =
        data.withNewScope {
            val type = data.embedType(whenExpression)
            val subj: Declare? = whenExpression.subjectVariable?.let { firSubjVar ->
                val subjExp = convert(firSubjVar.initializer!!)
                if (firSubjVar.name.isSpecial)
                    declareAnonVar(subjExp.type, subjExp)
                else
                    declareLocalVariable(firSubjVar.symbol, subjExp)
            }
            val body = withWhenSubject(subj?.variable) {
                convertWhenBranches(whenExpression.branches.iterator(), type, this)
            }
            subj?.let { blockOf(it, body) } ?: body
        }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val propertyAccess = data.embedPropertyAccess(propertyAccessExpression)
        return propertyAccess.getValue(data)
    }

    override fun visitEqualityOperatorCall(
        equalityOperatorCall: FirEqualityOperatorCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        require(equalityOperatorCall.arguments.size == 2) {
            "Invalid equality comparison $equalityOperatorCall, can only compare 2 elements."
        }
        val left = data.convert(equalityOperatorCall.arguments[0])
        val right = data.convert(equalityOperatorCall.arguments[1])

        return when (equalityOperatorCall.operation) {
            FirOperation.EQ -> convertEqCmp(left, right)
            FirOperation.NOT_EQ -> Not(convertEqCmp(left, right))
            else -> handleUnimplementedElement(
                "Equality comparison operation ${equalityOperatorCall.operation} not yet implemented.",
                data
            )
        }
    }

    private fun convertEqCmp(left: ExpEmbedding, right: ExpEmbedding): ExpEmbedding {
        //TODO: replace with call to left.equals()
        return EqCmp(left, right)
    }

    override fun visitComparisonExpression(
        comparisonExpression: FirComparisonExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val dispatchReceiver = checkNotNull(comparisonExpression.compareToCall.dispatchReceiver) {
            "found 'compareTo' call with null receiver"
        }
        val arg = checkNotNull(comparisonExpression.compareToCall.argumentList.arguments.firstOrNull()) {
            "found `compareTo` call with no argument at position 0"
        }
        val left = data.convert(dispatchReceiver)
        val right = data.convert(arg)

        val functionSymbol = comparisonExpression.compareToCall.toResolvedCallableSymbol()

        val functionType = data.embedFunctionPretype(functionSymbol as FirFunctionSymbol)

        val comparisonTemplate = when {
            functionType.formalArgTypes.all { it.equalToType { int() } } -> IntComparisonExpEmbeddingsTemplate
            functionType.formalArgTypes.all { it.equalToType { char() } } -> CharComparisonExpEmbeddingsTemplate
            else -> {
                val result = data.convert(comparisonExpression.compareToCall)
                return IntComparisonExpEmbeddingsTemplate.retrieve(comparisonExpression.operation)(result, IntLit(0))
            }
        }
        return comparisonTemplate.retrieve(comparisonExpression.operation)(left, right)
    }

    private interface ComparisonExpEmbeddingsTemplate {
        fun retrieve(operation: FirOperation): BinaryOperatorExpEmbeddingTemplate
    }

    private object IntComparisonExpEmbeddingsTemplate : ComparisonExpEmbeddingsTemplate {
        override fun retrieve(operation: FirOperation) = when (operation) {
            FirOperation.LT -> LtIntInt
            FirOperation.LT_EQ -> LeIntInt
            FirOperation.GT -> GtIntInt
            FirOperation.GT_EQ -> GeIntInt
            else -> throw IllegalArgumentException("Expected comparison operation but found ${operation}.")
        }
    }

    private object CharComparisonExpEmbeddingsTemplate : ComparisonExpEmbeddingsTemplate {
        override fun retrieve(operation: FirOperation) = when (operation) {
            FirOperation.LT -> LtCharChar
            FirOperation.LT_EQ -> LeCharChar
            FirOperation.GT -> GtCharChar
            FirOperation.GT_EQ -> GeCharChar
            else -> throw IllegalArgumentException("Expected comparison operation but found ${operation}.")
        }
    }

    private fun List<FirExpression>.withVarargsHandled(data: StmtConversionContext, function: FunctionEmbedding?) =
        flatMap { arg ->
            when (arg) {
                is FirVarargArgumentsExpression -> {
                    check(function != null && function.isVerifyFunction) {
                        "vararg arguments are currently supported for `verify` function only"
                    }
                    arg.arguments.map(data::convert)
                }

                else -> listOf(data.convert(arg))
            }
        }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext): ExpEmbedding {
        val symbol = functionCall.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            ?: throw NotImplementedError("Only functions are expected as callables of function calls, got ${functionCall.toResolvedCallableSymbol()}")

        when (val forAllLambda = functionCall.extractFormverFirBlock { isInvariantBuilderFunctionNamed("forAll") }) {
            null -> {
                val callee = data.embedFunction(symbol)
                return callee.insertCall(
                    functionCall.functionCallArguments.withVarargsHandled(data, callee),
                    data,
                    data.embedType(functionCall.resolvedType),
                )
            }

            else -> {
                if (!data.isValidForForAllBlock)
                    error("`forAll` scope is only allowed inside one of the `loopInvariants`, `preconditions` or `postconditions`.")
                val forAllArg = forAllLambda.valueParameters.first()
                val forAllBody = forAllLambda.body
                    ?: error("Lambda body should be accessible in `forAll` function call.")
                return data.insertForAllFunctionCall(forAllArg.symbol, forAllBody)
            }
        }
    }

    override fun visitImplicitInvokeCall(
        implicitInvokeCall: FirImplicitInvokeCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val receiver = implicitInvokeCall.dispatchReceiver as? FirPropertyAccessExpression
            ?: throw NotImplementedError("Implicit invoke calls only support a limited range of receivers at the moment.")
        val returnType = data.embedType(implicitInvokeCall.resolvedType)
        val receiverSymbol = receiver.calleeReference.toResolvedSymbol<FirBasedSymbol<*>>()!!
        val args = implicitInvokeCall.argumentList.arguments.withVarargsHandled(data, function = null)
        return when (val exp = data.embedLocalSymbol(receiverSymbol).ignoringMetaNodes()) {
            is LambdaExp -> {
                // The lambda is already the receiver, so we do not need to convert it.
                // TODO: do this more uniformly: convert the receiver, see it is a lambda, use insertCall on it.
                exp.insertCall(args, data, returnType)
            }

            else -> {
                InvokeFunctionObject(data.convert(receiver), args, returnType)
            }
        }
    }

    override fun visitProperty(property: FirProperty, data: StmtConversionContext): ExpEmbedding {
        val symbol = property.symbol
        check(symbol.isLocal) {
            "StmtConversionVisitor should not encounter non-local properties."
        }
        val type = data.embedType(symbol.resolvedReturnType)
        return data.declareLocalProperty(symbol, property.initializer?.let { data.convert(it).withType(type) })
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: StmtConversionContext): ExpEmbedding {
        val condition = data.convert(whileLoop.condition).withType { boolean() }
        val invariants = buildList {
            data.retrievePropertiesAndParameters().forEach {
                addIfNotNull(it.sharedPredicateAccessInvariant())
                addAll(it.provenInvariants())
            }
            extractLoopInvariants(whileLoop.block)?.let {
                addAll(data.withScopeImpl(ScopeIndex.NoScope) { data.collectInvariants(it) })
            }
        }
        return data.withFreshWhile(whileLoop.label) {
            val body = convert(whileLoop.block)
            While(condition, body, breakLabelName(), continueLabelName(), invariants)
        }
    }

    override fun visitBreakExpression(
        breakExpression: FirBreakExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val targetName = breakExpression.target.labelName
        val breakLabel = LabelLink(data.breakLabelName(targetName))
        return Goto(breakLabel)
    }

    override fun visitContinueExpression(
        continueExpression: FirContinueExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val targetName = continueExpression.target.labelName
        val continueLabel = LabelLink(data.continueLabelName(targetName))
        return Goto(continueLabel)
    }

    override fun visitDesugaredAssignmentValueReferenceExpression(
        desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression,
        data: StmtConversionContext
    ): ExpEmbedding {
        return data.convert(desugaredAssignmentValueReferenceExpression.expressionRef.value)
    }

    override fun visitVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val embedding = when (val lValue = variableAssignment.lValue) {
            is FirPropertyAccessExpression -> {
                data.embedPropertyAccess(lValue)
            }

            is FirDesugaredAssignmentValueReferenceExpression -> {
                data.embedPropertyAccess(lValue.expressionRef.value as FirPropertyAccessExpression)
            }

            else -> error("Lvalue must be either property access or desugared assignment.")
        }
        val convertedRValue = data.convert(variableAssignment.rValue)
        return embedding.setValue(convertedRValue, data)
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val exp = data.convert(smartCastExpression.originalExpression)
        val newType = data.embedType(smartCastExpression.smartcastType.coneType)
        // If the smart-cast is from A? to A, then is not necessary to inhale invariants
        return if (exp.type.getNonNullable() == newType) {
            exp.withType(newType)
        } else {
            // TODO: when there is a cast from B to A, only inhale invariants of A - invariants of B
            exp.withNewTypeInvariants(newType) {
                access = true
            }
        }
    }

    override fun visitBooleanOperatorExpression(
        booleanOperatorExpression: FirBooleanOperatorExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val left = data.convert(booleanOperatorExpression.leftOperand)
        val right = data.convert(booleanOperatorExpression.rightOperand)
        return when (booleanOperatorExpression.kind) {
            LogicOperationKind.AND -> SequentialAnd(left, right)
            LogicOperationKind.OR -> SequentialOr(left, right)
        }
    }

    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        // `thisReceiverExpression` has a bound symbol which can be used for lookup
        // for extensions `this`es the bound symbol is the function they originate from
        // for member functions the bound symbol is a class they're defined in
        //
        // since dispatch receiver can only originate from non-anonymous function we do not specify its name here
        // as we have only one candidate to resolve it
        fun tryResolve(symbol: FirBasedSymbol<*>): ExpEmbedding? {
            val resolved = when (symbol) {
                is FirClassSymbol<*> -> data.resolveDispatchReceiver()
                is FirAnonymousFunctionSymbol -> data.resolveExtensionReceiver(symbol.label!!.name)
                is FirFunctionSymbol<*> -> data.resolveExtensionReceiver(symbol.name.asString())
                else -> return null
            }

            return resolved
                ?: throw IllegalArgumentException("Can't resolve the 'this' receiver since the function does not have one.")
        }

        val symbol = thisReceiverExpression.calleeReference.boundSymbol
        tryResolve(symbol as FirBasedSymbol<*>)?.let { return it }
        val declSymbol = when (symbol) {
            is FirReceiverParameterSymbol -> symbol.containingDeclarationSymbol
            is FirValueParameterSymbol -> symbol.containingDeclarationSymbol
            else -> error("Unsupported receiver expression type.")
        }
        tryResolve(declSymbol)?.let { return it }

        throw IllegalArgumentException("No resolution approach to this symbol worked.")
    }

    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val argument = data.convert(typeOperatorCall.arguments[0])
        val conversionType = data.embedType(typeOperatorCall.conversionTypeRef.coneType)
        return when (typeOperatorCall.operation) {
            FirOperation.IS -> Is(argument, conversionType)
            FirOperation.NOT_IS -> Not(Is(argument, conversionType))
            FirOperation.AS -> Cast(argument, conversionType).withInvariants {
                proven = true
                access = true
            }

            FirOperation.SAFE_AS -> SafeCast(argument, conversionType).withInvariants {
                proven = true
                access = true
            }

            else -> handleUnimplementedElement("Can't embed type operator ${typeOperatorCall.operation}.", data)
        }
    }

    override fun visitAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val function = anonymousFunctionExpression.anonymousFunction
        return LambdaExp(data.embedFunctionSignature(function.symbol), function, data, function.symbol.label!!.name)
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: StmtConversionContext): ExpEmbedding {
        val (catchData, tryBody) = data.withCatches(tryExpression.catches) { catchData ->
            withNewScope {
                val jumps =
                    catchData.blocks.map { catchBlock -> NonDeterministically(Goto(catchBlock.entryLabel.toLink())) }
                val body = convert(tryExpression.tryBlock)
                GotoChainNode(
                    null,
                    Block {
                        addAll(jumps)
                        add(body)
                        addAll(jumps)
                    },
                    catchData.exitLabel.toLink()
                )
            }
        }
        val catches = catchData.blocks.map { catchBlock ->
            data.withNewScope {
                val parameter = catchBlock.firCatch.parameter
                // The value is the thrown exception, which we do not know, hence we do not initialise the exception variable.
                val paramDecl = declareLocalProperty(parameter.symbol, null)
                GotoChainNode(
                    catchBlock.entryLabel,
                    blockOf(
                        paramDecl,
                        convert(catchBlock.firCatch.block)
                    ),
                    catchData.exitLabel.toLink()
                )
            }
        }
        return Block {
            add(tryBody)
            addAll(catches)
            add(LabelExp(catchData.exitLabel))
        }
    }

    override fun visitElvisExpression(
        elvisExpression: FirElvisExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val lhs = data.convert(elvisExpression.lhs)
        val rhs = data.convert(elvisExpression.rhs)
        val expType = data.embedType(elvisExpression.resolvedType)
        return Elvis(lhs, rhs, expType)
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val selector = safeCallExpression.selector
        val receiver = data.convert(safeCallExpression.receiver)
        val expType = data.embedType(safeCallExpression.resolvedType)
        val checkedSafeCallSubjectType = data.embedType(safeCallExpression.checkedSubjectRef.value.resolvedType)

        return share(receiver) { sharedReceiver ->
            If(
                sharedReceiver.notNullCmp(),
                data.withCheckedSafeCallSubject(sharedReceiver.withType(checkedSafeCallSubjectType)) { convert(selector) },
                NullLit,
                expType
            )
        }
    }

    override fun visitCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: StmtConversionContext,
    ): ExpEmbedding = data.checkedSafeCallSubject
        ?: throw IllegalArgumentException("Trying to resolve checked subject $checkedSafeCallSubject which was not captured in StmtConversionContext")

    private fun handleUnimplementedElement(msg: String, data: StmtConversionContext): ExpEmbedding =
        when (data.config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                TODO(msg)

            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                data.errorCollector.addMinorError(msg)
                ErrorExp
            }
        }
}

object StmtConversionVisitorExceptionWrapper : FirVisitor<ExpEmbedding, StmtConversionContext>() {
    override fun visitElement(element: FirElement, data: StmtConversionContext): ExpEmbedding {
        try {
            return element.accept(StmtConversionVisitor, data)
        } catch (e: Exception) {
            data.errorCollector.addErrorInfo("... while converting ${element.source.text}")
            throw e
        }
    }
}
