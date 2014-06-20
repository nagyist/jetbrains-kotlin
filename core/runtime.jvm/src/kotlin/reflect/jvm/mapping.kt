/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect.jvm

import java.lang.reflect.*
import kotlin.jvm.internal.*
import kotlin.reflect.jvm.internal.*

// Kotlin reflection -> Java reflection


public val <T> KClass<T>.java: Class<T>
    get() = (this as KClassImpl<T>).jClass

public val KPackage.javaFacade: Class<*>
    get() = (this as KPackageImpl).jClass



public val KTopLevelProperty<*>.javaGetter: Method
    get() = (this as KTopLevelPropertyImpl<*>).getter

public val KMutableTopLevelProperty<*>.javaSetter: Method
    get() = (this as KMutableTopLevelPropertyImpl<*>).setter


public val KExtensionProperty<*, *>.javaGetter: Method
    get() = (this as KExtensionPropertyImpl<*, *>).getter

public val KMutableExtensionProperty<*, *>.javaSetter: Method
    get() = (this as KMutableExtensionPropertyImpl<*, *>).setter


public val KMemberProperty<*, *>.javaGetter: Method?
    get() = (this as KMemberPropertyImpl<*, *>).getter

public val KMutableMemberProperty<*, *>.javaSetter: Method?
    get() = (this as KMutableMemberPropertyImpl<*, *>).setter

public val KMemberProperty<*, *>.javaField: Field?
    get() = (this as KMemberPropertyImpl<*, *>).field


// Java reflection -> Kotlin reflection


public val <T> Class<T>.kotlinClass: KClass<T>?
    get() = kotlin.kClass as KClass<T>?

public fun Class<*>.toKotlinPackage(): KPackage? {
    return kotlin.kPackage
}


private val KOTLIN_CLASS_ANNOTATION_CLASS = Class.forName("kotlin.jvm.internal.KotlinClass") as Class<KotlinClass>
private val KOTLIN_PACKAGE_ANNOTATION_CLASS = Class.forName("kotlin.jvm.internal.KotlinPackage") as Class<KotlinPackage>

private val Class<*>.kotlin: KClassOrPackage
    get() {
        if (isAnnotationPresent(KOTLIN_PACKAGE_ANNOTATION_CLASS)) {
            return KClassOrPackage(null, kPackage(this))
        }
        if (isAnnotationPresent(KOTLIN_CLASS_ANNOTATION_CLASS)) {
            return KClassOrPackage(kClass(this as Class<Any?>), null)
        }
        // TODO: return special KClass for Java class
        return KClassOrPackage(kClass(this as Class<Any?>), null)
    }

private data class KClassOrPackage(val kClass: KClassImpl<*>?, val kPackage: KPackageImpl?)



public val Field.kotlin: KProperty<*>
    get() {
        // Note that extension properties cannot have backing fields, thus this code never creates an extension property
        val (kClass, kPackage) = getDeclaringClass().kotlin
        val name = getName()!!
        val final = Modifier.isFinal(getModifiers())
        if (kPackage != null) {
            return if (final) topLevelProperty(name, kPackage) else mutableTopLevelProperty(name, kPackage)
        }
        else if (kClass != null) {
            return if (final) memberProperty(name, kClass) else mutableMemberProperty(name, kClass)
        }
        else {
            throw IllegalArgumentException("$this is not available to Kotlin reflection")
        }
    }



