// RUN_PIPELINE_TILL: FRONTEND
interface A

interface RootType<T1>

interface ChildType1<T2> : RootType<T2>

interface ChildType2<T3> : RootType<T3>

class Test<<!INCONSISTENT_TYPE_PARAMETER_BOUNDS("T1; interface RootType<T1> : Any; Any?, A")!>T4<!>> where T4 : ChildType2<*>, T4 : ChildType1<A>

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nullableType, starProjection, typeConstraint,
typeParameter */
