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

package org.jetbrains.k2js.test.semantics;

import org.jetbrains.k2js.test.SingleFileTranslationTest;

public class CallableReferenceTest extends SingleFileTranslationTest {
    public CallableReferenceTest() {
        super("callableReference/");
    }


    public void testTopLevelFromClass() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTopLevelFromExtension() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTopLevelFromTopLevelStringNoArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testTopLevelFromTopLevelWithArg() throws Exception {
        fooBoxTest();
    }

    public void testTopLevelFromTopLevelViaFunCall() throws Exception {
        fooBoxTest();
    }

    public void testClassMemberFromClass()  throws Exception {
        checkFooBoxIsOk();
    }

    public void testClassMemberFromExtension()  throws Exception {
        checkFooBoxIsOk();
    }

    public void testClassMemberFromTopLevelStringNoArgs()  throws Exception {
        checkFooBoxIsOk();
    }

    public void testClassMemberFromTopLevelStringOneStringArg()  throws Exception {
        checkFooBoxIsOk();
    }

    public void testClassMemberFromTopLevelUnitNoArgs()  throws Exception {
        checkFooBoxIsOk();
    }

    public void testClassMemberFromTopLevelUnitOneStringArg()  throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionFromTopLevel() throws Exception {
        fooBoxTest();
    }

    public void testExtensionFromClass() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionFromExtension() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionFromTopLevelStringNoArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionFromTopLevelStringOneStringArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionFromTopLevelUnitNoArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testExtensionFromTopLevelUnitOneStringArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testConstructorFromTopLevelNoArgs() throws Exception {
        checkFooBoxIsOk();
    }

    public void testConstructorFromTopLevelOneStringArg() throws Exception {
        checkFooBoxIsOk();
    }

    public void testAbstractClassMember() throws Exception {
        checkFooBoxIsOk();
    }

    public void testBooleanNotIntrinsic() throws Exception {
        checkFooBoxIsOk();
    }
}
