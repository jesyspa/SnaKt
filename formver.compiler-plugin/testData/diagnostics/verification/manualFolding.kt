// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

abstract class Super(
    var x: @Unique Any
)

@Manual
class Test(
    x: @Unique Any
) : Super(x)

fun <!VIPER_TEXT!>test<!>(p: @Unique Test) {
    unfold(UniquePred(p))
    unfold(UniquePred(p as Super))
    p.x = Any()
    fold(UniquePred(p as Super))
    fold(UniquePred(p))
}


@Manual
class Tree(
    var left: @Unique Tree?,
    var right: @Unique Tree?,
    var data: Int,
)


fun <!VIPER_TEXT!>contains<!>(tree: @Unique @Borrowed Tree?, search: Int) : Boolean {
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


fun <!VIPER_TEXT!>combine<!>(left: @Unique Tree, right: @Unique Tree) : @Unique Tree {
    unfold(UniquePred(left))
    unfold(UniquePred(right))
    val data = left.data + right.data
    fold(UniquePred(left))
    fold(UniquePred(right))
    val res: @Unique Tree = Tree(left, right, data)
    return res
}
