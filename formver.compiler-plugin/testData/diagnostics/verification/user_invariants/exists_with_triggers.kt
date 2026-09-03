// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

// `triggers(...)` is documented for `exists` as well as `forAll`. This file is the
// existential counterpart of forall_with_triggers.kt: it pins that explicit triggers reach
// the emitted `exists` as `{ ... }` groups, in each specification position that admits a
// quantifier. Without it, dropping the triggers on an `exists` changes no golden.

// An existential in a precondition is assumed rather than proved, so the trigger only has
// to survive conversion into the `requires` clause.
fun <!VIPER_TEXT!>existsWithSimpleTriggerInPrecondition<!>(n: Int): Int {
    preconditions {
        exists<Int> {
            // Specify trigger expression to guide SMT solver
            triggers(it * it)
            it * it == n
        }
    }
    return n
}

// Each argument of `triggers()` becomes a separate trigger group on the quantifier.
fun <!VIPER_TEXT!>existsWithMultipleTriggersInPrecondition<!>(n: Int): Int {
    preconditions {
        exists<Int> {
            // Multiple trigger expressions can be provided
            triggers(it * it, it + 1)
            it * it == n && it + 1 > 0
        }
    }
    return n
}

// Triggers can also be attached to an existential in a loop invariant. `val c = s[0]`
// reads `s[0]` before the loop, so the explicit `s[it]` trigger has a ground term to
// match and the solver can instantiate `it = 0` as the witness on every iteration.
fun <!VIPER_TEXT!>existsWithTriggerInLoopInvariant<!>(s: String): Int {
    preconditions {
        s.length > 0
    }
    val c = s[0]
    var i = 0
    while (i < s.length) {
        loopInvariants {
            0 <= i && i <= s.length
            exists<Int> {
                // Triggers can be used in loop invariants
                triggers(s[it])
                0 <= it && it < s.length && s[it] == c
            }
        }
        i += 1
    }
    return i
}
