// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

abstract class Super(
    @Unique var x: Int
)

@Manual
class Test(
    x: Int
) : Super(x)

fun <!VIPER_TEXT!>test<!>(@Unique p: Test) {
    unfold(UniquePred(p))
    unfold(UniquePred(p as Super))
    p.x = 5
    fold(UniquePred(p as Super))
    fold(UniquePred(p))
}


@Manual
class Tree(
    @Unique var left: Tree?,
    @Unique var right: Tree?,
    @Unique var data: Int,
)


fun <!VIPER_TEXT!>contains<!>(@Unique @Borrowed tree: Tree?, search: Int) : Boolean {
    if (tree == null) return false
    unfold(UniquePred(tree))
    if (tree.data == search) {
        fold(UniquePred(tree))
        return true
    }
    val res = contains(tree.left, search) || contains(tree.right, search)
    fold(UniquePred(tree))
    return res
}


@Unique
fun <!VIPER_TEXT!>combine<!>(@Unique left: Tree, @Unique right: Tree) : Tree {
    unfold(UniquePred(left))
    unfold(UniquePred(right))
    val data = left.data + right.data
    fold(UniquePred(left))
    fold(UniquePred(right))
    val res = Tree(left, right, data)
    return res
}
