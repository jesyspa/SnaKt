# SnaKt Code Improvement Suggestions

This document catalogs potential improvements to the SnaKt formal verification plugin codebase, organized by severity and category.

## Table of Contents

1. [Minor Bugs (Immediately Fixable)](#1-minor-bugs-immediately-fixable)
2. [Structural Bugs](#2-structural-bugs)
3. [Potential Refactorings](#3-potential-refactorings)
4. [Missing Features](#4-missing-features)
5. [Conceptual Shortcomings](#5-conceptual-shortcomings)
6. [Documentation and Testing Gaps](#6-documentation-and-testing-gaps)

---

## 1. Minor Bugs (Immediately Fixable)

### 1.1 Incomplete Equality Comparison Implementation

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/StmtConversionVisitor.kt:211`

**Issue:** Equality comparison uses structural comparison instead of calling `equals()` method.

```kotlin
private fun convertEqCmp(left: ExpEmbedding, right: ExpEmbedding): ExpEmbedding {
    //TODO: replace with call to left.equals()
    return EqCmp(left, right)
}
```

**Fix:** Implement proper `equals()` method calling for non-primitive types while maintaining structural comparison for primitives.

---

### 1.2 Poor Error Messages for Unreachable Code

**Files:**
- `formver.compiler-plugin/viper/src/org/jetbrains/kotlin/formver/viper/ast/Info.kt:25`
- `formver.compiler-plugin/viper/src/org/jetbrains/kotlin/formver/viper/ast/Position.kt:22`

**Issue:** Using `TODO("Unreachable")` which doesn't provide context about what went wrong.

```kotlin
else -> TODO("Unreachable")
```

**Fix:** Replace with descriptive error messages:
```kotlin
else -> error("Unexpected Silver AST info type: ${info::class.qualifiedName}")
```

---

### 1.3 Gradle Dependency Comments

**Files:**
- `formver.compiler-plugin/plugin/build.gradle.kts:13`
- `formver.compiler-plugin/core/build.gradle.kts:10`

**Issue:** Unresolved dependency structure questions.

```kotlin
// TODO: figure out how to avoid this dependency
```

**Action Required:** Review dependency graph and document why these dependencies are needed, or refactor to remove them.

---

### 1.4 Missing Test for `repeat` Function

**File:** `formver.compiler-plugin/testData/diagnostics/verification/stdlib_replacement_tests.kt:11`

**Issue:** Known bug in `repeat` function verification with unsatisfied precondition in loops.

```kotlin
// TODO: add test for `repeat` (we actually have a bug there because we require unsatisfied precondition in loops)
```

**Fix:** Add test case and fix the precondition handling in loop verification.

---

## 2. Structural Bugs

### 2.1 Incomplete Purity Checking System

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/purity/ExpPurityVisitor.kt`

**Critical Issues:**

#### 2.1.1 No Support for @Pure Methods (Line 40)
```kotlin
override fun visitMethodCall(e: MethodCall) = false // TODO: Whitelist for annotated methods?
```

**Impact:** Methods annotated with `@Pure` are still treated as impure, preventing their use in specifications.

**Fix:** Implement whitelist mechanism to check for `@Pure` annotation on method declarations.

#### 2.1.2 Field Access Not Implemented (Lines 52-53)
```kotlin
override fun visitFieldAccess(e: FieldAccess): Boolean = false // TODO
override fun visitPrimitiveFieldAccess(e: PrimitiveFieldAccess): Boolean = false // TODO
```

**Impact:** Pure functions cannot access any fields, severely limiting what can be expressed in specifications.

**Fix:** Implement field access purity checking. Reading immutable fields should be considered pure.

---

### 2.2 Inconsistent Error Handling System

**File:** `formver.common/src/org/jetbrains/kotlin/formver/common/ErrorCollector.kt:14`

**Issue:** Ad-hoc error collection with no structured approach.

```kotlin
/** Collector for some plugin errors.
 * We currently are not consistent with what we report this way, vs through other channels.
 * TODO: Replace this with some kind of more systematic approach to generating diagnostics.
 */
class ErrorCollector {
    private val minorErrors = mutableListOf<String>()
    private val purityErrors = mutableListOf<Pair<KtSourceElement, String>>()
```

**Problems:**
- No error taxonomy or severity levels
- Mixing string-based errors with structured errors
- No error codes for programmatic handling
- Inconsistent reporting between `ErrorCollector` and other diagnostic mechanisms

**Fix:** Design a unified diagnostic system with:
- Error codes and categories
- Severity levels (error, warning, info)
- Structured error data classes
- Consistent reporting across all components

---

### 2.3 Resource Management Issues

**File:** `formver.compiler-plugin/plugin/src/org/jetbrains/kotlin/formver/plugin/compiler/ViperPoweredDeclarationChecker.kt`

**Issue:** `Verifier` instances are created but never explicitly closed or cleaned up.

```kotlin
val verifier = Verifier()
// ... use verifier but no cleanup
```

**Risk:** If `Verifier` holds native resources (Z3 process, file handles), this could cause resource leaks.

**Fix:**
- Implement `AutoCloseable` on `Verifier` if it holds resources
- Use `.use { }` blocks for proper resource management
- Document resource lifecycle

---

### 2.4 Unsafe lateinit Variable Usage

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/domains/FunctionBuilder.kt:27`

**Issue:** lateinit vars without initialization checks.

```kotlin
private lateinit var retType: Type

val result: Function
    get() = Function(name, formalArgs.toList(), retType, pres.toList(), posts.toList(), NoInfo)
```

**Risk:** Accessing `result` before calling `returns()` will throw `UninitializedPropertyAccessException`.

**Fix:**
- Make `retType` nullable with explicit check
- Or use a lazy delegate with validation
- Add custom getter that provides better error message

---

### 2.5 Known Bug in Shared Expression Handling

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/Meta.kt:86-89`

**Critical Bug:** Shared expressions with mutable variable references produce incorrect results.

```kotlin
/**
 * This solution has some bugs: if a shared expression references a variable and that variable is modified
 * between the shared parts, the second occurrence may have a different value than the first. We can fix this
 * quite easily by using a fresh variable every time, but that would add unreasonable bloat to many programs.
 * TODO: fix this.
 */
```

**Impact:** Can cause verification to succeed when it should fail, or vice versa. **Soundness issue.**

**Example:**
```kotlin
var x = 5
val result = (x++) ?: 0  // Should evaluate x++ once, but may evaluate twice
```

**Fix:** Introduce fresh variable for shared expressions when they contain non-pure operations. Consider optimization to skip this for provably pure expressions.

**Priority:** **HIGH** - This affects correctness of verification.

---

### 2.6 Purity Check Failures Don't Halt Conversion

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/StmtConversionContext.kt:253`

**Issue:** Conversion continues even after detecting purity violations.

```kotlin
// TODO: Terminate conversion if purity check fails
body.checkValidity(declaration.source, errorCollector)
```

**Impact:** May produce invalid Viper code that confuses the verifier.

**Fix:** Throw exception or return early when purity check fails to avoid cascading errors.

---

## 3. Potential Refactorings

### 3.1 Overly Long Files

Several files exceed 500 lines and would benefit from being split into multiple focused files:

#### 3.1.1 Exp.kt (726 lines)
**File:** `formver.compiler-plugin/viper/src/org/jetbrains/kotlin/formver/viper/ast/Exp.kt`

**Suggestion:** Split into separate files by expression category:
- `ExpLiterals.kt` - Boolean, Int, Null literals
- `ExpArithmetic.kt` - Add, Sub, Mul, Div, Mod
- `ExpComparison.kt` - Lt, Le, Gt, Ge, Eq, Ne
- `ExpLogical.kt` - And, Or, Not, Implies
- `ExpQuantifiers.kt` - ForAll, Exists
- `ExpAccess.kt` - FieldAccess, PredicateAccess
- `ExpPermissions.kt` - AccPred, FieldAccessPredicate

#### 3.1.2 ProgramConverter.kt (631 lines)
**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/ProgramConverter.kt`

**Suggestion:** Extract responsibilities into separate classes:
- `TypeEmbeddingConverter.kt` - Type embedding logic
- `FunctionEmbeddingConverter.kt` - Function signature and body embedding
- `PropertyEmbeddingConverter.kt` - Property and field embedding
- `ProgramBuilder.kt` - Final Viper program assembly
- Keep `ProgramConverter.kt` as orchestrator

#### 3.1.3 StmtConversionVisitor.kt (603 lines)
**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/StmtConversionVisitor.kt`

**Suggestion:** Extract logical groups:
- `OperatorConversionVisitor.kt` - Equality, comparison, boolean operators
- `ControlFlowConversionVisitor.kt` - If, when, while, try-catch
- `CallConversionVisitor.kt` - Function calls, method calls, lambda invocations
- Keep `StmtConversionVisitor.kt` as base with common utilities

#### 3.1.4 ExpEmbedding.kt (556 lines)
**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/ExpEmbedding.kt`

**Suggestion:** Separate linearization concerns from expression structure.

---

### 3.2 Complex Methods

#### 3.2.1 ProgramConverter.embedFullSignature()
**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/ProgramConverter.kt:353-459`

**Issue:** 106-line method doing multiple things.

**Refactoring:**
```kotlin
// Extract to separate methods:
private fun extractFunctionContract(declaration: FirFunction): ContractInfo
private fun buildFunctionParameters(declaration: FirFunction): List<ParameterEmbedding>
private fun buildPreconditions(contract: ContractInfo): List<ExpEmbedding>
private fun buildPostconditions(contract: ContractInfo): List<ExpEmbedding>
private fun embedFullSignature(...) {
    // Orchestrate the above
}
```

#### 3.2.2 StmtConversionVisitor.visitFunctionCall()
**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/StmtConversionVisitor.kt:287-314`

**Issue:** Special-case handling for `forAll` embedded in general function call logic.

**Refactoring:**
```kotlin
override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext): ExpEmbedding {
    return when {
        isForAllCall(functionCall) -> convertForAllCall(functionCall, data)
        else -> convertRegularFunctionCall(functionCall, data)
    }
}
```

---

### 3.3 Code Duplication

#### 3.3.1 Name Embedding Patterns
**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/names/NameEmbeddings.kt:107-120`

**Issue:** Repetitive scoping logic for getter/setter name embedding.

**Fix:** Extract common scoping function:
```kotlin
private fun <T> embedWithScope(
    symbol: FirBasedSymbol<*>,
    suffix: String?,
    builder: (ScopedKotlinName) -> T
): T
```

#### 3.3.2 Invariant Building Duplication
**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/types/ClassEmbeddingDetails.kt:46-78`

**Issue:** Similar patterns for shared vs unique predicate building.

**Fix:** Extract common predicate builder with strategy pattern for shared/unique differences.

---

### 3.4 Poor Separation of Concerns

#### 3.4.1 StmtConversionContext Mixing Concerns
**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/StmtConversionContext.kt:74-86`

**Issue:** Context class contains both state management and factory methods.

**Refactoring:**
- Extract `DeclarationFactory` for creating variable/property declarations
- Extract `ScopeManager` for scope management
- Keep `StmtConversionContext` for coordinating these components

---

### 3.5 Excessive Mutable State

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/ProgramConverter.kt:54-62`

**Issue:** 5 mutable maps make initialization order and thread-safety unclear.

```kotlin
private val typeEmbeddings = mutableMapOf<ClassId, ClassTypeEmbedding>()
private val properties = mutableMapOf<ScopedKotlinName, PropertyEmbedding>()
private val functions = mutableMapOf<ScopedKotlinName, CallableEmbedding>()
private val fields = mutableListOf<Field>()
private val methods = mutableMapOf<ScopedKotlinName, RichCallableEmbedding>()
```

**Refactoring:**
- Use builder pattern with immutable data structures
- Or make initialization phases explicit (collect, then build)
- Document initialization invariants

---

## 4. Missing Features

### 4.1 Pure Function Body Conversion

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/ProgramConverter.kt:124`

**Issue:** Pure function bodies are stubbed out with `NullLit`.

```kotlin
// TODO: Replace the empty body with the actual expression representation of the function body
embedPureUserFunction(declaration.symbol, signature).apply {
    body = Exp.NullLit()
}
```

**Impact:** Cannot verify pure functions by inlining their bodies into specifications.

**Priority:** Medium - useful for verification but workaround exists (write explicit contracts).

---

### 4.2 Lambda Storage

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/LambdaExp.kt:34`

**Issue:** Cannot store lambdas in variables for later use.

```kotlin
override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
    TODO("create new function object with counter, duplicable (requires toViper restructuring)")
}
```

**Impact:** Cannot verify higher-order functions that store lambdas.

**Priority:** Medium - limits expressiveness of verifiable code.

---

### 4.3 ForAll Quantifier Limitations

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/ForAllEmbedding.kt`

#### 4.3.1 Single Variable Only (Line 19)
```kotlin
// TODO: support multiple variables
val variable: VariableEmbedding
```

**Impact:** Cannot write specifications like `forAll { x, y -> x + y == y + x }`

#### 4.3.2 No User-Defined Triggers (Line 28)
```kotlin
// TODO: right now we hope that Viper will infer triggers successfully, later we might enable user triggers here
triggers = emptyList()
```

**Impact:**
- Automatic trigger inference may fail, causing incomplete verification
- Performance issues with complex quantifiers
- No control over instantiation patterns

**Priority:** Medium-High - affects verification completeness and performance.

---

### 4.4 Loop Invariants Support

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/ControlFlow.kt`

#### 4.4.1 Viper Version Workaround (Line 107)
```kotlin
// TODO: this logic can be rewritten back to invariants once the version of Viper is updated
```

**Action:** Update Viper version and restore proper invariant handling.

#### 4.4.2 Missing Invariants (Line 115)
```kotlin
// TODO: add invariants
```

**Context:** Some loop structures don't properly support invariants.

---

### 4.5 Cast Safety Checks

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/TypeOp.kt:55`

**Issue:** No runtime type assertion before casting.

```kotlin
// TODO: Do we want to assert `inner isOf type` here before making a cast itself?
override fun toViper(ctx: LinearizationContext) = inner.toViper(ctx)
```

**Impact:** Unsafe casts could lead to unsound verification if not checked elsewhere.

**Priority:** High - potential soundness issue.

---

### 4.6 Inline Function Specification Design

**File:** `formver.compiler-plugin/testData/diagnostics/verification/user_invariants/simple_precondition.kt:30`

**Issue:** No design for inlining functions with specifications.

```kotlin
// TODO: come up with a proper design for inlining functions with specifications
```

**Impact:** Cannot verify code that uses inline functions with contracts.

**Priority:** Medium - inline functions are common in Kotlin.

---

### 4.7 Uniqueness Checks Disabled

**File:** `formver.compiler-plugin/cli/src/org/jetbrains/kotlin/formver/cli/FormalVerificationPluginComponentRegistrar.kt:39`

**Issue:** Uniqueness checking system exists but is not exposed in configuration.

```kotlin
// TODO: provide configuration to enable uniqueness checks
```

**Action:** Add configuration option and document usage of `@Unique` and `@Borrowed` annotations.

---

## 5. Conceptual Shortcomings

### 5.1 Type System Limitations

#### 5.1.1 No Generic Type Parameters

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/types/ClassTypeEmbedding.kt:14`

**Issue:** Type embeddings don't support generics.

```kotlin
// TODO: incorporate generic parameters.
data class ClassTypeEmbedding(override val name: ScopedKotlinName) : PretypeEmbedding
```

**Impact:**
- Cannot verify generic classes like `List<T>`, `Map<K,V>`
- Cannot verify generic functions
- Severely limits what code can be verified

**Priority:** **High** - fundamental limitation of the verification system.

**Suggested Approach:**
- Encode type parameters in Viper using domains
- Use Viper's type system or axiomatize type relationships
- Consider monomorphization for simple cases

---

#### 5.1.2 Return Type Boxing

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/callables/FullNamedFunctionSignature.kt:100`

**Issue:** All function return types are boxed into `Ref` type.

```kotlin
// TODO: Be explicit about the return types of functions instead of boxing them into a Ref
```

**Problems:**
- Inefficient encoding in Viper
- Unclear semantics for primitive types
- Makes reasoning about types more complex

**Fix:** Use Viper's type system directly where appropriate, only box when necessary (e.g., polymorphism).

---

### 5.2 Architectural Design Issues

#### 5.2.1 Permission Model Inconsistency

**Files:** Multiple locations
- `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/ProgramConverter.kt:404`
- `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/ControlFlow.kt:288`

**Issue:** Unclear whether to use `inhale` vs `require` for permissions.

```kotlin
// TODO (inhale vs require) Decide if `predicateAccessInvariant` should be required rather than inhaled
```

**Impact:**
- Inconsistent permission handling could lead to unsound verification
- `inhale` is for assumptions, `require` is for checked preconditions
- Confusion about when to use each

**Fix:** Establish clear guidelines:
- Use `require` for user-specified preconditions (checked at call site)
- Use `inhale` for internal invariants (trusted assumptions)
- Document the decision and apply consistently

---

#### 5.2.2 Function Header Design

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/callables/RichCallableEmbedding.kt:19`

**Issue:** Unclear separation between function declaration and definition.

```kotlin
/**
 * TODO: The Function header will likely be removed as it makes no sense to emit a function header on its own
 */
```

**Fix:** Clarify model:
- Functions with bodies → Viper methods
- Pure functions without bodies → Viper functions
- Abstract functions → Viper functions with axioms

---

#### 5.2.3 Conversion Phase Separation

**Files:**
- `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/ProgramConverter.kt:416,438`

**Issue:** Contract/specification conversion happens inline with other conversions.

```kotlin
// TODO: this process should be a separate step in the conversion.
```

**Problems:**
- Mixed concerns make code hard to understand
- Can't validate specifications before trying to convert them
- Difficult to add optimization passes

**Suggested Phases:**
1. **Resolution:** Resolve all symbols and types
2. **Contract Extraction:** Parse and validate all specifications
3. **Type Embedding:** Build type representations
4. **Function Embedding:** Build function signatures
5. **Body Conversion:** Convert function bodies
6. **Viper Generation:** Generate final Viper AST

---

#### 5.2.4 Name Building System

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/names/ScopedKotlinNameBuilder.kt:64`

**Issue:** Ad-hoc name construction with potential collision risks.

```kotlin
// TODO: generalise this to work for all names.
fun buildName(init: ScopedKotlinNameBuilder.() -> KotlinName): ScopedKotlinName
```

**Risks:**
- Name collisions between generated and user code
- Unclear scoping rules
- Hard to debug mangled names

**Fix:**
- Implement systematic name mangling scheme
- Add validation to detect collisions
- Improve debug output for names

---

#### 5.2.5 Type Builder Hack

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/types/PretypeBuilder.kt:109`

**Issue:** Workaround to reuse existing type embeddings.

```kotlin
// TODO: ensure we can build the types with the builders, without hacks like this.
class ExistingPretypeBuilder(val embedding: PretypeEmbedding) : PretypeBuilder
```

**Fix:** Redesign builder API to properly handle both new and existing types.

---

### 5.3 Scalability Concerns

#### 5.3.1 No Caching Strategy

**Impact:**
- Type embeddings recomputed for each usage
- Function signatures rebuilt multiple times
- Property accesses not memoized

**Consequence:** Performance degrades on large codebases.

**Fix:**
- Add memoization for expensive computations
- Cache type embeddings by `ClassId`
- Cache function signatures by symbol

---

#### 5.3.2 Not Thread-Safe

**Issue:** Mutable state in `ProgramConverter` and other classes prevents parallel processing.

**Fix:**
- Use immutable data structures
- Or make conversion process explicitly single-threaded with documentation
- Consider using actors/coroutines for parallelism if needed

---

#### 5.3.3 No Error Recovery

**Issue:** First error stops entire conversion, even for independent declarations.

**Suggested Improvement:**
- Continue conversion after errors in individual functions
- Report all errors at once
- Allow partial verification results

---

### 5.4 Documentation and Naming

#### 5.4.1 Unclear "Pretype" vs "Type"

**Files:**
- `PretypeEmbedding.kt`
- `TypeEmbedding.kt`

**Issue:** The distinction between "pretype" and "type" is not documented.

**Action:** Add comprehensive documentation explaining:
- What is a pretype vs a type?
- When to use each?
- How do they relate?

---

#### 5.4.2 Name Scope Hack

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/names/NameScope.kt:20`

**Issue:** Documented as a hack.

```kotlin
// This is a hack required by how we deal with public names.
```

**Action:** Document why this is needed and what proper solution would look like.

---

#### 5.4.3 Property Lookup Safety

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/conversion/StmtConversionContext.kt:99`

**Issue:** Uncertain if property parent lookup is safe.

```kotlin
// TODO: decide if we leave this lookup or consider it unsafe.
fun FirPropertySymbol.findFinalParentProperty()
```

**Action:** Research and document when this lookup could fail and how to handle it.

---

## 6. Documentation and Testing Gaps

### 6.1 Missing Documentation

#### 6.1.1 Architecture Overview
- No high-level design document
- Unclear compilation pipeline
- No diagram showing component relationships

#### 6.1.2 Conversion Algorithm
- Complex algorithms (e.g., sharing context) lack explanation
- No examples showing Kotlin → Viper transformations
- Missing formal specification of translation

#### 6.1.3 Extension Points
- No guide for adding new expression types
- No documentation on adding new annotations
- No explanation of embedding architecture

---

### 6.2 Test Coverage Gaps

#### 6.2.1 Missing Test Categories
- **Error recovery tests:** What happens when conversion fails?
- **Resource cleanup tests:** Are resources properly released?
- **Negative tests:** Do TODOs properly reject unsupported features?
- **Integration tests:** End-to-end verification of real code
- **Performance tests:** Scalability on large codebases

#### 6.2.2 Unstable Tests

**File:** `formver.compiler-plugin/testData/diagnostics/verification/expensive_diagnostics/z_function.kt:3`

```kotlin
// TODO: make verification of this procedure stable.
```

**Issue:** Complex verification tests are unstable, possibly due to:
- Non-deterministic SMT solver behavior
- Timeout issues
- Viper version incompatibilities

**Action:** Investigate root cause and either fix or document limitations.

---

### 6.3 User-Facing Documentation

#### 6.3.1 Incomplete README
The README covers basic setup but misses:
- Troubleshooting guide
- Supported Kotlin features (what works, what doesn't)
- Examples of verified code
- Performance characteristics and limitations
- Migration guide for updates

#### 6.3.2 No Tutorial
Missing:
- Step-by-step guide to writing first verified function
- Common patterns and idioms
- How to debug verification failures
- Best practices for writing specifications

#### 6.3.3 Annotation Documentation
Annotations are defined but lack:
- Detailed semantics of each annotation
- Examples showing usage
- Interaction between annotations
- Performance implications

---

## 7. External Dependencies

### 7.1 Hardcoded Z3 Version

**File:** `README.md:118`

**Issue:** Requires specific Z3 version 4.8.7 (released 2018).

**Concerns:**
- Very old version (7+ years old)
- Missing security updates
- Missing performance improvements
- May not work on newer OS versions

**Action:**
- Test with newer Z3 versions
- Document compatibility matrix
- Consider supporting multiple versions

---

### 7.2 Viper Version Coupling

**File:** `formver.compiler-plugin/core/src/org/jetbrains/kotlin/formver/core/embeddings/expression/ControlFlow.kt:107`

**Issue:** Code contains workarounds for Viper version limitations.

**Action:**
- Identify minimum required Viper version
- Test with latest Viper version
- Update dependencies if newer versions fix issues

---

### 7.3 Kotlin Version Lock

**Issue:** Tightly coupled to Kotlin 2.2.0.

**Concerns:**
- Need to track Kotlin compiler API changes
- May break on Kotlin updates

**Action:**
- Document supported Kotlin version range
- Set up CI to test against multiple Kotlin versions
- Consider version compatibility strategy

---

## Summary and Priorities

### Critical (Fix Immediately)
1. ⚠️ **Shared expression mutable variable bug** (Meta.kt:86) - Soundness issue
2. ⚠️ **Cast safety checks** (TypeOp.kt:55) - Potential soundness issue
3. ⚠️ **Purity checking for @Pure methods** (ExpPurityVisitor.kt:40) - Blocks user code

### High Priority
4. Generic type parameter support (ClassTypeEmbedding.kt:14)
5. Field access purity checking (ExpPurityVisitor.kt:52-53)
6. Systematic error handling (ErrorCollector.kt:14)
7. ForAll multi-variable and trigger support (ForAllEmbedding.kt:19,28)

### Medium Priority
8. Code organization refactoring (split large files)
9. Pure function body conversion (ProgramConverter.kt:124)
10. Lambda storage support (LambdaExp.kt:34)
11. Inline function specification design
12. Documentation improvements

### Low Priority
13. Performance optimizations (caching, parallelism)
14. Better error messages
15. Code cleanup (remove TODOs with clear decisions)
16. Naming improvements

---

## Contributing

When addressing these suggestions:

1. **For bugs:** Write a failing test first, then fix
2. **For refactorings:** Ensure existing tests still pass
3. **For missing features:** Add tests demonstrating the new capability
4. **For documentation:** Include examples where appropriate
5. **Update this document:** Mark items as complete or add new findings

---

**Document Generated:** 2025-11-17
**Codebase Version:** Based on commit `ca5aea3`
**Reviewer:** Automated code analysis
