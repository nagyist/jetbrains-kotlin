/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text.regex

/** Gets canonical class for given codepoint from decomposition mappings table. */
internal expect fun getCanonicalClassInternal(ch: Int): Int

/** Check if the given character is in table of single decompositions. */
internal expect fun hasSingleCodepointDecompositionInternal(ch: Int): Boolean

/**
 * Decomposes the given string represented as an array of codepoints. Saves the decomposition into [outputCodepoints] array.
 * Returns the length of the decomposition.
 */
internal expect fun decomposeString(inputCodePoints: IntArray, inputLength: Int, outputCodePoints: IntArray): Int

/**
 * Decomposes the given codepoint. Saves the decomposition into [outputCodepoints] array starting with [fromIndex].
 * Returns the length of the decomposition.
 */
internal expect fun decomposeCodePoint(codePoint: Int, outputCodePoints: IntArray, fromIndex: Int): Int