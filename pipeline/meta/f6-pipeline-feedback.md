# F6 (`exists<T>`) — Pipeline Feedback (Step 9 Meta-Review)

Objections to the pipeline *design and orchestration*, each grounded in
something that happened this run, each paired with a concrete fix. Ordered by
severity.

---

## 1. Spurious-crash → auto-retry → duplicate worker (highest severity)

**What happened.** In Step 5 a "worker crashed" turn-end event fired while
fyodor was alive and doing the work. The orchestrator trusted that signal and
spawned a retry (calliope). calliope duplicated the entire debug task and
converged on an identical fix. The collision was caught **only because calliope
itself noticed and stood down** — the orchestrator never detected it. fyodor
then really did stall (uncommitted) and needed a manual operator resume. Net:
one wasted opus build, and the near-miss of two workers racing to commit the
same fix to the same branch.

**Why the design is at fault.** The orchestrator treated a *turn-end crash
event* as proof of *worker death* and respawned on it, with no independent
liveness check and no mutual exclusion between the original and the retry.
Recovery depended on a worker's good manners, not on the control plane.

**Fixes.**
- Require positive death confirmation before respawn: a process exit code or a
  heartbeat that has gone silent past a timeout. A single turn-end event is a
  hint, not a death certificate — poll liveness before acting on it.
- Put a lease/lock on the work item (branch or task id). A respawn must acquire
  the lease; if the original still holds it, the retry no-ops. This makes the
  collision impossible by construction rather than caught-if-lucky.
- Have the debugger checkpoint (commit WIP to its branch) at milestones so a
  *real* crash loses minutes, not the whole task — which would also have made
  fyodor's genuine stall recoverable without a manual resume.

---

## 2. Direct-push-to-`main` instead of branch+PR discipline

**What happened.** In Step 6, rafael's comparator deliverable was first pushed
directly to the examples repo's `main`. The operator required a PR workflow, so
`main` was reset and the doc moved to PR #1 — a mid-run correction.

**Why the design is at fault.** "Push to a branch, open a PR" was an unwritten
expectation, not an enforced precondition. Nothing stopped the push: the
examples repo had no branch protection, and the agent brief did not state the
policy.

**Fixes.**
- State the push policy explicitly in every agent brief that touches a repo:
  feature branch + PR, never a push to any default branch. (The SnaKt side
  already had this convention; the examples repo did not inherit it.)
- Enforce it mechanically: enable branch protection on `main` of *both* repos so
  a stray push fails fast instead of needing a human-driven reset.

---

## 3. The two-repo split and its cross-repo friction

**What happened.** The run spanned two repos — the SnaKt fork and
`komiputer/snakt-verifythis`. Solver reports live on three separate branches
(`solver-a/b/c`) of the examples repo; the Step 6 comparison lives on yet
another branch (`pipeline/f6-step6-eval`). Steps 4, 6, 7, and 8 all had to do
cross-repo `gh` fetches to read their own inputs, and this meta-review had to
reach across four branches of a second repo to verify the log.

**Assessment — worth it, but under-served.** The split is justified: examples
should not build inside the SnaKt tree (they consume the published artifact),
and isolating three independent solvers on their own branches is exactly right
for Step 3's "must not share intermediate results" rule. The cost is
auditability: the run is *not self-contained*. Its evidence is scattered across
branches of a repo that every downstream reviewer must separately authenticate
to and branch-juggle, and a reader of `pipeline/runs/f6.md` alone cannot
reconstruct what happened.

**Fixes (keep the split, close the gap).**
- At synthesis time (Step 4) and comparison time (Step 6), *mirror* the source
  artifacts — the three solver reports and the Step 6 comparison — into the
  SnaKt `pipeline/` tree (e.g. `pipeline/solvers/f6-{a,b,c}.md`) so the run is
  self-contained and auditable from one branch.
- Pin exact commit shas of the examples-repo artifacts in the run log, not just
  branch names — branches move, and this review had to trust that the branches
  still pointed where the log implied.

---

## 4. Resource contention forcing review-only steps — and the unverified head

**What happened.** Host memory contention (concurrent bots; full SnaKt builds
OOM at ~11 min) pushed multiple steps to lightweight or review-only variants:
Step 5 validated via component tasks instead of a full `build`; Step 6 did no
SnaKt rebuild; Step 7's gate was REVIEW-ONLY. Step 6.5 was comment-only and
never re-published.

**Two distinct problems.**

**(a) "Evidence sufficient, skip rebuild" is ad-hoc.** Each agent decided on its
own to skip the build and cited memory pressure in prose. That reasoning is
mostly sound — rafael verified the published jar's provenance against
`547d1b04`, which is genuinely rigorous — but it is improvised per agent, not a
pipeline rule. A skipped rebuild should be a *first-class, documented decision*
with a required evidence citation, not a paragraph an agent writes if it
remembers to.

*Fix.* Add an explicit rule to PIPELINE.md: a step may substitute prior
evidence for a rebuild only if it (i) names the exact artifact/sha it is
trusting, (ii) states the artifact's git provenance, and (iii) records that a
rebuild was skipped and why. Make "review-only" a declared mode with a
checklist, so the assurance level of each step is legible.

**(b) The accepted head is unverified-as-built.** This is the real gap. The
published mavenLocal jar corresponds to `547d1b04` (fyodor's fix, which rafael
verified). But the branch head is `f4910294` — after marisol's comment-only
strip (`da572d3`) and quirin's review. Nothing rebuilt or republished from the
final head. Comment-only changes *cannot* alter behavior, so the risk here is
low — but the pipeline has no mechanism that *proves* the accepted head is
comment-only, nor any republish tying the shipped artifact to the branch tip. As
designed, a Step 6.5 that made a non-comment edit would slip through unbuilt.

*Fix.* Either (i) require Step 6.5 to republish and require Step 7's gate to
assert `published-artifact provenance == branch HEAD`; or (ii) require a
mechanical diff-check that every post-accept commit touches only comments/docs,
and record that check as the justification for not rebuilding. Do not leave "the
head is safe because the edits looked cosmetic" as an unstated assumption.

---

## 5. Model assignment and irreversible worker actions

**What happened.** The haiku solver (C) produced net-negative value: a wrong
root cause ("nested quantifiers fail") that contradicted the other two solvers
on the same problems, plus it **deleted its solution files**, leaving mateusz to
reconstruct the failure from the crash signature rather than from C's actual
attempts.

**Fixes.**
- The Step 3 solver role demands independent diagnosis under an opaque compiler.
  That is the wrong job for the weakest model. Either reserve the independent-
  diagnosis seat for opus/sonnet, or give haiku a tightly-constrained checklist
  (a fixed set of idioms to try, no free-form root-cause claims) and require it
  to flag "crash, cause unknown" rather than generalize.
- Forbid workers from deleting the record of their attempts. A blocked solver
  should *commit its failing files* (marked failing) so the synthesizer can
  inspect the exact AST shape that broke — the most useful thing a failed
  solver produces is a reproducer, and C threw it away.

---

## 6. The debug loop and the gate provided little assurance this run

**Observation, lower severity.** The debug loop is built for up to 5 iterations
of Step 5→6→7; it ran exactly once. That is fine when the fix converges — but
this run's single iteration was then gated by a *review-only* Step 7. So the
loop's convergence was never stress-tested by an executing gate: one debug pass,
accepted by a comparator that did no SnaKt rebuild, waved through by a gate that
did no build. The end-to-end behavioral evidence that *does* exist (rafael's
clean rebuild in the examples project against the published plugin) is solid and
real — but it sits in Step 6, and Steps 5 and 7 leaned on it rather than adding
independent verification. The assurance came from one agent's diligence, not
from the loop structure.

*Fix.* Fold this into §4(a): when the gate can't build, it should at minimum
*cite* the specific behavioral evidence it is accepting (which problem verified,
against which artifact sha) rather than pass on review of the diff alone. A gate
that names its evidence is auditable; a gate that says "looks good" is not.

---

## Summary of orchestration critique

The worker steps were largely strong (implementation, both capable solvers,
synthesis, and the final review were excellent). The weaknesses this run were
concentrated in **orchestration and policy**: respawning on an unconfirmed
crash, tolerating a push to `main`, leaving evidence-sufficiency and artifact
mirroring to agent improvisation, and shipping a branch head whose final commits
were never rebuilt. All four are control-plane gaps, and all four are fixable
with mechanical guards (leases, branch protection, provenance assertions,
declared review-only modes) rather than by asking agents to be more careful.
