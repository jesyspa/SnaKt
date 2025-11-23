import org.jetbrains.kotlin.formver.plugin.Pure

fun  <!VIPER_TEXT!>iAmAMethod<!>(): Int {
    return 1;
}

<!INTERNAL_ERROR!>@Pure
fun testWronglyAnnotatedAsPure(): Int {
    return iAmAMethod()
}<!>
