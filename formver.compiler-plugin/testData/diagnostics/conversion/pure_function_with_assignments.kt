import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>returnNumberVal<!>(): Int {
    val x = 42
    return x
}

@Pure
fun <!VIPER_TEXT!>multipleAssignmentsOfDifferentType<!>(): Boolean {
    val a = 42
    val b = "Hello SnaKt"
    val c = true
    val d = 'A'
    return c
}

@Pure
fun <!VIPER_TEXT!>multipleAssignmentsWithLiteralReturn<!>(): Int {
    val a = 42
    val b = "Hello SnaKt"
    val c = true
    val d = 'A'
    return 42
}

@Pure
fun <!VIPER_TEXT!>laterInitializersCanRelyOnPrevious<!>(): Int {
    val a = 40
    val b = a + 2
    val c = b * 2
    return c
}

@Pure
fun <!VIPER_TEXT!>initializersCanRelyOnParameters<!>(x: Int, y: Int): Int {
    val sum  = x + y
    val diff = x - y
    val res  = sum * diff
    return res
}