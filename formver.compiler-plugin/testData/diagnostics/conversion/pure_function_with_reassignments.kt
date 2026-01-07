import org.jetbrains.kotlin.formver.plugin.Pure

@Pure
fun <!VIPER_TEXT!>doubleIncrement<!>(): Int {
    var x = 1
    x = x + 1
    x = x + 1
    return x
}

@Pure
fun <!VIPER_TEXT!>updateThenReadIntoOther<!>(): Int {
    var x = 3
    x = x + 4
    val y = x + 1
    return y
}

@Pure
fun <!VIPER_TEXT!>readOldValueBeforeUpdate<!>(): Int {
    var x = 10
    val y = x + 1
    x = x + 5
    x = y + x
    return x
}


@Pure
fun <!VIPER_TEXT!>chainThroughTemp<!>(): Int {
    var x = 2
    val t = x + 3
    x = t + 4
    return x
}

@Pure
fun <!VIPER_TEXT!>overwriteNotSelfReferential<!>(): Int {
    var x = 7
    x = 100
    return x
}