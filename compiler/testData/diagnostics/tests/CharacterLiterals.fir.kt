// RUN_PIPELINE_TILL: FRONTEND
fun test(c : Char) {
  test(<!EMPTY_CHARACTER_LITERAL!>''<!>)
  test('a')
  test(<!TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL!>'aa'<!>)
  test(<!INCORRECT_CHARACTER_LITERAL!>'a)<!>
  <!UNRESOLVED_REFERENCE!>test<!>(<!INCORRECT_CHARACTER_LITERAL!>'<!>
  <!UNRESOLVED_REFERENCE!>test<!>(0<!INCORRECT_CHARACTER_LITERAL!><!SYNTAX!><!>'<!>
  <!UNRESOLVED_REFERENCE!>test<!>('\n')
  test('\\')
  test(<!EMPTY_CHARACTER_LITERAL!>''<!><!EMPTY_CHARACTER_LITERAL, TOO_MANY_ARGUMENTS!><!SYNTAX!><!>''<!>)
  test('\'')
  test('\"')
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral */
