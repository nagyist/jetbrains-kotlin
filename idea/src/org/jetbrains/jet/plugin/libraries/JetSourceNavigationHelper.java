/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.libraries;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.jet.codegen.binding.PsiCodegenPredictor;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.mapping.KotlinToJavaTypesMap;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetTopLevelFunctionsFqnNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetTopLevelPropertiesFqnNameIndex;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.plugin.libraries.MemberMatching.*;

public class JetSourceNavigationHelper {
    private static boolean forceResolve = false;

    private JetSourceNavigationHelper() {
    }

    @Nullable
    private static JetClassOrObject getSourceClassOrObject(@NotNull JetClassOrObject decompiledClassOrObject) {
        if (decompiledClassOrObject instanceof JetObjectDeclaration && decompiledClassOrObject.getParent() instanceof JetClassObject) {
            // class object case

            JetClass decompiledClass = PsiTreeUtil.getParentOfType(decompiledClassOrObject, JetClass.class);
            assert decompiledClass != null;

            JetClass sourceClass = (JetClass) getSourceForNamedClassOrObject(decompiledClass);
            if (sourceClass == null) {
                return null;
            }

            if (sourceClass.hasModifier(JetTokens.ENUM_KEYWORD)) {
                return sourceClass;
            }

            JetClassObject classObject = sourceClass.getClassObject();
            assert classObject != null;
            return classObject.getObjectDeclaration();
        }
        return getSourceForNamedClassOrObject(decompiledClassOrObject);
    }

    @NotNull
    private static GlobalSearchScope createLibrarySourcesScope(@NotNull JetFqNamedDeclaration decompiledDeclaration) {
        JetFile containingFile = (JetFile) decompiledDeclaration.getContainingFile();
        VirtualFile libraryFile = containingFile.getVirtualFile();
        if (libraryFile == null) {
            return GlobalSearchScope.EMPTY_SCOPE;
        }

        Project project = decompiledDeclaration.getProject();
        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        GlobalSearchScope resultScope = GlobalSearchScope.EMPTY_SCOPE;
        for (OrderEntry orderEntry : projectFileIndex.getOrderEntriesForFile(libraryFile)) {
            for (VirtualFile sourceDir : orderEntry.getFiles(OrderRootType.SOURCES)) {
                resultScope = resultScope.uniteWith(GlobalSearchScopes.directoryScope(project, sourceDir, true));
            }
        }
        return resultScope;
    }

    private static List<JetFile> getContainingFiles(@NotNull Iterable<JetFqNamedDeclaration> declarations) {
        Set<JetFile> result = Sets.newHashSet();
        for (JetFqNamedDeclaration declaration : declarations) {
            PsiFile containingFile = declaration.getContainingFile();
            if (containingFile instanceof JetFile) {
                result.add((JetFile) containingFile);
            }
        }
        return Lists.newArrayList(result);
    }

    private static boolean haveRenamesInImports(@NotNull List<JetFile> files) {
        for (JetFile file : files) {
            for (JetImportDirective importDirective : file.getImportDirectives()) {
                if (importDirective.getAliasName() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static JetFqNamedDeclaration findSpecialProperty(@NotNull Name memberName, @NotNull JetClass containingClass) {
        // property constructor parameters
        List<JetParameter> constructorParameters = containingClass.getPrimaryConstructorParameters();
        for (JetParameter constructorParameter : constructorParameters) {
            if (memberName.equals(constructorParameter.getNameAsName()) && constructorParameter.getValOrVarNode() != null) {
                return constructorParameter;
            }
        }

        // enum entries
        if (containingClass.hasModifier(JetTokens.ENUM_KEYWORD)) {
            for (JetEnumEntry enumEntry : ContainerUtil.findAll(containingClass.getDeclarations(), JetEnumEntry.class)) {
                if (memberName.equals(enumEntry.getNameAsName())) {
                    return enumEntry;
                }
            }
        }
        return null;
    }

    @Nullable
    private static JetFqNamedDeclaration getSourcePropertyOrFunction(@NotNull JetFqNamedDeclaration decompiledDeclaration) {
        String memberNameAsString = decompiledDeclaration.getName();
        assert memberNameAsString != null;
        Name memberName = Name.identifier(memberNameAsString);

        PsiElement decompiledContainer = decompiledDeclaration.getParent();

        Collection<JetFqNamedDeclaration> candidates;
        if (decompiledContainer instanceof JetFile) {
            candidates = getInitialTopLevelCandidates(decompiledDeclaration);
        }
        else if (decompiledContainer instanceof JetClassBody) {
            JetClassOrObject decompiledClassOrObject = (JetClassOrObject) decompiledContainer.getParent();
            JetClassOrObject sourceClassOrObject = getSourceClassOrObject(decompiledClassOrObject);

            //noinspection unchecked
            candidates = sourceClassOrObject == null
                         ? Collections.<JetFqNamedDeclaration>emptyList()
                         : getInitialMemberCandidates(sourceClassOrObject, memberName, (Class<JetFqNamedDeclaration>) decompiledDeclaration.getClass());

            if (candidates.isEmpty()) {
                if (decompiledDeclaration instanceof JetProperty && sourceClassOrObject instanceof JetClass) {
                    return findSpecialProperty(memberName, (JetClass) sourceClassOrObject);
                }
            }
        }
        else {
            throw new IllegalStateException("Unexpected container of decompiled declaration: "
                                            + decompiledContainer.getClass().getSimpleName());
        }

        if (candidates.isEmpty()) {
            return null;
        }

        if (!forceResolve) {
            candidates = filterByReceiverPresenceAndParametersCount(decompiledDeclaration, candidates);

            if (candidates.size() <= 1) {
                return candidates.isEmpty() ? null : candidates.iterator().next();
            }

            if (!haveRenamesInImports(getContainingFiles(candidates))) {
                candidates = filterByReceiverAndParameterTypes(decompiledDeclaration, candidates);

                if (candidates.size() <= 1) {
                    return candidates.isEmpty() ? null : candidates.iterator().next();
                }
            }
        }
        Project project = decompiledDeclaration.getProject();
        LockBasedStorageManager storageManager = new LockBasedStorageManager();
        FileBasedDeclarationProviderFactory providerFactory = new FileBasedDeclarationProviderFactory(storageManager, getContainingFiles(candidates),
                new Predicate<FqName>() {
                    @Override
                    public boolean apply(@Nullable FqName fqName) {
                        return KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(fqName);
                    }
                });
        ModuleDescriptorImpl moduleDescriptor = new ModuleDescriptorImpl(Name.special("<library module>"),
                                                                         DefaultModuleConfiguration.DEFAULT_JET_IMPORTS,
                                                                         PlatformToKotlinClassMap.EMPTY);
        moduleDescriptor.setModuleConfiguration(DefaultModuleConfiguration.INSTANCE);
        KotlinCodeAnalyzer analyzer = new ResolveSession(
                project,
                storageManager,
                moduleDescriptor,
                providerFactory);

        for (JetFqNamedDeclaration candidate : candidates) {
            //noinspection unchecked
            CallableDescriptor candidateDescriptor = (CallableDescriptor) analyzer.resolveToDescriptor(candidate);
            if (receiversMatch(decompiledDeclaration, candidateDescriptor)
                    && valueParametersTypesMatch(decompiledDeclaration, candidateDescriptor)
                    && typeParametersMatch((JetTypeParameterListOwner) decompiledDeclaration, candidateDescriptor.getTypeParameters())) {
                return candidate;
            }
        }

        return null;
    }

    @Nullable
    private static JetClassOrObject getSourceForNamedClassOrObject(@NotNull JetClassOrObject decompiledClassOrObject) {
        FqName classFqName = decompiledClassOrObject.getFqName();
        assert classFqName != null;

        GlobalSearchScope librarySourcesScope = createLibrarySourcesScope(decompiledClassOrObject);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return null;
        }
        Collection<JetClassOrObject> classes = JetFullClassNameIndex.getInstance()
                .get(classFqName.asString(), decompiledClassOrObject.getProject(), librarySourcesScope);
        if (classes.isEmpty()) {
            return null;
        }
        return classes.iterator().next(); // if there are more than one class with this FQ, find first of them
    }

    @NotNull
    private static Collection<JetFqNamedDeclaration> getInitialTopLevelCandidates(@NotNull JetFqNamedDeclaration decompiledDeclaration) {
        FqName memberFqName = decompiledDeclaration.getFqName();
        assert memberFqName != null;

        GlobalSearchScope librarySourcesScope = createLibrarySourcesScope(decompiledDeclaration);
        if (librarySourcesScope == GlobalSearchScope.EMPTY_SCOPE) { // .getProject() == null for EMPTY_SCOPE, and this breaks code
            return Collections.emptyList();
        }
        //noinspection unchecked
        StringStubIndexExtension<JetFqNamedDeclaration> index =
                (StringStubIndexExtension<JetFqNamedDeclaration>) getIndexForTopLevelPropertyOrFunction(decompiledDeclaration);
        return index.get(memberFqName.asString(), decompiledDeclaration.getProject(), librarySourcesScope);
    }

    private static StringStubIndexExtension<? extends JetFqNamedDeclaration> getIndexForTopLevelPropertyOrFunction(
            @NotNull JetFqNamedDeclaration decompiledDeclaration
    ) {
        if (decompiledDeclaration instanceof JetNamedFunction) {
            return JetTopLevelFunctionsFqnNameIndex.getInstance();
        }
        if (decompiledDeclaration instanceof JetProperty) {
            return JetTopLevelPropertiesFqnNameIndex.getInstance();
        }
        throw new IllegalArgumentException("Neither function nor declaration: " + decompiledDeclaration.getClass().getName());
    }

    @NotNull
    private static List<JetFqNamedDeclaration> getInitialMemberCandidates(
            @NotNull JetClassOrObject sourceClassOrObject,
            @NotNull final Name name,
            @NotNull Class<JetFqNamedDeclaration> declarationClass
    ) {
        List<JetFqNamedDeclaration> allByClass = ContainerUtil.findAll(sourceClassOrObject.getDeclarations(), declarationClass);
        return ContainerUtil.filter(allByClass, new Condition<JetFqNamedDeclaration>() {
            @Override
            public boolean value(JetFqNamedDeclaration declaration) {
                return name.equals(declaration.getNameAsSafeName());
            }
        });
    }

    @NotNull
    private static List<JetFqNamedDeclaration> filterByReceiverPresenceAndParametersCount(
            final @NotNull JetFqNamedDeclaration decompiledDeclaration,
            @NotNull Collection<JetFqNamedDeclaration> candidates
    ) {
        return ContainerUtil.filter(candidates, new Condition<JetFqNamedDeclaration>() {
            @Override
            public boolean value(JetFqNamedDeclaration candidate) {
                return sameReceiverPresenceAndParametersCount(candidate, decompiledDeclaration);
            }
        });
    }

    @NotNull
    private static List<JetFqNamedDeclaration> filterByReceiverAndParameterTypes(
            final @NotNull JetFqNamedDeclaration decompiledDeclaration,
            @NotNull Collection<JetFqNamedDeclaration> candidates
    ) {
        return ContainerUtil.filter(candidates, new Condition<JetFqNamedDeclaration>() {
            @Override
            public boolean value(JetFqNamedDeclaration candidate) {
                return receiverAndParametersShortTypesMatch(candidate, decompiledDeclaration);
            }
        });
    }

    @Nullable
    private static JetFqNamedDeclaration getSourceProperty(@NotNull JetProperty decompiledProperty) {
        return getSourcePropertyOrFunction(decompiledProperty);
    }

    @Nullable
    private static JetFqNamedDeclaration getSourceFunction(@NotNull JetNamedFunction decompiledFunction) {
        return getSourcePropertyOrFunction(decompiledFunction);
    }

    @TestOnly
    static void setForceResolve(boolean forceResolve) {
        JetSourceNavigationHelper.forceResolve = forceResolve;
    }

    @Nullable
    public static PsiClass getOriginalPsiClassOrCreateLightClass(@NotNull JetClassOrObject classOrObject) {
        if (LightClassUtil.belongsToKotlinBuiltIns((JetFile) classOrObject.getContainingFile())) {
            Name className = classOrObject.getNameAsName();
            assert className != null : "Class from BuiltIns should have a name";
            ClassDescriptor classDescriptor = KotlinBuiltIns.getInstance().getBuiltInClassByName(className);
            Type javaAnalog = KotlinToJavaTypesMap.getInstance().getJavaAnalog(classDescriptor.getDefaultType());
            if (javaAnalog != null) {
                if (AsmUtil.isPrimitive(javaAnalog)) {
                    javaAnalog = KotlinToJavaTypesMap.getInstance().getJavaAnalog(TypeUtils.makeNullable(classDescriptor.getDefaultType()));
                    assert javaAnalog != null : "Java analog should exists for primitive nullable type";
                }
                if (javaAnalog.getSort() != Type.OBJECT) {
                    return null;
                }
                String fqName = JvmClassName.byType(javaAnalog).getFqName().asString();
                return JavaPsiFacade.getInstance(classOrObject.getProject()).
                        findClass(fqName, GlobalSearchScope.allScope(classOrObject.getProject()));
            }
        }
        if (!JetPsiUtil.isLocalClass(classOrObject)) {
            return LightClassUtil.getPsiClass(classOrObject);
        }
        return null;
    }

    @Nullable
    public static PsiClass getOriginalClass(@NotNull JetClassOrObject classOrObject) {
        // Copied from JavaPsiImplementationHelperImpl:getOriginalClass()
        JvmClassName className = PsiCodegenPredictor.getPredefinedJvmClassName(classOrObject);
        if (className == null) {
            return null;
        }
        String fqName = className.getFqName().asString();

        JetFile file = (JetFile) classOrObject.getContainingFile();

        VirtualFile vFile = file.getVirtualFile();
        Project project = file.getProject();

        final ProjectFileIndex idx = ProjectRootManager.getInstance(project).getFileIndex();

        if (vFile == null || !idx.isInLibrarySource(vFile)) return null;
        final Set<OrderEntry> orderEntries = new THashSet<OrderEntry>(idx.getOrderEntriesForFile(vFile));

        PsiClass original = JavaPsiFacade.getInstance(project).findClass(fqName, new GlobalSearchScope(project) {
            @Override
            public int compare(VirtualFile file1, VirtualFile file2) {
                return 0;
            }

            @Override
            public boolean contains(VirtualFile file) {
                List<OrderEntry> entries = idx.getOrderEntriesForFile(file);
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < entries.size(); i++) {
                    OrderEntry entry = entries.get(i);
                    if (orderEntries.contains(entry)) return true;
                }
                return false;
            }

            @Override
            public boolean isSearchInModuleContent(@NotNull Module aModule) {
                return false;
            }

            @Override
            public boolean isSearchInLibraries() {
                return true;
            }
        });

        return original;
    }

    @Nullable
    static JetNamedDeclaration findSourceDeclaration(@NotNull JetNamedDeclaration original) {
        return original.accept(new SourceForDecompiledExtractingVisitor(), null);
    }

    private static class SourceForDecompiledExtractingVisitor extends JetVisitor<JetNamedDeclaration, Void> {
        @Override
        public JetNamedDeclaration visitNamedFunction(JetNamedFunction function, Void data) {
            return getSourceFunction(function);
        }

        @Override
        public JetNamedDeclaration visitProperty(JetProperty property, Void data) {
            return getSourceProperty(property);
        }

        @Override
        public JetNamedDeclaration visitObjectDeclaration(JetObjectDeclaration declaration, Void data) {
            return getSourceClassOrObject(declaration);
        }

        @Override
        public JetNamedDeclaration visitClass(JetClass klass, Void data) {
            return getSourceClassOrObject(klass);
        }
    }
}
