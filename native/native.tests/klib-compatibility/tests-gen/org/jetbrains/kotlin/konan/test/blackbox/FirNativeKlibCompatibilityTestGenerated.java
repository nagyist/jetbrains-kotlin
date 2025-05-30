/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/klib/versionCompatibility")
@TestDataPath("$PROJECT_ROOT")
public class FirNativeKlibCompatibilityTestGenerated extends AbstractNativeKlibCompatibilityTest {
  @Test
  @TestMetadata("addEnumEntry")
  public void testAddEnumEntry() {
    runTest("compiler/testData/klib/versionCompatibility/addEnumEntry/");
  }

  @Test
  public void testAllFilesPresentInVersionCompatibility() {
    KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/klib/versionCompatibility"), Pattern.compile("^([^_](.+))$"), null, false);
  }

  @Test
  @TestMetadata("kt51302")
  public void testKt51302() {
    runTest("compiler/testData/klib/versionCompatibility/kt51302/");
  }

  @Test
  @TestMetadata("varargUsage")
  public void testVarargUsage() {
    runTest("compiler/testData/klib/versionCompatibility/varargUsage/");
  }
}
