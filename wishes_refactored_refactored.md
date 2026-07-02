# SnaKt Wishes — Reasonableness Audit

Each feature is assessed on two axes:
- **Reasonable?** — is the proof obligation it serves real and standard in the verification literature?
- **Proposed API** — is the sketched syntax coherent with SnaKt's existing model?

A feature is "reasonable" if it maps onto a well-understood technique (separation logic,
Viper primitives, Dafny/Why3 precedent, etc.). Features are ordered from least to most
complex to implement. Original identifiers (F1–F16) are preserved for cross-reference.

---

## F14 — Old Array Snapshots — 3 problems

**Reasonable? Yes.** `old(expr)` for scalar fields is already in SnaKt; extending it to
whole-array snapshots is a small, well-motivated step. Without it, stating "this half of the
array is unchanged" requires per-element quantification.

**Needed for:** 2012 PrefixSum, 2015 Dancing Links, 2017 Pair Insertion Sort.

```kotlin
fun prefixSum(a: IntArray) {
    val snap = old(a.snapshot())         // capture the entire pre-state array
    postconditions<Unit> {
        forAll<Int> { i ->
            (0 < i && i < a.size) implies (a[i] == snap[i] + snap[i - 1])
        }
    }
}
```

---

## F15 — Array Reallocation / Fresh Arrays — 3 problems

**Reasonable? Yes.** `fresh(b)` is a standard Viper/Dafny framing primitive that asserts a
reference was not in the pre-state heap. Without it, a function returning a new array cannot
claim the caller's other heap locations are untouched.

**Needed for:** 2017 Tree Buffer, 2018 Gap Buffer, 2025 Persistent Arrays.

```kotlin
fun grow(a: IntArray, extra: Int): IntArray {
    postconditions<IntArray> { b ->
        fresh(b)                         // b did not exist before the call
        b.size == a.size + extra
        forAll<Int> { i -> (0 <= i && i < a.size) implies b[i] == a[i] }
    }
}
```

---

## F6 — Existential Quantifiers — 11 problems

**Reasonable? Yes.** A `forAll` without `exists` is incomplete; witnesses are required for
maximum/minimum properties, duplicate existence, and constructive postconditions. Viper has
`exists`, Dafny has `exists` — this is table-stakes.

**Needed for:** 2011 MaxElim, 2011 TreeMax, 2011 TwoEq, 2015 Relaxed Prefix,
2017 Maximum-sum subarray, 2018 Colored Tiles, 2019 Cartesian Trees,
2021 Lexicographic Permutations, 2022 Downsampling, 2024 Smart Array Copy,
2025 Minimum Excludant.

```kotlin
// the result is the maximum of the array and it actually occurs
postconditions<Int> { m ->
    forAll<Int> { i -> (0 <= i && i < a.size) implies a[i] <= m } &&
    exists<Int> { i -> 0 <= i && i < a.size && a[i] == m }
}
```

---

## F3 — Mathematical Sequences and Lists — 14 problems

**Reasonable? Yes.** Viper's `Seq[T]` already exists; exposing it in Kotlin specs is a natural
binding. Slice/concat/take operations are the usual vocabulary for string, array, and list proofs.

**Needed for:** 2012 LCP/LRS, 2017 Tree Buffer, 2018 Colored Tiles, 2018 Gap Buffer,
2019 Cartesian Trees, 2019 GHC Sort, 2019 Sparse Matrix Multiplication,
2021 Lexicographic Permutations, 2021 DLL to BST, 2022 Downsampling,
2022 Mergesort with Runs, 2023 List Reversal, 2023 Rope, 2025 Linus List Removal.

```kotlin
// specify that a gap buffer delete preserves surrounding content
fun delete(buf: GapBuf, l: Int, r: Int) {
    val before: Seq<Int> = buf.toSeq()
    postconditions<Unit> {
        buf.toSeq() == before.take(l) + before.drop(r)
    }
}
```

---

## F7 — Mathematical Multisets and Permutations — 11 problems

**Reasonable? Yes.** Sorting correctness requires two things: order and permutation preservation.
Without a multiset type the permutation half cannot be expressed. Viper has `Multiset[T]`; this
is again a binding gap.

**Needed for:** 2012 LCP/LRS, 2014 Doubly-linked bubble sort, 2017 Pair Insertion Sort,
2017 Odd-even Sort, 2019 GHC Sort, 2021 Lexicographic Permutations, 2021 Shearsort,
2022 Mergesort with Runs, 2024 Smart Array Copy, 2024 LL/SC Queue, 2025 Minimum Excludant.

```kotlin
fun sort(a: IntArray) {
    postconditions<Unit> {
        a.toMultiset() == old(a.toMultiset())   // no elements added or lost
        forAll<Int> { i -> (0 <= i && i < a.size - 1) implies a[i] <= a[i + 1] }
    }
}
```

---

## F12 — Nested / Two-Dimensional Array Support — 4 problems

**Reasonable? Yes.** `Array<IntArray>` is idiomatic Kotlin; the verifier needs to give
per-row permissions and let specs range over both indices without flattening. This is a
straightforward extension of the existing `acc` model.

**Needed for:** 2016 Matrix Multiplication, 2017 Maximum-sum subarray, 2021 Shearsort,
2022 Downsampling.

```kotlin
fun matMul(a: Array<IntArray>, b: Array<IntArray>, c: Array<IntArray>) {
    preconditions {
        forAll<Int> { i -> (0 <= i && i < a.size) implies acc(a[i], read()) }
        forAll<Int> { i -> (0 <= i && i < b.size) implies acc(b[i], read()) }
        forAll<Int> { i -> (0 <= i && i < c.size) implies acc(c[i], write()) }
    }
}
```

---

## F5 — Nullable References and Small Data Classes in Specifications — 12 problems

**Reasonable? Yes.** Kotlin's type system is null-aware; specs must be too. This is not a new
idea — it is fixing an impedance mismatch between the host language and the spec layer.

**Needed for:** 2011 TreeMax, 2011 Cycle, 2012 TreeDel, 2014 Doubly-linked bubble sort,
2016 Binary Tree Traversal, 2019 Cartesian Trees, 2021 DLL to BST, 2023 List Reversal,
2023 BDD, 2023 Rope, 2025 Linus List Removal, 2025 Persistent Arrays.

```kotlin
data class Interval(val lo: Int, val hi: Int)   // ghost model

fun search(a: IntArray, x: Int): Interval? {
    postconditions<Interval?> { r ->
        (r != null) implies (r.lo >= 0 && r.hi < a.size && a[r.lo] == x)
    }
}
```

---

## F1 — Loop Variants / Termination Measures — 30 problems

**Reasonable? Yes.** Termination is a first-class proof obligation in every mature verifier
(Dafny `decreases`, Viper `termination`, Why3 `variant`). Demanded by 30/42 problems — the
single highest-impact missing feature.

**Needed for:** 2011 MaxElim, 2011 TwoEq, 2011 Cycle, 2012 LCP, 2012 PrefixSum,
2012 TreeDel, 2014 Coincidence Count, 2014 Doubly-linked bubble sort, 2015 Parallel GCD,
2016 Binary Tree Traversal, 2017 Odd-even Sort, 2018 ABQL, 2018 Register Allocation,
2019 Cartesian Trees, 2019 GHC Sort, 2021 Lexicographic Permutations, 2021 DLL to BST,
2021 Shearsort, 2022 Mergesort with Runs, 2022 Lock-Free Hash Set, 2023 List Reversal,
2023 BDD, 2023 Rope, 2024 Smart Array Copy, 2024 Work-Stealing, 2024 LL/SC Queue,
2025 Minimum Excludant, 2025 Linus List Removal, 2025 Persistent Arrays,
2025 Persistent Disjoint Sets.

```kotlin
fun gcd(a: Int, b: Int): Int {
    preconditions { a > 0; b > 0 }
    var x = a; var y = b
    while (x != y) {
        loopInvariants { x > 0; y > 0 }
        loopVariant { x + y }          // strictly decreases each iteration
        if (x > y) x -= y else y -= x
    }
    return x
}
```

---

## F10 — Collection ADTs: Sets, Maps, Stacks, Deques, Lists — 7 problems

**Reasonable? Yes.** Spec-level mathematical collections (not runtime) are standard ghost
vocabulary in any proof assistant. They enable clean register-allocation, hash-set, and
work-stealing specs without encoding everything as arrays.

**Needed for:** 2018 Register Allocation, 2019 Cartesian Trees, 2019 GHC Sort,
2022 Lock-Free Hash Set, 2023 BDD, 2024 Work-Stealing, 2025 Persistent Disjoint Sets.

```kotlin
// register allocator: result lives in a free register and does not alias live set
postconditions<Int> { r ->
    r in freeRegs(state) && r !in liveSet(state)
}
```

---

## F8 — Pure Recursive Lemmas / Induction over `@Pure` Functions — 11 problems

**Reasonable? Yes.** Writing a `@Pure` recursive function is already supported; the gap is
proving reusable facts about it by induction and invoking those proofs as lemmas. This is how
Dafny's `lemma`, Lean's `theorem`, and Why3's `lemma` work. The annotation and call syntax
proposed below is conventional.

**Needed for:** 2012 LCP/LRS, 2012 PrefixSum, 2014 Coincidence Count,
2016 Matrix Multiplication, 2017 Maximum-sum subarray, 2018 Register Allocation,
2019 Sparse Matrix Multiplication, 2022 Downsampling, 2023 BDD,
2025 Persistent Arrays, 2025 Persistent Disjoint Sets.

```kotlin
@Pure fun rangeSum(a: IntArray, lo: Int, hi: Int): Int = ...

@Lemma
fun rangeSumStep(a: IntArray, lo: Int, hi: Int) {
    preconditions { 0 <= lo && lo <= hi && hi < a.size }
    postconditions<Unit> {
        rangeSum(a, lo, hi + 1) == rangeSum(a, lo, hi) + a[hi]
    }
    // body: unfold definition, base case / inductive step
}

// call site: invoke the lemma to unlock the fact for the solver
rangeSumStep(a, 0, k)
```

---

## F13 — Relational / Equivalence Specifications — 4 problems

**Reasonable? Yes, niche.** Relational (two-run) Hoare logic is well-founded; product-program
encodings exist (e.g. Benton's RHL, Barthe et al.). It is the right tool for "parallel and
sequential versions compute the same result". Only 4 problems need it, so priority is low.

**Needed for:** 2016 Matrix Multiplication, 2017 Tree Buffer,
2019 Sparse Matrix Multiplication, 2021 Shearsort.

```kotlin
@Relational
fun shearsortEquiv(m: Array<IntArray>) {
    // proves: for all inputs, both implementations yield identical output arrays
    ensuresEqual(
        { parallelShearsort(m.deepCopy()) },
        { sequentialShearsort(m.deepCopy()) }
    )
}
```

---

## F4 — Reachability and Footprints — 12 problems

**Reasonable? Yes.** Reachability sets and framing footprints are standard in shape analyses
(e.g. Viper's `forperm`, VeriFast's `lseg`). Stating "only this heap region changes" is
otherwise impossible without them.

**Needed for:** 2011 TreeMax, 2011 Cycle, 2012 TreeDel, 2014 Doubly-linked bubble sort,
2016 Binary Tree Traversal, 2019 Cartesian Trees, 2021 DLL to BST, 2023 List Reversal,
2023 BDD, 2023 Rope, 2024 Work-Stealing, 2025 Linus List Removal.

```kotlin
fun markAll(root: Node?) {
    postconditions<Unit> {
        forAll<Node> { n -> reachable(root, n) implies n.mark }
    }
    modifies(footprint(root))   // no other heap locations touched
}
```

---

## F2 — Heap-Shape Predicates for Trees, Linked Structures and DAGs — 16 problems

**Reasonable? Yes.** Recursive heap predicates are the core of separation logic and Viper's
`predicate` mechanism. Without them, ownership of linked/tree structures cannot be stated at all.

**Needed for:** 2011 TreeMax, 2011 Cycle, 2012 TreeDel, 2014 Doubly-linked bubble sort,
2015 Dancing Links, 2016 Binary Tree Traversal, 2016 Static Tree Barriers,
2019 Cartesian Trees, 2021 DLL to BST, 2023 List Reversal, 2023 BDD, 2023 Rope,
2024 Work-Stealing, 2025 Linus List Removal, 2025 Persistent Arrays,
2025 Persistent Disjoint Sets.

```kotlin
predicate dll(node: Node?, ghost values: Seq<Int>)
// unfold inside the function to access fields:
unfold dll(head, values)
preconditions { dll(head, values) }
postconditions<Unit> { dll(head, values) }
```

---

## F11 — Resource and Cost Bounds — 6 problems

**Reasonable? Yes, but very ambitious.** Cost semantics are theoretically sound (e.g.
Resource-Aware ML, AARA, Tezos gas proofs). However, plugging a cost model into a separation-
logic backend requires either a dedicated instrumented semantics or a credits-based encoding.
The feature is legitimate; the effort is disproportionate to 6 problems.

**Needed for:** 2017 Tree Buffer, 2022 Mergesort with Runs, 2023 List Reversal,
2023 Rope, 2025 Persistent Arrays, 2025 Persistent Disjoint Sets.

```kotlin
fun append(buf: Buf, x: Int): Buf {
    costBound { credits >= 1 }           // one allocation unit consumed
    postconditions<Buf> { b -> b.size == buf.size + 1 }
}
```

---

## F16 — Subtree-Quantified Global Invariants — 1 problem

**Reasonable? Yes, highly specific.** The invariant "if a node's sense flag is set then all
nodes in its subtree have it set too" is a valid tree-inductive invariant; global invariants
over all heap nodes exist in Viper (`forperm`) and Iris. It is reasonable to request; depends
on F2 and F4, which must be in place first.

**Needed for:** 2016 Static Tree Barriers.

```kotlin
globalInvariant("senseMonotone") {
    forAll<Node> { n ->
        n.sense implies forAll<Node> { m -> inSubtree(n, m) implies m.sense }
    }
}
```

---

## F9 — Concurrency / Transition-System Modelling — 9 problems

**Reasonable? Yes, but ambitious.** Rely/guarantee, linearization points, and CAS are
well-studied (CIVL, TaDA, Iris), but each is a large vertical addition. The feature is
coherent and necessary for 9 problems; it is the hardest item on the list by implementation
cost. The API sketch below is plausible as a surface syntax over an encoding.

**Needed for:** 2015 Parallel GCD, 2016 Static Tree Barriers, 2017 Odd-even Sort,
2018 ABQL, 2019 Sparse Matrix Multiplication, 2021 Shearsort,
2022 Lock-Free Hash Set, 2024 Work-Stealing, 2024 LL/SC Queue.

```kotlin
// CAS-based lock-free push
fun push(stack: AtomicRef<Node?>, value: Int) {
    atomic {                              // atomic block = linearization point
        val old = stack.get()
        val node = Node(value, old)
        compareAndSwap(stack, old, node)
    }
}
```
