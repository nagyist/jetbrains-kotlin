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

package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;

public class InlineDescriptorUtils {

    public static boolean checkNonLocalReturnUsage(@NotNull DeclarationDescriptor returnFunction, @NotNull JetExpression startExpression, @NotNull BindingTrace trace) {
        PsiElement containingFunction = getDeclaration(startExpression);
        assert containingFunction != null : "Couldn't find parent declaration for " + startExpression.getText();

        DeclarationDescriptor parentDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, containingFunction);
        if (parentDescriptor == null) {
            return false;
        }

        BindingContext bindingContext = trace.getBindingContext();
        DeclarationDescriptor containingFunctionDescriptor = getContainingClassOrFunctionDescriptor(parentDescriptor, false);
        if (containingFunctionDescriptor == null) {
            return false;
        }
        containingFunction = BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), containingFunctionDescriptor);
        assert containingFunction != null;

        while (containingFunction instanceof JetFunctionLiteral && returnFunction != containingFunctionDescriptor) {
            //JetFunctionLiteralExpression
            containingFunction = containingFunction.getParent();
            boolean isInlinedLambda = false;
            JetExpression callable = JetPsiUtil.getCallableParentIfPresent((JetFunctionLiteralExpression) containingFunction);
            if (callable != null) {
                ResolvedCall<?> call = bindingContext.get(BindingContext.RESOLVED_CALL, JetPsiUtil.getCalleeExpressionIfAny(callable));
                CallableDescriptor resultingDescriptor = call == null ? null : call.getResultingDescriptor();
                if (resultingDescriptor instanceof SimpleFunctionDescriptor) {
                    isInlinedLambda = ((SimpleFunctionDescriptor) resultingDescriptor).getInlineStrategy().isInline();
                }
            }
            if (!isInlinedLambda) {
                return false;
            }

            containingFunctionDescriptor = getContainingClassOrFunctionDescriptor(containingFunctionDescriptor, true);

            containingFunction = containingFunctionDescriptor != null
                                 ? BindingContextUtils.descriptorToDeclaration(bindingContext, containingFunctionDescriptor)
                                 : null;
        }

        return returnFunction == containingFunctionDescriptor;
    }

    @Nullable
    public static DeclarationDescriptor getContainingClassOrFunctionDescriptor(@NotNull DeclarationDescriptor descriptor, boolean strict) {
        DeclarationDescriptor currentDescriptor = strict ? descriptor.getContainingDeclaration() : descriptor;
        while (currentDescriptor != null) {
            if (currentDescriptor instanceof FunctionDescriptor || currentDescriptor instanceof ClassDescriptor) {
                return currentDescriptor;
            }
            currentDescriptor = currentDescriptor.getContainingDeclaration();
        }

        return null;
    }

    @Nullable
    public static PsiElement getDeclaration(JetExpression expression) {
        do {
            expression = PsiTreeUtil.getParentOfType(expression, JetDeclaration.class);
        } while (expression instanceof JetMultiDeclaration || expression instanceof JetProperty);
        return expression;
    }
}
