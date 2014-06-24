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

package kotlin.reflect.jvm.internal

import java.lang.reflect.Method

open class KTopLevelPropertyImpl<out R>(
        name: String,
        protected val owner: KPackageImpl
) : KTopLevelProperty<R>, KVariableImpl<R>(name) {
    // TODO: extract, make lazy (weak?), use our descriptors knowledge, support Java fields
    protected val getter: Method = owner.jClass.getMethod(getterName(name))

    override fun get(): R {
        return getter(null) as R
    }
}

class KMutableTopLevelPropertyImpl<R>(
        name: String,
        owner: KPackageImpl
) : KMutableTopLevelProperty<R>, KTopLevelPropertyImpl<R>(name, owner) {
    private val setter = owner.jClass.getMethod(setterName(name), getter.getReturnType()!!)

    override fun set(value: R) {
        setter.invoke(null, value)
    }
}
