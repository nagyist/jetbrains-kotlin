// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: a.kt

package weatherForecast

fun weatherToday() = "snow"

// FILE: b.kt

package myApp

import weatherForecast.*
import weatherForecast.*

fun needUmbrella() = weatherToday() == "rain"

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, stringLiteral */
