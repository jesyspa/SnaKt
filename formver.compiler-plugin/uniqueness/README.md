# Uniqueness Module

This module implements the `@Unique` checker for the SnaKt compiler plugin (`formver.compiler-plugin:uniqueness`).

## Scope

The module is responsible for:
- interpreting `@Unique` as a FIR type attribute
- resolving declared uniqueness of symbols/expressions
- tracking the uniqueness state across the control flow of a function
- reporting uniqueness diagnostics (mismatch, use-after-move, escaping/leaking moved subpaths, unique-argument collisions)

`@Borrowed` behavior is provided by the locality module and consumed here to restore borrowed values after calls.

## Wiring

`FormalVerificationPluginExtensionRegistrar` registers `UniquenessAttributeExtension` and this module's
resolvers unconditionally, so a `@Unique` type carries `UniquenessAttribute` in every run of the plugin.
Conversion relies on that: `ProgramConverter` and `SignatureCreation` ask a symbol's type for its
`scopeUniqueness` — and for its `locality`, in the case of `@Borrowed` — rather than reading the annotations
themselves.

The diagnostics are separate and on by default. `PluginAdditionalCheckers` adds the checkers below only when
`PluginConfiguration.checkUniqueness` is set, which the `check_uniqueness` compiler option controls; unsetting
it leaves `@Unique` in the Viper encoding with nothing validating it.

## Strategy

The checker tracks two kinds of uniqueness:

1. **Declared uniqueness (flow-insensitive)**
   - Tracks uniqueness requirements from types and declarations.
   - Used by type-like assignment/call/return/throw compatibility checks.

2. **State uniqueness (aka "actual" uniqueness)**
   - Tracks which concrete access paths are moved at each CFG point.
   - Used for use-after-move and consistency checks (escape/exit).

## Core Mechanism

### Uniqueness attribute and attribute checking

- `UniquenessAttributeExtension.kt` maps the `@Unique` annotation to `UniquenessAttribute`.
- `Uniqueness.kt` defines the actual uniqueness lattice:
  - `Unique < Unknown < Shared < Moved`
- `Uniqueness.kt` also reads a type's attributes into that lattice:
  - `scopeUniqueness` — what a value of the type is assumed to be: `Unique` for `@Unique`, `Unknown` for
    `@Borrowed`, `Shared` otherwise.
  - `parameterUniqueness` — what a parameter declared with the type requires.
- `@Unique` applies to types only (`AnnotationTarget.TYPE`). `TypeRefUniquenessAttributeChecker.kt` narrows
  that further to the type positions the analysis understands: value parameters, receiver parameters,
  properties, function return types, and implicit type arguments.

### Path models

- Paths are represented as symbol sequences (`Path.kt`).
- Both access information and uniqueness state are represented by tries (`PathTrie.kt`):
  - `AccessState = PathTrie<Access>`
  - `UniquenessState = PathTrie<Uniqueness>`
- `ExpressionAccessStateResolver.kt` extracts the paths touched by each expression.

### CFG uniqueness state analysis

`GraphUniquenessStatesAnalyzer.kt` computes a fixed point over the following CFG nodes:

- **Initialization**
  - The initial state contains the uniqueness information of the function's parameters.

- **Variable declaration/assignment**
  - Project RHS' uniqueness substate into LHS' path.
  - Initialize LHS path to declared uniqueness.
  - Move RHS access paths.

- **Function-call enter**
  - Move all receivers (including context receivers) and value arguments.

- **Function-call exit**
  - Re-initialize arguments/receiver whose required locality is local (`@Borrowed`).

- **Return / throw**
  - Move the escaped expression paths.

- **Merges**
  - Join incoming states path-wise.

Both call nodes are skipped for a call the analyzer's `UniquenessNeutralCallPredicate` accepts, leaving the
state untouched. The plugin supplies `isSpecificationCall`, which holds for the `@SpecificationHelper`
specification DSL (`verify`, `preconditions`, `old`, `fold`, `UniquePred`, and the rest), so a specification
neither moves nor mutates what it mentions. Those functions take their arguments `@Borrowed`, so what a
specification mentions does not escape either.

### Diagnostics

Checkers consume the above analyses:

- Type-compatibility checkers (`UniquenessTypeCheckers.kt`)
  - assignment/call/qualified-access/return/throw mismatches
- `FunctionUseAfterMoveChecker.kt`
  - reports `INVALID_MOVED_ACCESS`
- `FunctionEscapeUniquenessConsistencyChecker.kt`
  - reports moved subpaths on escaping values
- `FunctionExitUniquenessConsistencyChecker.kt`
  - reports moved subpaths left in borrowed locals at function exit
- `ExpressionArgumentUniquenessCollisionChecker.kt`
  - reports duplicate/overlapping unique arguments in one call

The three flow-sensitive function checkers are registered through `asSpecificationAware()`
(`SpecificationAwareChecker.kt`, in the plugin module), which suppresses them inside the arguments of a
specification call.

## Current Test Coverage

Uniqueness diagnostics are primarily covered by:

`formver.compiler-plugin/testData/diagnostics/uniqueness_checker/`

Current scenarios include:
- annotation targeting
- local/property assignments
- call argument checking and collisions
- aliasing behavior
- receiver/context parameter behavior
- return/throw escape consistency
- loops, `when`, `try/catch`, nullable flows
- constructor/operator cases

## Current Limitations / Untested Behavior

Known limitations in current code/tests:

- **Caught `throw` handling is conservative**
  - Throw expressions are treated as escapes/exits even when locally caught.
  - See TODOs in:
    - `FunctionEscapeUniquenessConsistencyChecker.kt`
    - `FunctionExitUniquenessConsistencyChecker.kt`
  - Also documented in `uniqueness_checker/throw.kt`.

- **Invoke-call uniqueness contracts**
  - `ExpressionArgumentUniquenessesMapper.kt` currently uses `TODO: Implement uniqueness contract resolution`.
  - This means that the parameters of higher-order functions cannot be specified as `@Unique`.

- **Property path receiver support**
  - `QualifiedAccessPathReceiverResolver.kt` currently handles only property accesses with backing fields through dispatch receiver.

- **Interaction with closures** 
  - The current `GraphUniquenessStatesAnalyzer.kt` visits the lambda subgraphs as if they were part of the local control flows, which can result in unexpected behavior.

- **Reading a moved value inside a specification**
  - Whether this is reported depends on the shape of the DSL entry point. `verify(a === b)` evaluates its
    argument in the enclosing function's flow, so a moved `a` is reported; `preconditions { a.n == 0 }` puts it
    in a lambda that `asSpecificationAware()` stands the checker down on, so nothing is reported.
  - Both behaviours are pinned by `uniqueness_checker/specification.kt`.
