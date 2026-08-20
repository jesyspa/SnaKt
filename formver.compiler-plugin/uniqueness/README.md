# Uniqueness Module

This module implements the `@Unique` checker for the SnaKt compiler plugin (`formver.compiler-plugin:uniqueness`).

## Scope

The module is responsible for:
- interpreting `@Unique` as a FIR type attribute
- resolving declared uniqueness of symbols/expressions
- tracking the uniqueness state across the control flow of a function
- reporting uniqueness diagnostics (mismatch, use-after-move, escaping/leaking moved subpaths, unique-argument collisions)

`@Borrowed` behavior is provided by the locality module and consumed here to restore borrowed values after calls.

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
- `TypeRefUniquenessAttributeChecker.kt` restricts valid annotation targets to local variables and function parameters.

### Path models

- Paths are represented as symbol sequences (`Path.kt`).
- Both access information and uniqueness state are represented by tries (`PathTrie.kt`):
  - `AccessState = PathTrie<Access>`
  - `UniquenessState = PathTrie<Uniqueness>`
- `ExpressionAccessStateResolver.kt` extracts the paths touched by each expression.
- A trie node can be a **summary node**: a leaf whose value is the join of everything that would otherwise be below it, standing for both its own prefix and every path under it. A lookup or update that would descend past a summary node lands on the node itself instead. `UniquenessState.summarizeRecursivePaths` collapses any path that revisits a symbol into a summary, which bounds the trie's depth by the number of distinct symbols a function mentions — the mechanism that keeps recursive types from growing the trie without bound.

### CFG uniqueness state analysis

`GraphUniquenessStatesAnalyzer.kt` computes a fixed point over the following CFG nodes. The outgoing state of every node is normalized by `UniquenessState.summarizeRecursivePaths` before it is recorded, so recursion through a symbol never deepens the trie past a summary node — this is what makes the fixed point terminate over recursive types.

- **Initialization**
  - The initial state contains the uniqueness information of the function's parameters.

- **Variable declaration/assignment**
  - Project RHS' uniqueness substate against the incoming state.
  - Move RHS access paths, also against the incoming state.
  - Insert the projected substate into LHS' path.
  - Initialize LHS path to declared uniqueness.
  - The order matters when the LHS is a prefix of an RHS path: advancing a cursor with `current = current.next` moves the field out of the node the cursor is leaving, and overwriting `current` then drops that mark, because it describes a location the cursor no longer reaches.

- **Function-call enter**
  - Move all receivers (including context receivers) and value arguments.

- **Function-call exit**
  - Re-initialize arguments/receiver whose required locality is local (`@Borrowed`).

- **Return / throw**
  - Move the escaped expression paths.

- **Merges**
  - Join incoming states path-wise.

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
- recursive data structures: linked lists and binary trees, walked both by loop and by recursion

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

- **Summary nodes are approximate**
  - A summary node stands for a whole region of paths rather than one path, so operations that reach past it apply to that entire region. An access past a summary lands on the summary itself, so a read there is treated as touching everything it covers. Restoring a borrow through a summary likewise restores the whole summarized region, not just the one path that was borrowed.
  
