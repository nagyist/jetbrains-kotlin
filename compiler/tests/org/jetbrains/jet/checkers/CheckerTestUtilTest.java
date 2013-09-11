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

package org.jetbrains.jet.checkers;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;

import java.util.Collections;
import java.util.List;

public class CheckerTestUtilTest extends JetLiteFixture {

    public CheckerTestUtilTest() {
        super("diagnostics/checkerTestUtil");
    }

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }


    protected void doTest(TheTest theTest) throws Exception {
        prepareForTest("test");
        theTest.test(getFile());
    }

    public void testEquals() throws Exception {
        doTest(new TheTest() {
            @Override
            protected void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges) {
            }
        });
    }

    public void testMissing() throws Exception {
        final DiagnosticData typeMismatch1 = diagnostics.get(1);
        doTest(new TheTest(missing(typeMismatch1)) {
            @Override
            protected void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges) {
                diagnostics.remove(typeMismatch1.index);
            }
        });
    }

    public void testUnexpected() throws Exception {
        final DiagnosticData typeMismatch1 = diagnostics.get(1);
        doTest(new TheTest(unexpected(typeMismatch1)) {
            @Override
            protected void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.remove(typeMismatch1.index);
            }
        });
    }

    public void testBoth() throws Exception {
        final DiagnosticData typeMismatch1 = diagnostics.get(1);
        final DiagnosticData unresolvedReference = diagnostics.get(6);
        doTest(new TheTest(unexpected(typeMismatch1), missing(unresolvedReference)) {
            @Override
            protected void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.remove(typeMismatch1.rangeIndex);
                diagnostics.remove(unresolvedReference.index);
            }
        });
    }

    public void testMissingInTheMiddle() throws Exception {
        final DiagnosticData noneApplicable = diagnostics.get(4);
        final DiagnosticData typeMismatch3 = diagnostics.get(5);
        doTest(new TheTest(unexpected(noneApplicable), missing(typeMismatch3)) {
            @Override
            protected void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.remove(noneApplicable.rangeIndex);
                diagnostics.remove(typeMismatch3.index);
            }
        });
    }

    private static abstract class TheTest {
        private final String[] expected;

        protected TheTest(String... expectedMessages) {
            this.expected = expectedMessages;
        }

        public void test(@NotNull PsiFile psiFile) {
            BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(
                    (JetFile) psiFile,Collections.<AnalyzerScriptParameter>emptyList())
                    .getBindingContext();

            String expectedText = CheckerTestUtil.addDiagnosticMarkersToText(psiFile, CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, psiFile)).toString();

            List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
            CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges);
            for (CheckerTestUtil.DiagnosedRange diagnosedRange : diagnosedRanges) {
                diagnosedRange.setFile(psiFile);
            }

            List<Diagnostic> diagnostics = CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, psiFile);
            Collections.sort(diagnostics, CheckerTestUtil.DIAGNOSTIC_COMPARATOR);

            makeTestData(diagnostics, diagnosedRanges);

            List<String> expectedMessages = Lists.newArrayList(expected);
            final List<String> actualMessages = Lists.newArrayList();

            CheckerTestUtil.diagnosticsDiff(diagnosedRanges, diagnostics, new CheckerTestUtil.DiagnosticDiffCallbacks() {

                @Override
                public void missingDiagnostic(String type, int expectedStart, int expectedEnd) {
                    actualMessages.add(missing(type, expectedStart, expectedEnd));
                }

                @Override
                public void unexpectedDiagnostic(String type, int actualStart, int actualEnd) {
                    actualMessages.add(unexpected(type, actualStart, actualEnd));
                }
            });

            assertEquals(listToString(expectedMessages), listToString(actualMessages));
        }

        private String listToString(List<String> expectedMessages) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String expectedMessage : expectedMessages) {
                stringBuilder.append(expectedMessage).append("\n");
            }
            return stringBuilder.toString();
        }

        protected abstract void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges);
    }

    private static String unexpected(String type, int actualStart, int actualEnd) {
        return "Unexpected " + type + " at " + actualStart + " to " + actualEnd;
    }

    private static String missing(String type, int expectedStart, int expectedEnd) {
        return "Missing " + type + " at " + expectedStart + " to " + expectedEnd;
    }

    private static String unexpected(DiagnosticData data) {
        return unexpected(data.name, data.startOffset, data.endOffset);
    }

    private static String missing(DiagnosticData data) {
        return missing(data.name, data.startOffset, data.endOffset);
    }

    private static class DiagnosticData {
        public int index;
        public int rangeIndex;
        public String name;
        public int startOffset;
        public int endOffset;

        private DiagnosticData(int index, int rangeIndex, String name, int startOffset, int endOffset) {
            this.index = index;
            this.rangeIndex = rangeIndex;
            this.name = name;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }

    private final List<DiagnosticData> diagnostics = Lists.newArrayList(
            new DiagnosticData(0, 0, "UNUSED_PARAMETER", 8, 9),
            new DiagnosticData(1, 1, "TYPE_MISMATCH", 56, 57),
            new DiagnosticData(2, 2, "UNUSED_VARIABLE", 67, 68),
            new DiagnosticData(3, 3, "TYPE_MISMATCH", 98, 99),
            new DiagnosticData(4, 4, "NONE_APPLICABLE", 120, 121),
            new DiagnosticData(5, 5, "TYPE_MISMATCH", 159, 167),
            new DiagnosticData(6, 6, "UNRESOLVED_REFERENCE", 164, 166),
            new DiagnosticData(7, 6, "TOO_MANY_ARGUMENTS", 164, 166)
    );
}
