// NEVER_VALIDATE

fun <!VIPER_TEXT!>returnUnit<!>() {}
fun <!VIPER_TEXT!>returnInt<!>(): Int { return 0 }
fun <!VIPER_TEXT!>takeIntReturnUnit<!>(@Suppress("UNUSED_PARAMETER") x: Int) {}
fun <!VIPER_TEXT!>takeIntReturnInt<!>(x: Int): Int { return x }
fun <!VIPER_TEXT!>takeIntReturnIntExpr<!>(x: Int): Int = x
fun <!VIPER_TEXT!>withIntDeclaration<!>(): Int {
    val x = 0
    return x
}
fun <!VIPER_TEXT!>intAssignment<!>() {
    var x = 0
    x = 1
}

// NEVER_VALIDATE

// Test Double literals
fun <!VIPER_TEXT!>testDoubleLiteral<!>() {
    val x = 1.0  // Double литерал
}

// Test class-related errors
class TestClass
fun <!VIPER_TEXT!>testClassUsages<!>() {
    TestClass      // ResolvedQualifier
    TestClass::class  // Class literal
}

// Test function literals
fun <!VIPER_TEXT!>testFunctionLiteral<!>() {
    val f = { x: Int -> x + 1 } // Anonymous function
}

