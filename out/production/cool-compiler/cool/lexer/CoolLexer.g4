lexer grammar CoolLexer;

tokens { ERROR }

@header{
    package cool.lexer;
}

@members{
    private void raiseError(String msg) {
        setText(msg);
        setType(ERROR);
    }
}

// Keywords
CLASS : ('c'|'C') ('l'|'L') ('a'|'A') ('s'|'S') ('s'|'S');  // case insensitive keyword for CLASS
INHERITS : 'inherits';      // All of the keywords except true and false can be case insensitive in COOL, but
ISVOID : 'isvoid';          // it's a tedious process to write each letter with both upper and lower case:(
LET : 'let';
IN : 'in';
LOOP : 'loop';
POOL : 'pool';
WHILE : 'while';
CASE : 'case';
ESAC : 'esac';
NEW : 'new';
OF : 'of';
NOT : 'not';

IF : 'if';
THEN : 'then';
ELSE : 'else';
FI: 'fi';

//TYPE : 'Int' | 'String' | 'Bool' | 'SELF_TYPE' | 'Object';
fragment UPPERCASE_LETTER: [A-Z];
fragment LETTER: [a-zA-Z];
fragment DIGIT : [0-9];
TYPE: UPPERCASE_LETTER (LETTER | '_' | DIGIT)*;

BOOL : 'true' | 'false';

INT : DIGIT+;

ID : (LETTER | '_')(LETTER | '_' | DIGIT)*;

fragment LINE_TERMINATOR : '\n' | '\r\n';
fragment ESCAPED_LINE : ('\\' LINE_TERMINATOR);
STRING : '"' (ESCAPED_LINE | ~('\n'))*? '"'
    {
        String text = getText();
        StringBuilder s = new StringBuilder();
        String specialChars = "ntbf";
        String specialCharsEscaped = "\n\t\b\f";

        for (int i = 1; i < text.length() - 1; ++i) {
            if (text.charAt(i) != '\\') {
                s.append(text.charAt(i));
            } else {
                // Always i+1 is a valid index, within bounds
                int pos = specialChars.indexOf(text.charAt(i + 1));

                // Add a special char in the result
                if (pos >= 0) {
                    s.append(specialCharsEscaped.charAt(pos));
                    i += 1;
                } else if (text.charAt(i + 1) == '\\') {
                    // Double backslash should generate a single backslash in string
                    s.append(text.charAt(i));
                    i += 1;
                }
            }
        }

        if (s.length() > 1024) {
            raiseError("String constant too long");
        } else if (s.indexOf("\0") >= 0) {
            raiseError("String contains null character");
        } else {
            setText(s.toString());
        }
    };

// These rules are not fragment to avoid the ANTLR4 feature of matching the
// first rule instead of the longest match, when the non greedy operator is
// found (otherwise, STR_ERR would identify EOF errors as Unterminated string
// errors, and to solve that the order of the alternatives should've been switched
// in STR_ERR)
UNTERMINATED_STRING_CONSTANT : '"' (~('"'))*? LINE_TERMINATOR { raiseError("Unterminated string constant"); };
EOF_IN_STRING_CONSTANT : '"' (~('"'))*? EOF { raiseError("EOF in string constant"); };

STR_ERR : UNTERMINATED_STRING_CONSTANT { raiseError("Unterminated string constant"); }
        | EOF_IN_STRING_CONSTANT { raiseError("EOF in string constant"); };

COLON : ':';

SEMI : ';';

COMMA : ',';

POINT : '.';

ASSIGN : '<-';

LPAREN : '(';

RPAREN : ')';

LBRACE : '{';

RBRACE : '}';

PLUS : '+';

UMINUS : '~';

MINUS : '-';

MULT : '*';

DIV : '/';

EQUAL : '=';

LT : '<';

LE : '<=';

ARROW : '=>';

AT : '@';

BLOCK_COMMENT : '(*' (BLOCK_COMMENT | .)*? '*)' -> skip;

// Using '*)' after the non-greedy part, we make sure that
// if the ending part of the block comment is on the last
// position in the file, this rule does not match, because
// BLOCK_COMMENT rule will match first, and the matching
// size is the same for both rules
EOF_IN_COMMENT : '(*' (BLOCK_COMMENT | .)*? (EOF | '*)') { raiseError("EOF in comment"); };

UNMATCHED_BLOCK_COMMENT_END : '*)' { raiseError("Unmatched *)"); };

COMMENT : '--' .*? (LINE_TERMINATOR | EOF) -> skip;

WS
    :   [ \n\f\r\t]+ -> skip
    ;

INVALID_CHARACTER : . { raiseError("Invalid character: " + getText()); };
