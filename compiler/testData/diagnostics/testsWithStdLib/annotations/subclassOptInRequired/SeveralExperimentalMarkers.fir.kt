// RUN_PIPELINE_TILL: FRONTEND
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class ApiMarkerA

@RequiresOptIn
annotation class ApiMarkerB

@SubclassOptInRequired(ApiMarkerA::class, ApiMarkerB::class)
open class OpenKlass

class MyKlass() : <!OPT_IN_TO_INHERITANCE_ERROR, OPT_IN_TO_INHERITANCE_ERROR!>OpenKlass<!>()

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, classDeclaration, classReference,
primaryConstructor */
