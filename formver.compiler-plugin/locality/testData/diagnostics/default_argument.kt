// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `assign global value as global default argument`(
    x: Any = Any()
) {}

fun `assign global value as local default argument`(
    x: @Borrowed Any = Any()
) {}

fun `assign local argument as global default argument`(
    x: @Borrowed Any,
    y: Any = <!LOCALITY_MISMATCH!>x<!>
) {}

fun `assign local argument as local default argument`(
    x: @Borrowed Any,
    y: @Borrowed Any = x
) {}

class `assign global value as global default argument in constructor`(
    x: @Borrowed Any = Any(),
)

class `assign local argument as global default argument in constructor`(
    x: @Borrowed Any,
    y: Any = <!LOCALITY_MISMATCH!>x<!>,
)
