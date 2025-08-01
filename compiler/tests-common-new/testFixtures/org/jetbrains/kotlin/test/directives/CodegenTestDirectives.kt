/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability.File
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability.Global
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider

object CodegenTestDirectives : SimpleDirectivesContainer() {
    val IGNORE_BACKEND by enumDirective<TargetBackend>(
        description = "Ignore failures of test on target backend",
        applicability = Global
    )

    val IGNORE_BACKEND_K1 by enumDirective<TargetBackend>(
        description = "Ignore specific backend if test uses K1 frontend",
        applicability = Global
    )

    val IGNORE_BACKEND_K2 by enumDirective<TargetBackend>(
        description = "Ignore specific backend if test uses K2 frontend",
        applicability = Global
    )

    val IGNORE_BACKEND_MULTI_MODULE by enumDirective<TargetBackend>(
        description = "Ignore failures of multimodule test on target backend",
        applicability = Global
    )

    val IGNORE_BACKEND_K2_MULTI_MODULE by enumDirective<TargetBackend>(
        description = "Ignore failures of multimodule test on target backend if test uses K2 frontend",
        applicability = Global
    )

    val IGNORE_HMPP by enumDirective<TargetBackend>("Ignore test in HMPP setup")

    val USE_JAVAC_BASED_ON_JVM_TARGET by directive(
        description = """
            Determine version of javac for compilation of java files based
              on JvmTarget of module. If not enabled then javac from
              current runtime will be used
        """.trimIndent()
    )

    val JAVAC_OPTIONS by stringDirective(
        description = "Specify javac options to compile java files"
    )

    val WITH_HELPERS by directive(
        """
            Adds util functions for checking coroutines
            See files in ./compiler/testData/codegen/helpers/
        """.trimIndent()
    )

    val CHECK_BYTECODE_TEXT by directive(
        description = "Check for particular patterns present in generated bytecode",
        applicability = Global
    )

    val CHECK_BYTECODE_LISTING by directive(
        description = "Dump generated classes to .txt or _ir.txt file",
        applicability = Global
    )

    val WITH_SIGNATURES by directive(
        description = "Include generic signatures in generated bytecode listing",
        applicability = Global
    )

    val IGNORE_ANNOTATIONS by directive(
        description = "Do not render annotations in generated bytecode listing",
        applicability = Global
    )

    val DONT_SORT_DECLARATIONS by directive(
        description = "Do not sort declarations",
        applicability = Global
    )

    val RUN_DEX_CHECKER by directive(
        description = "Run DxChecker and D8Checker"
    )

    val IGNORE_DEXING by directive(
        description = "Ignore dex checkers"
    )

    val IGNORE_ERRORS by directive(
        description = """
            Ignore frontend errors in ${NoCompilationErrorsHandler::class}
            If this directive is enabled then ${JvmIrBackendFacade::class} won't produce any binaries for test
              if there are errors in it
        """.trimIndent()
    )

    val IGNORE_FIR_DIAGNOSTICS by directive(
        description = "Run backend even FIR reported some diagnostics with ERROR severity"
    )

    val IGNORE_FIR_DIAGNOSTICS_DIFF by directive(
        description = "Don't compare diagnostics in testdata for FIR codegen tests"
    )

    val IGNORE_BACKEND_DIAGNOSTICS by directive(
        description = "Prevent adding backend diagnostics to GlobalMetadataInfoHandler. This is needed when the backend is executed for tests that originally were not designed for it."
    )

    val DUMP_IR by directive(
        description = "Dumps generated backend IR (enables ${IrTextDumpHandler::class})"
    )

    val DUMP_IR_AFTER_INLINE by directive(
        description = "Dumps generated backend IR after inlining (enables ${IrTextDumpHandler::class} after inlining)"
    )

    val DUMP_EXTERNAL_CLASS by stringDirective(
        description = "Specifies names of external classes which IR should be dumped"
    )

    val EXTERNAL_FILE by directive(
        description = "Indicates that test file is external and should be skipped in ${IrTextDumpHandler::class}",
        applicability = File
    )

    val DUMP_KT_IR by directive(
        description = "Dumps generated backend IR in pretty kotlin dump (enables ${IrPrettyKotlinDumpHandler::class})"
    )

    val DUMP_SOURCE_RANGES_IR by directive(
        description = "Dumps generated backend IR together with elements source ranges (enables ${IrSourceRangesDumpHandler::class})"
    )

    val SKIP_KT_DUMP by directive(
        description = "Skips check pretty kt IR dump (disables ${IrPrettyKotlinDumpHandler::class})"
    )

    val DUMP_SIGNATURES by directive(
        description = """
        Like $DUMP_KT_IR, but does not dump function bodies, and prints a rendered binary signature and a mangled name for each declaration
        (enables ${IrMangledNameAndSignatureDumpHandler::class})
        """.trimIndent()
    )

    val SEPARATE_SIGNATURE_DUMP_FOR_K2 by directive(
        description = """
            Usually the signature dump must not differ between K1 and K2.
            There are rare cases, however, when there is legitimate difference (for example, if the set of fake overrides is different).
            Please always document the usage of this directive and carefully verify that the difference between K1 and K2 does
            not affect IR linkage.
            """.trimIndent()
    )

    val MUTE_SIGNATURE_COMPARISON_K2 by enumDirective<TargetBackend>(
        description = "Ignores failures of signature dump comparison for tests with the $DUMP_SIGNATURES directive if the test uses the K2 frontend and the specified backend."
    )

    val DUMP_IR_FOR_GIVEN_PHASES by stringDirective(
        description = "Dumps backend IR after given lowerings (enables ${PhasedIrDumpHandler::class})",
    )

    val TREAT_AS_ONE_FILE by directive(
        description = "Treat bytecode from all files as one in ${BytecodeTextHandler::class}"
    )

    val NO_CHECK_LAMBDA_INLINING by directive(
        description = "Skip checking of lambda inlining in ${BytecodeInliningHandler::class.java}"
    )

    val SKIP_INLINE_CHECK_IN by stringDirective(
        description = "Skip checking of specific methods in ${BytecodeInliningHandler::class.java}"
    )

    val DUMP_SMAP by directive(
        description = """Enables ${SMAPDumpHandler::class}"""
    )

    val NO_SMAP_DUMP by directive(
        description = "Don't dump smap for marked file",
        applicability = File
    )

    val SEPARATE_SMAP_DUMPS by directive(
        description = """
            If enabled then ${SMAPDumpHandler::class} will dump smap dumps
              into ${SMAPDumpHandler.SMAP_SEP_EXT} and ${SMAPDumpHandler.SMAP_EXT}
              files instead of ${SMAPDumpHandler.SMAP_EXT} depending of module
              structure of test
        """.trimIndent()
    )

    val ATTACH_DEBUGGER by directive(
        description = """
            This directive can be used to attach debugger to instance of JVM which
              is used to run `box` test in case it runs in separate JVM instance
              (e.g. when this case should be ran on some modern JDK like JDK 17)
              
            After running test run remote debugger on port 5005 (test will wait until
              debugger won't be attached)
              
            Please don't forget to remove this directive after debug session is over 
        """.trimIndent()
    )

    val REQUIRES_SEPARATE_PROCESS by directive(
        description = """
            Force run `box` method in separate process even if jdk of current process
            is same as required jdk for test
        """.trimIndent()
    )

    val IGNORE_FIR2IR_EXCEPTIONS_IF_FIR_CONTAINS_ERRORS by directive(
        description = """
            Ignore FIR2IR exceptions if FIR reported some diagnostics with ERROR severity
        """.trimIndent()
    )

    val IGNORE_FIR_METADATA_LOADING_K1 by directive(
        description = """
            Ignore exceptions in AbstractFirLoadK1CompiledKotlin tests
        """.trimIndent()
    )

    val IGNORE_FIR_METADATA_LOADING_K2 by directive(
        description = """
            Ignore exceptions in AbstractFirLoadK2CompiledKotlin tests
        """.trimIndent()
    )

    val JVM_ABI_K1_K2_DIFF by stringDirective(
        description = "Expect difference in JVM ABI between K1 and K2",
        applicability = Global
    )

    val DISABLE_IR_VISIBILITY_CHECKS by enumDirective<TargetBackend>(
        description = "Don't check for visibility violations when validating IR on the target backend"
    )

    val DISABLE_IR_VARARG_TYPE_CHECKS by enumDirective<TargetBackend>(
        description = "Don't check for vararg type mismatches when validating IR on the target backend"
    )
}

fun ValueDirective<TargetBackend>.isApplicableTo(module: TestModule, testServices: TestServices): Boolean {
    val specifiedBackends = module.directives[this]
    return testServices.defaultsProvider.targetBackend in specifiedBackends || TargetBackend.ANY in specifiedBackends
}

fun extractIgnoredDirectiveForTargetBackend(
    testServices: TestServices,
    module: TestModule,
    targetBackend: TargetBackend,
    customIgnoreDirective: ValueDirective<TargetBackend>? = null,
): ValueDirective<TargetBackend>? =
    when (testServices.defaultsProvider.frontendKind) {
        FrontendKinds.ClassicFrontend -> CodegenTestDirectives.IGNORE_BACKEND_K1
        FrontendKinds.FIR -> CodegenTestDirectives.IGNORE_BACKEND_K2
        else -> null
    }?.let { specificIgnoreDirective ->
        when {
            customIgnoreDirective != null -> customIgnoreDirective
            !module.directives.contains(specificIgnoreDirective) -> CodegenTestDirectives.IGNORE_BACKEND
            else -> {
                val inCommonIgnored = module.directives[CodegenTestDirectives.IGNORE_BACKEND].let { targetBackend in it || TargetBackend.ANY in it }
                val inSpecificIgnored = module.directives[specificIgnoreDirective].let { targetBackend in it || TargetBackend.ANY in it }
                if (inCommonIgnored && inSpecificIgnored) {
                    throw AssertionError("Both, IGNORE_BACKEND and ${specificIgnoreDirective.name} contain target backend ${targetBackend.name}. Please remove one of them.")
                }
                if (inCommonIgnored) CodegenTestDirectives.IGNORE_BACKEND else specificIgnoreDirective
            }
        }
    }

