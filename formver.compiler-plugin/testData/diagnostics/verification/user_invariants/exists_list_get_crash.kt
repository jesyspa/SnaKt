// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// `List.get` inside a quantifier body goes through a method-call (non-pure) embedding path,
// unlike `String`'s indexing operator. The quantifier lowering's fresh-anonymous-variable
// machinery assumes a pure context, so this currently crashes the compiler instead of producing
// a verification diagnostic. Confirmed to reproduce identically with `forAll<Int>` in place of
// `exists<Int>` here, so this is a pre-existing defect in quantifier lowering generally, not
// something introduced by `exists<T>`; pinning it via `exists` since that is this PR's scope.
// A real fix needs the linearizer to handle a non-pure quantifier body, which is more than a
// minimal patch, so this is landed as a documented crash rather than fixed here.
fun existsListGetCrash(l: List<Int>, res: Int): Int {
    postconditions<Int> {
        exists<Int> { i -> 0 <= i && i < l.size && l[i] == l[res] }
    }
    return 0
}
