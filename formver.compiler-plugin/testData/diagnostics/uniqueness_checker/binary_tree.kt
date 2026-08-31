// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique

class Tree(var left: @Unique Tree?, var right: @Unique Tree?)

fun consumeBoth(a: @Unique Tree?, b: @Unique Tree?) {}

fun `insert leaf into an empty child slot`(t: @Unique Tree, leaf: @Unique Tree) {
    if (t.left == null) {
        t.left = leaf
    } else {
        t.right = leaf
    }
}

// The checker does not see `pivot?.right = root` as restoring `pivot.right`, so the pivot escapes with a moved field.
// The same rotation on a non-null pivot checks clean, which places the gap in the safe-call assignment.
fun `rotate right around the root`(root: @Unique Tree): @Unique Tree? {
    val pivot: @Unique Tree? = root.left
    root.left = pivot?.right
    pivot?.right = root
    return <!ESCAPE_UNIQUENESS_INCONSISTENCY!>pivot<!>
}

fun `swap the children through a temporary`(t: @Unique Tree) {
    val tmp: @Unique Tree? = t.left
    t.left = t.right
    t.right = tmp
}

fun `pass the same child as two unique arguments`(t: @Unique Tree) {
    consumeBoth(<!INVALID_DUPLICATE_UNIQUE_ARGUMENT!>t.left<!>, <!INVALID_DUPLICATE_UNIQUE_ARGUMENT!>t.left<!>)
}
