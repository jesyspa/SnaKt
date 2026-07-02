# F6 (`exists<T>`) — Consolidated Solver Feedback

Synthesis of three independent solver runs (A/opus, B/sonnet, C/haiku) against
VerifyThis problems using the new `exists<T>` quantifier. This document drives
the F6 debug step (Step 5).

Sources: `solver-a.md@solver-a`, `solver-b.md@solver-b`, `solver-c.md@solver-c`
in `komiputer/snakt-verifythis`.

---

## 1. Outcome table

| Problem | Solver(s) | Result | One-line cause |
| --- | --- | --- | --- |
| 2011 TwoEq (one duplet) | A | **verified** | nested `exists`/`forAll` + witness-carrying invariant is `exists`' sweet spot |
| 2011 MaxElim | B | **verified** | scalar-shadow witness (`s[i]==mval`) + invariant mirroring the postcondition |
| 2022 Downsampling (occupancy core) | B | **verified** (reduced scope) | direct top-level index witness gives an auto-triggered iff; full 3D-real problem inexpressible |
| 2025 Minimum Excludant | A | **near-miss** (1 err) | inductive step needs `Char+Int` successor axiom (uninterpreted `addCharInt`) |
| 2015 Relaxed Prefix | B | **partial** (core verifies, postcondition 1 err) | `exists k` split-point won't instantiate (no trigger; hints don't rescue) |
| 2017 Max-sum subarray (Kadane) | A | **blocked** | element arithmetic (`sum`/`Char-Char`) unsupported in pure spec context |
| 2011 MaxElim | C | **blocked** | translator crash — *misdiagnosed* as "nested quantifiers fail" (see §3) |
| 2011 TwoEq | C | **blocked** | same translator crash — *misdiagnosed* (see §3) |

Net: 3 verified, 1 near-miss, 1 partial, 1 hard-blocked (Kadane), 2 spurious
blocks (C, same crash A/B isolated and worked around).

---

## 2. Points of agreement

Confirmed by two or more solvers:

- **`exists` works for the direct-index witness-carrying-invariant idiom.**
  A (TwoEq), B (MaxElim, Downsampling) all verify `exists` when the bound
  variable directly indexes a `String` at the top level of the quantifier body
  and a loop invariant restates the *same* quantified shape bounded by the
  loop's progress variable. B's Downsampling occupancy check even verifies an
  **iff** (both directions auto-triggered).
- **Nested `exists` + `forAll` translate and verify.** A verified nested
  `exists.exists` (TwoEq) and `forAll<Char> ⇒ exists<Int>` (mex entry);
  B verified `exists` in loop-invariant position alongside `forAll` maximality
  clauses (MaxElim). This directly refutes C's root-cause claim.
- **Track the "current best" with an explicit scalar shadow, not a second
  index.** B's fix (`mval = s[x]`, compare `s[i] == mval`) is the reliable
  idiom; re-indexing the same String with a free variable (`s[i] == s[res]`)
  triggers the crash in §4.1.
- **Always `clean` before trusting the Viper-error grep.** A and B both hit
  Gradle UP-TO-DATE caching reporting `0` errors on an unchanged/incrementally-
  built tree. Use `clean build` (or `--rerun-tasks`).
- **`--no-daemon` is needed across shared worktrees.** A documents daemon
  contention (sibling `--stop` killing builds, corrupting the Kotlin
  incremental cache); B corroborates the caching half. Recommend HOWTO adopt
  `--no-daemon` and forbid `--stop`.
- **`String` is the modelling vehicle; element *values* are the wall.**
  `.length`, indexing, and `Char ==` are pure/quantifier-safe. Any arithmetic
  on element values is not (§4.3).

---

## 3. Conflict, flagged explicitly

**C claims:** "Nested quantifiers (`forAll`/`exists` inside another) are
fundamentally unsupported — both problems are BLOCKED by quantifier support."

**A and B refute this directly:** A verified nested `exists.exists` and
`exists`-inside-`forAll` on the *same problem C blocked* (2011 TwoEq); B
verified nested `exists`/`forAll` on the *other problem C blocked* (2011
MaxElim). Nesting is not the issue.

**Resolution — C misdiagnosed.** C's actual failures are instances of the
single translator crash A and B independently isolated: the generic
`kotlin.NotImplementedError: An operation is not implemented: Unreachable`
raised from `Info.fromSilver` (`viper/.../ast/Info.kt:25`, an `else -> TODO`).
C saw this opaque "Unreachable" message, could not map it to a cause, and
over-generalised to "nested quantifiers fail," then removed both solution files.
The correct reading: C hit issue §4.1 (likely via the `s[i]==s[res]`
double-index shape B bisected, or a contradictory/undischarged clause as A
found) and mistook a crashing error-reporting path for a semantic limitation.
Both of C's problems are in fact **verifiable** — A did TwoEq, B did MaxElim.

C's process lesson stands and generalises: the "Unreachable" crash gives no
actionable signal, which is *why* it must be fixed (§4.1).

---

## 4. Prioritised issues

### 4.1 — Translator crashes instead of failing verification gracefully  ·  IN-SCOPE  ·  Severity: HIGH

**Symptom.** Certain `exists`-bearing specs raise
`kotlin.NotImplementedError: An operation is not implemented: Unreachable`
(BUILD FAILED) instead of a normal "might not hold" Viper warning.

**Root cause (confirmed).** All three solvers' crashes converge on one node:
`Info.fromSilver` at
`formver.compiler-plugin/viper/src/.../ast/Info.kt:25` — an `else -> TODO("Unreachable")`.
It is reached from at least two entry paths:
- `FormattedError.lookupSourceRole` → `reportVerificationError` (B's trace) —
  the error-*reporting* path, hit when Silicon reports a failure whose Viper AST
  node this branch doesn't handle.
- `ViperPoweredDeclarationChecker` (A's trace).

So this is one missing case in the error formatter, surfaced by several
triggers — not several bugs.

**Minimal repros.**
- B#1 (cleanest): `exists<Int> { i -> 0 <= i && i < s.length && s[i] == s[res] }`
  — indexing the *same* String at two distinct variables under one `exists`.
  Reproducible in both postcondition and loop-invariant position, *regardless of
  whether the assertion is actually provable* (B added a supporting invariant;
  still crashed). Contrast: `exists<Int> { i -> i == res }` (no indexing) fails
  with a *normal* warning. So the trigger is the AST shape, not proof failure.
  (`vt2011/MaxElim.kt` bisection.)
- A#5: a deliberately-false postcondition clause also crashes here (found while
  confirming TwoEq's verifier was live).
- C's blocker: same crash, misread as "nested quantifiers" (§3).

**Why fix first.** It is the single highest-leverage item: it directly caused C
to spuriously abandon two verifiable problems, and it poisons the debugging
experience for everyone (an opaque `TODO` where a Viper verification error
belongs). Likely a small, localised fix (handle the offending Silver node /
source-role in `Info.fromSilver` and the `lookupSourceRole` path).

### 4.2 — `exists` won't instantiate when the bound var is only a nested-`forAll` boundary  ·  IN-SCOPE  ·  Severity: MEDIUM–HIGH

**Symptom.** `exists k. (forAll j. P) && (forAll j. Q)` — where `k` appears only
as a *bound* for the inner `forAll`s and never as a direct index at the top
level — gets no automatic instantiation, even when the witness is trivially true
(vacuous ranges at `i=0`).

**Minimal repro.** B's Relaxed Prefix postcondition/loop invariant
(`vt2015/RelaxedPrefix.kt`). B exhausted the manual rescues and *none* worked:
(1) bare invariant fails "on entry" at `i=0` where `k=0` is the only witness and
both `forAll`s are vacuous; (2) adding a trigger-anchor conjunct
(`pat[k]==pat[k]`) changed the emitted Viper trigger but still failed — no ground
term at that program point; (3) planting `verify(pat[0]==pat[0])` /
`verify(pat[removedAt]==pat[removedAt])` ground terms before the loop / before
`return` still failed identically. Conclusion: Silicon cannot bridge "the two
`forAll`s hold for the *concrete* `removedAt`" to "therefore `exists k`," and the
DSL exposes no witness/instantiation hint to force it.

**Adjacency.** A's mex near-miss is the milder, related case: there the outer
`forAll<Char> ⇒ exists<Int>` *does* translate and its entry verifies (the
`exists<Int>` there is a direct-index witness); mex is blocked by §4.3, not by
this. So this issue is specifically about `exists` over a *split-point / boundary*
variable feeding nested universals — a very common VerifyThis pattern
(relaxed matching, RLE, partitioning).

**Options for the debug step.** Emit a default trigger for the boundary case, or
expose an explicit witness/trigger hint in the DSL (Dafny-style `{:trigger}` or
`assert exists x :: P(x) witness w`). The DSL currently has no such construct —
which is exactly the workaround that rescued half the VerifyThis 2015 teams on
this problem. Non-trivial; scope carefully (see §5).

### 4.3 — Element *arithmetic* in a pure spec context  ·  ADJACENT (not `exists`)  ·  Severity: HIGH for affected problems

**Symptom / repros.**
- `Char + Int` lowers to an uninterpreted `addCharInt(r, intToRef(1))` with **no
  successor/code-point axiom**, so `charFromRef(addCharInt(r,1))` is unrelated to
  `charFromRef(r)+1`. This is exactly what sinks mex's minimality induction
  (`vt2025/MinimumExcludant.kt:54`, "might not be preserved"). A confirmed via
  `short_viper_dump`.
- `Char.code`, `Char - Char`, and comparing `s[i].code` to an `Int` all fail
  identically with
  `PureLinearizer ... freshAnonVar is not supported in a pure context`
  (A's Kadane probe; C hit `.code` too). No `sum` primitive exists, and the
  prefix-sum reformulation needs `Char - Char`, so **Kadane is hard-blocked**.

**Scope call.** Not an `exists` bug — `exists` translates fine here; the wall is
integer arithmetic on sequence elements. Defer to a dedicated feature (an
`addCharInt` successor axiom, a pure `Char.code`/`Char(Int)` bridge, or a native
bounded-integer sequence type).

### 4.4 — Other adjacent gaps  ·  ADJACENT  ·  Severity: LOW–MEDIUM

- **`forAll<Char>`/`exists<Char>` are lowered to an *unbounded* `forall x:Int`** —
  bound var not constrained to `[0, 65536)`, so without a manual `' ' <= c` guard
  the clause ranges over negative code points and fails on entry. SnaKt should
  emit the char-range bound automatically. (A, mex edge #1.) Small, self-contained;
  arguably worth doing under F6 since it directly bites an `exists<Char>` user.
- **Top-level `const val` in a spec crashes translation** with
  `Property <NAME> not found in scope. Please report this` — must inline literals.
  (A, mex edge #2.) Adjacent; low priority.
- **Helper-function calls in specs unsupported** (C's `countChar`). Consistent
  with pure-context restrictions; adjacent.

### 4.5 — Tooling (environment, not code)  ·  OUT-OF-SCOPE for code fix  ·  Severity: LOW (doc fix)

- Gradle `compileKotlin` UP-TO-DATE emits *no* warnings, so the HOWTO grep
  recipe silently reports `0` on an unchanged tree (A, B). Fix: HOWTO must say
  `clean build` (or `--rerun-tasks`) before trusting the grep.
- Shared Gradle daemon corruption under concurrent worktrees (A). Fix: HOWTO
  adopt `--no-daemon`; do not `--stop`.
- (Non-SnaKt) a NUL byte from a literal `' '` in a written file made `grep` treat
  the source as binary and suppress output (A). Environmental; note only.

---

## 5. Recommendation for the debug step

**Fix now under F6, in rank order:**

1. **§4.1 — the `Info.fromSilver` "Unreachable" crash.** Highest leverage,
   smallest blast radius. It spuriously blocked an entire solver (C) on two
   problems that are actually verifiable, and it is the one defect that makes
   every other `exists` failure undebuggable. Make the offending Silver node /
   source-role degrade to a normal Viper verification error. Do this first.
2. **§4.4 auto-bound for `forAll<Char>`/`exists<Char>`.** Small, self-contained,
   and directly in the `exists<T>` surface — emit the `[0,65536)` range bound so
   `exists<Char>`/`forAll<Char>` don't need a manual guard.
3. **§4.2 — `exists` instantiation for the split-point/boundary pattern.** In
   scope and high-value, but non-trivial (trigger inference or a new DSL hint).
   Attempt it *after* §4.1/§4.4; time-box it. If a full witness-hint construct is
   too large for the remaining debug iterations, land a default-trigger
   improvement for the boundary case and defer the explicit-hint DSL to its own
   feature. A verified partial spec (B's approach) is acceptable interim output.

**Defer as separate features (do NOT attempt under F6):**

- **§4.3 element arithmetic / `Char` successor axiom / pure `Char.code` / sum
  primitive.** This is a distinct capability (integer arithmetic on sequence
  elements), unblocks mex's last obligation and Kadane, but is out of `exists`'
  scope and larger than the F6 debit. Track as its own wish.
- **§4.4 `const val` in specs**, helper-fn calls in specs — adjacent, low
  priority.
- **§4.5 tooling** — fold into a HOWTO doc update, not a code change.

**Bottom line.** F6's `exists<T>` core is sound: nested `exists`/`forAll` and the
witness-carrying-invariant idiom verify real searches (A TwoEq, B MaxElim,
Downsampling). The debug step should spend its budget on (1) turning the crash
into a graceful failure, (2) the `Char`-range auto-bound, and (3) a bounded
attempt at split-point instantiation — and explicitly leave element arithmetic
for a later feature.
