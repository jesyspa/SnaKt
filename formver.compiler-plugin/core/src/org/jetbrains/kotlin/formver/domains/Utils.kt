package org.jetbrains.kotlin.formver.core.domains
import org.jetbrains.kotlin.formver.names.DomainFuncParameterName
import org.jetbrains.kotlin.formver.viper.ast.Type
import org.jetbrains.kotlin.formver.viper.ast.Var

fun domainVar(name: String, type: Type) = Var(DomainFuncParameterName(name), type)