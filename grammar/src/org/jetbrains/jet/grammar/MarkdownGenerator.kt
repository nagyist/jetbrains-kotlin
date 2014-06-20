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

package org.jetbrains.jet.grammar

import com.google.common.collect.*
import com.intellij.openapi.util.io.FileUtil
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.Reader
import java.util.*
import java.util.regex.Pattern
import java.awt.Toolkit

private val GRAMMAR_EXTENSION: String = "grm"
private val FILE_NAMES_IN_ORDER = listOf("notation", "toplevel", "class", "class_members", "enum", "types", "control", "expressions", "when", "modifiers", "attributes", "lexical")

public fun main(args: Array<String>) {
    val grammarDir = File("grammar/src")

    val used = HashSet<File>()
    val tokens = getJoinedTokensFromAllFiles(grammarDir, used)
    assertAllFilesAreUsed(grammarDir, used)

    val result = generate(tokens)

    val outFile = File("grammar/out.txt")
    if (!outFile.exists()) {
        outFile.writeText(result.toString())
        throw AssertionError("Output file does not exist, created: $outFile")
    }

    if (outFile.readText() != result.toString()) {
        val errFile = File("grammar/out.wrong.txt")
        errFile.writeText(result.toString())
        throw AssertionError("File content mismatch, see $errFile")
    }

    copyToClipboard(result)
}

private fun getJoinedTokensFromAllFiles(grammarDir: File, used: MutableSet<File>): List<Token> {
    val allTokens = arrayListOf<Token>()
    for (fileName in FILE_NAMES_IN_ORDER) {
        val file = File(grammarDir, fileName + "." + GRAMMAR_EXTENSION)
        used.add(file)
        val text = FileUtil.loadFile(file, true)
        val textWithMarkedDeclarations = markDeclarations(text)
        val tokens = tokenize(createLexer(file.getPath(), textWithMarkedDeclarations))
        allTokens.addAll(tokens)
    }
    return allTokens
}

private fun createLexer(fileName: String, output: StringBuilder): _GrammarLexer {
    val grammarLexer = _GrammarLexer(null as Reader?)
    grammarLexer.reset(output, 0, output.length(), 0)
    grammarLexer.setFileName(fileName)
    return grammarLexer
}

private fun assertAllFilesAreUsed(grammarDir: File, used: Set<File>) {
    for (file in grammarDir.listFiles()!!) {
        if (file.getName().endsWith(GRAMMAR_EXTENSION)) {
            if (!used.contains(file)) {
                throw IllegalStateException("Unused grammar file : " + file.getAbsolutePath())
            }
        }
    }
}

private fun markDeclarations(allRules: CharSequence): StringBuilder {
    val output = StringBuilder()

    val symbolReference = Pattern.compile("^\\w+$", Pattern.MULTILINE)
    val matcher = symbolReference.matcher(allRules)
    var copiedUntil = 0
    while (matcher.find()) {
        val start = matcher.start()
        output.append(allRules.subSequence(copiedUntil, start))

        val group = matcher.group()
        output.append("&").append(group)
        copiedUntil = matcher.end()
    }
    output.append(allRules.subSequence(copiedUntil, allRules.length()))
    return output
}

private fun generate(tokens: List<Token>): StringBuilder {
    val result = StringBuilder("h1. Contents\n").append("{toc:style=disc|indent=20px}")

    val declaredSymbols = HashSet<String>()
    val usedSymbols = HashSet<String>()
    val usages = Multimaps.newSetMultimap<String, String>(HashMap(), { Sets.newHashSet() })!!

    var lastDeclaration: Declaration? = null
    for (advance in tokens) {
        if (advance is Declaration) {
            val declaration = advance as Declaration
            lastDeclaration = declaration
            declaredSymbols.add(declaration.getName())
        }
        else
            if (advance is Identifier) {
                val identifier = advance as Identifier
                assert(lastDeclaration != null)
                usages.put(identifier.getName(), lastDeclaration?.getName())
                usedSymbols.add(identifier.getName())
            }
    }

    for (token in tokens) {
        if (token is Declaration) {
            val declaration = token as Declaration
            result.append("{anchor:").append(declaration.getName()).append("}")
            if (!usedSymbols.contains(declaration.getName())) {
                //                    result.append("(!) *Unused!* ");
                System.out.println("Unused: " + tokenWithPosition(token))
            }
            val myUsages = usages[declaration.getName()]!!
            if (!myUsages.isEmpty()) {
                result.append("\\[{color:grey}Used by ")
                result.append(myUsages.map { "[#$it]" }.join(", "))
                result.append("{color}\\]\n")
            }
            result.append(token)
            continue
        }
        else
            if (token is Identifier) {
                val identifier = token as Identifier
                if (!declaredSymbols.contains(identifier.getName())) {
                    result.append("(!) *Undeclared!* ")
                    System.out.println("Undeclared: " + tokenWithPosition(token))
                }
            }
        result.append(token)
    }
    return result
}

private fun tokenWithPosition(token: Token): String {
    return "$token at ${token.getFileName()}:${token.getLine()}"
}

private fun tokenize(grammarLexer: _GrammarLexer): List<Token> {
    val tokens = ArrayList<Token>()
    while (true) {
        val advance = grammarLexer.advance()
        if (advance == null) {
            break
        }
        tokens.add(advance)
    }
    return tokens
}

private fun copyToClipboard(result: StringBuilder) {
    val clipboard = Toolkit.getDefaultToolkit()?.getSystemClipboard()
    clipboard?.setContents(StringSelection(result.toString()), {x, y -> })
}