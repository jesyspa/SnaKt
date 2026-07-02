# SnaKt Feature Development Pipeline

A structured, multi-agent workflow for implementing missing SnaKt features, validating them
against real VerifyThis problems, and iterating to correctness.

The feature backlog lives in [`wishes_refactored_refactored.md`](../wishes_refactored_refactored.md)
(features F1–F16, ranked by demand).

---

## Overview

Each run of the pipeline targets **one missing feature** (e.g. F1 Loop Variants).
If the feature is large, it is split into self-contained pieces and the full pipeline
runs for each piece independently before moving on.

Every step is handled by a **dedicated agent**. No agent doubles up on steps.

```
Step 0  → pick feature / split into pieces
Step 1  → fork the repository
Step 2  → implement
Step 3  → three solver agents give feedback
Step 4  → one agent consolidates feedback
Step 5  → debug cycle begins
Step 6  → review: is the debugged version better?
Step 6.5→ strip TODO-style comments from accepted version
Step 7  → gate check: good enough? loop up to 5× back to Step 5
Step 8  → final review report
Step 9  → pipeline meta-review (logs only, isolated agent)
```

---

## Steps

### Step 0 — Feature Selection

**Agent role:** Planner

Pick the next unimplemented feature from the backlog.
If the feature is large (e.g. F2 Heap-Shape Predicates, F9 Concurrency), decompose it into
independent pieces. For each piece, run Steps 1–9 in full before starting the next piece.

**Output:** Feature name + optional list of sub-pieces.

---

### Step 1 — Fork Repository

**Agent role:** Repository setup

Create a fork of the SnaKt repository that will hold this feature's implementation.
All subsequent code changes land on this fork, not the main repo.

**Output:** Fork URL / local branch reference.

---

### Step 2 — Implement the Feature

**Agent role:** Implementer

Implement the feature on the fork. This includes:
- Compiler/plugin changes inside SnaKt as needed
- New Kotlin contract API surface (annotations, functions, DSL blocks)
- At minimum one working usage example

The implementation must compile cleanly via `./gradlew build`.

**Output:** Working implementation on the fork branch.

---

### Step 3 — Solver Agents (×3, independent)

**Agent roles:** Solver A, Solver B, Solver C — powered by different models where possible

Each solver agent works independently:
1. Picks one or more VerifyThis problems identified as blocked by this feature.
2. Attempts to write a fully-specified Kotlin solution using the new feature.
3. Reports what worked, what did not, and any rough edges encountered.

The three agents must not share intermediate results with each other.

**Output:** Three independent feedback reports.

---

### Step 4 — Feedback Consolidation

**Agent role:** Synthesizer

Reads the three solver reports and produces a single consolidated feedback document:
- Points of agreement across solvers
- Conflicting observations (flagged explicitly)
- Prioritised list of bugs, gaps, and usability issues

**Output:** Consolidated feedback document.

---

### Step 5 — Debug

**Agent role:** Debugger

Takes the consolidated feedback and applies targeted fixes to the fork.
Each fix is minimal — no unrelated cleanup.

**Output:** Updated implementation on the fork.

---

### Step 6 — Review: Better or Worse?

**Agent role:** Comparator

Compares the post-debug implementation against the pre-debug version.
Decides which is strictly better. If the debugged version is better, it becomes the
new main version on the fork. If not, the pre-debug version is restored.

**Output:** Decision (accepted / reverted) + brief justification.

---

### Step 6.5 — Strip TODO-Style Comments

**Agent role:** Cleaner

Goes through the current accepted version and removes all TODO-style comments —
anything that reads like an unfinished note, a placeholder, or a reminder to self.
Functional comments explaining *why* (non-obvious constraints, invariants) are kept.

**Output:** Cleaned source files committed to the fork.

---

### Step 7 — Gate Check (max 5 iterations)

**Agent role:** Quality Gate

Assesses whether the current implementation is good enough to proceed to final review.
Criteria: compiles, contracts are meaningful, at least one solver problem passes end-to-end.

- **If good enough:** proceed to Step 8.
- **If not good enough and iteration count < 5:** go back to Step 5.
- **If iteration count reaches 5 and still not good enough:** proceed to Step 8 anyway,
  but flag in the final summary that the feature did not converge after five debug cycles.

**Output:** Pass / fail decision + iteration count.

---

### Step 8 — Final Review Report

**Agent role:** Reviewer

Produces a self-contained report for the implemented feature covering:
- **What was implemented** — API surface, annotations, DSL additions
- **Pros** — what the feature enables, which proof obligations it unlocks
- **Cons / limitations** — known gaps, edge cases not handled, backend caveats
- **Usage examples** — two or three concrete Kotlin snippets from the solver problems

**Output:** `pipeline/reviews/<feature-id>.md`

---

### Step 9 — Pipeline Meta-Review

**Agent role:** Meta-Reviewer (isolated — reads logs only, makes no code changes)

This agent has access to the logs of all previous agents in this pipeline run and
nothing else. It:
1. Rates each agent's contribution on impact (1–5) and quality (1–5).
2. Produces a Markdown table: agent | step | impact | quality | notes.
3. Produces a separate file with objections to the current pipeline design and concrete
   ideas for improving it.

**Output:**
- `pipeline/meta/<feature-id>-agent-ratings.md` — ratings table
- `pipeline/meta/<feature-id>-pipeline-feedback.md` — objections and improvement ideas

---

## Iteration Limit

The debug loop (Steps 5 → 6 → 7) runs at most **five times** per piece.
If five iterations are exhausted without a passing gate check, the final review (Step 8)
and meta-review (Step 9) still run, and the final report must state:

> "Feature `<id>` did not reach a passing gate after 5 debug iterations."

---

## File Conventions

| Path | Contents |
|---|---|
| `pipeline/PIPELINE.md` | This document |
| `pipeline/reviews/<feature-id>.md` | Final review report (Step 8) |
| `pipeline/meta/<feature-id>-agent-ratings.md` | Agent impact/quality table (Step 9) |
| `pipeline/meta/<feature-id>-pipeline-feedback.md` | Pipeline improvement ideas (Step 9) |
