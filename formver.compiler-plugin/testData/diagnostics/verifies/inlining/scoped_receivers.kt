// REPLACE_STDLIB_EXTENSIONS

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

@Suppress("NOTHING_TO_INLINE")
@NeverConvert
inline fun Int?.isNull() = this == null

@Suppress("NOTHING_TO_INLINE")
@NeverConvert
inline fun Int?.isNotNull() = this != null

@Suppress("LABEL_NAME_CLASH")
fun Int?.<!VIPER_TEXT!>with_run_extension_labeled<!>() {
    val cond1 = isNull() || this@with_run_extension_labeled.isNotNull()
    verify(cond1)
    with(true) {
        with(null) {
            verify(
                isNull(),
                this.isNull(),
                this@with.isNull(),
            )
        }
        verify(this, this@with)
        false.run {
            verify(
                !this,
                !this@run,
                this@with,
                this@with_run_extension_labeled.isNull() || isNotNull(),
            )
            with(false) {
                verify(
                    !this,
                    !this@with,
                    !this@run,
                )
                with(true) labeled_with@{
                    verify(
                        !this@with,
                        this,
                        this@labeled_with,
                    )
                }
            }
        }
    }
}

