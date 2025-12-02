import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>returnNumberVal<!>(): Int {
    val x = 42
    return x
}

@Pure
fun <!VIPER_TEXT!>returnNumberVar<!>(): Int {
    var x = 42
    return x
}

@Pure
fun <!VIPER_TEXT!>multipleAssignmentsOfDifferentType<!>(): Boolean {
    val a = 42
    a = 43
    val b = "Hello SnaKt"
    val c = true
    val d = 'A'
    return c
}