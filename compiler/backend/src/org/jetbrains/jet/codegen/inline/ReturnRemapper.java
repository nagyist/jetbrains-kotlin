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

package org.jetbrains.jet.codegen.inline;

import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

public abstract class ReturnRemapper extends InstructionAdapter {
    protected final LabelOwner labelOwner;
    protected final boolean remapReturn;
    protected boolean skipReturnRemapping;

    protected ReturnRemapper(
            MethodVisitor mv,
            LabelOwner labelOwner,
            boolean remapReturn
    ) {
        super(InlineCodegenUtil.API, mv);
        this.labelOwner = labelOwner;
        this.remapReturn = remapReturn;
    }

    @Override
    public void visitInsn(int opcode) {
        if (remapReturn && opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
            if (skipReturnRemapping) {
                super.visitInsn(opcode);
            } else {
                super.visitJumpInsn(Opcodes.GOTO, getEndLabel());
            }
            skipReturnRemapping = false;
        }
        else {
            super.visitInsn(opcode);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (owner.equals(InlineCodegenUtil.GLOBAL_RETURN)) {
            skipReturnRemapping = !labelOwner.isMyLabel(name);
            if (skipReturnRemapping) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    public abstract Label getEndLabel();
}
