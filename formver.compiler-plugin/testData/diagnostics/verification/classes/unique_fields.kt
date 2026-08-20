import org.jetbrains.kotlin.formver.plugin.Unique

class UniqueContainer(
    val sharedVal: Any,
    var sharedVar: Any,
    val uniqueVal: @Unique Any,
    var uniqueVar: @Unique Any
)

fun <!VIPER_TEXT!>testPrimitiveFieldGetterUnique<!>(pf: @Unique UniqueContainer) {
    val sharedVal = pf.sharedVal
    var sharedVar = pf.sharedVar
    val uniqueVal = pf.uniqueVal
    var uniqueVar = pf.uniqueVar
}

fun <!VIPER_TEXT!>testPrimitiveFieldGetterShared<!>(pf: UniqueContainer) {
    val sharedVal = pf.sharedVal
    var sharedVar = pf.sharedVar
    val uniqueVal = pf.uniqueVal
    var uniqueVar = pf.uniqueVar
}

fun <!VIPER_TEXT!>testPrimitiveFieldSetterUnique<!>(pf: @Unique UniqueContainer) {
    pf.sharedVar = Any()
    pf.uniqueVar = Any()
}

fun <!VIPER_TEXT!>testPrimitiveFieldSetterShared<!>(pf: UniqueContainer) {
    pf.sharedVar = Any()
    pf.uniqueVar = Any()
}

class UniqueNestedContainer(
    val sharedVal: UniqueContainer,
    var sharedVar: UniqueContainer,
    val uniqueVal: @Unique UniqueContainer,
    var uniqueVar: @Unique UniqueContainer
)

fun <!VIPER_TEXT!>testReferenceFieldGetterUnique<!>(rf: @Unique UniqueNestedContainer) {
    val sharedVal = rf.sharedVal
    var sharedVar = rf.sharedVar
    val uniqueVal = rf.uniqueVal
    var uniqueVar = rf.uniqueVar
}

fun <!VIPER_TEXT!>testReferenceFieldGetterShared<!>(rf: UniqueNestedContainer) {
    val sharedVal = rf.sharedVal
    var sharedVar = rf.sharedVar
    val uniqueVal = rf.uniqueVal
    var uniqueVar = rf.uniqueVar
}

fun <!VIPER_TEXT!>testReferenceFieldSetterUnique<!>(rf: @Unique UniqueNestedContainer) {
    rf.sharedVar = UniqueContainer(Any(), Any(), Any(), Any())
    rf.uniqueVar = UniqueContainer(Any(), Any(), Any(), Any())
}

fun <!VIPER_TEXT!>testReferenceFieldSetterShared<!>(rf: UniqueNestedContainer) {
    rf.sharedVar = UniqueContainer(Any(), Any(), Any(), Any())
    rf.uniqueVar = UniqueContainer(Any(), Any(), Any(), Any())
}
