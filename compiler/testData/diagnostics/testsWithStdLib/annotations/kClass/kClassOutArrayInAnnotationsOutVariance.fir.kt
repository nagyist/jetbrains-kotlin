// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KClass

open class A
class B1 : A()
class B2 : A()

annotation class Ann1(val arg: <!PROJECTION_IN_TYPE_OF_ANNOTATION_MEMBER_ERROR!>Array<out KClass<out A>><!>)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, outProjection, primaryConstructor, propertyDeclaration */
