// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Hashtable;

import static jminusminus.TokenKind.*;

/**
 * A lexical analyzer for j--, that has no backtracking mechanism.
 */
class Scanner {
	// End of file character.
	public final static char EOFCH = CharReader.EOFCH;

	// Keywords in j--.
	private Hashtable<String, TokenKind> reserved;

	// Source characters.
	private CharReader input;

	// Next unscanned character.
	private char ch;

	// Whether a scanner error has been found.
	private boolean isInError;

	// Source file name.
	private String fileName;

	// Line number of current token.
	private int line;

	/**
	 * Constructs a Scanner from a file name.
	 *
	 * @param fileName name of the source file.
	 * @throws FileNotFoundException when the named file cannot be found.
	 */
	public Scanner(String fileName) throws FileNotFoundException {
		this.input = new CharReader(fileName);
		this.fileName = fileName;
		isInError = false;

		// Keywords in j--
		reserved = new Hashtable<String, TokenKind>();
		reserved.put(ABSTRACT.image(), ABSTRACT);
		reserved.put(BOOLEAN.image(), BOOLEAN);
		reserved.put(CHAR.image(), CHAR);
		reserved.put(CLASS.image(), CLASS);
		reserved.put(ELSE.image(), ELSE);
		reserved.put(EXTENDS.image(), EXTENDS);
		reserved.put(FALSE.image(), FALSE);
		reserved.put(IF.image(), IF);
		reserved.put(IMPORT.image(), IMPORT);
		reserved.put(INSTANCEOF.image(), INSTANCEOF);
		reserved.put(INT.image(), INT);
		reserved.put(NEW.image(), NEW);
		reserved.put(NULL.image(), NULL);
		reserved.put(PACKAGE.image(), PACKAGE);
		reserved.put(PRIVATE.image(), PRIVATE);
		reserved.put(PROTECTED.image(), PROTECTED);
		reserved.put(PUBLIC.image(), PUBLIC);
		reserved.put(RETURN.image(), RETURN);
		reserved.put(STATIC.image(), STATIC);
		reserved.put(SUPER.image(), SUPER);
		reserved.put(THIS.image(), THIS);
		reserved.put(TRUE.image(), TRUE);
		reserved.put(VOID.image(), VOID);
		reserved.put(WHILE.image(), WHILE);
		reserved.put(BREAK.image(), BREAK);
		reserved.put(BYTE.image(), BYTE);
		reserved.put(CASE.image(), CASE);
		reserved.put(CATCH.image(), CATCH);
		reserved.put(CONST.image(), CONST);
		reserved.put(CONTINUE.image(), CONTINUE);
		reserved.put(DEFAULT.image(), DEFAULT);
		reserved.put(DO.image(), DO);
		reserved.put(DOUBLE.image(), DOUBLE);
		reserved.put(ENUM.image(), ENUM);
		reserved.put(FINAL.image(), FINAL);
		reserved.put(FINALLY.image(), FINALLY);
		reserved.put(FLOAT.image(), FLOAT);
		reserved.put(FOR.image(), FOR);
		reserved.put(IMPLEMENTS.image(), IMPLEMENTS);
		reserved.put(INTERFACE.image(), INTERFACE);
		reserved.put(LONG.image(), LONG);
		reserved.put(NATIVE.image(), NATIVE);
		reserved.put(SHORT.image(), SHORT);
		reserved.put(STRICTFP.image(), STRICTFP);
		reserved.put(SWITCH.image(), SWITCH);
		reserved.put(SYNCHRONIZED.image(), SYNCHRONIZED);
		reserved.put(THROW.image(), THROW);
		reserved.put(THROWS.image(), THROWS);
		reserved.put(TRANSIENT.image(), TRANSIENT);
		reserved.put(TRY.image(), TRY);
		reserved.put(VOLATILE.image(), VOLATILE);
		reserved.put(ASSERT.image(), ASSERT);
		reserved.put(GOTO.image(), GOTO);
		reserved.put(UNDERSCORE.image(), UNDERSCORE);

		// Prime the pump.
		nextCh();
	}

	/**
	 * Scans and returns the next token from input.
	 *
	 * @return the next scanned token.
	 */
	@SuppressWarnings("unused")
	public TokenInfo getNextToken() {
		StringBuffer buffer;
		boolean moreWhiteSpace = true;
		while (moreWhiteSpace) {
			while (isWhitespace(ch)) {
				nextCh();
			}
			if (ch == '/') {
				nextCh();
				if (ch == '/') {
					// CharReader maps all new lines to '\n'.
					while (ch != '\n' && ch != EOFCH) {
						nextCh();
					}
					// -------------------------------------------------------------------------
					// TODO
				} else if (ch == '*') { // Added functionality
					boolean endOfMultiLineFound = false;// In case user doesn't put "*/:
					while (ch != EOFCH) {
						if (ch == '*') {
							nextCh();
							if (ch == '/') {
								nextCh();// This skips past to avoid thinking it's a lone '/'
								endOfMultiLineFound = true;// end of comment found
								break;
							}
						} else {
							nextCh();
						}
					}
					if (!endOfMultiLineFound) {
						reportScannerError("No */ at end of multi line comment");
					}

				} else {
//                    reportScannerError("Operator / is not supported in j--");
					if (ch == '=') {
						nextCh();
						return new TokenInfo(DIVASSIGN, line);
					}
					return new TokenInfo(DIV, line);
				}
				// ------------------------------------------------------------------------------
			} else {
				moreWhiteSpace = false;
			}
		}
		line = input.line();
		switch (ch) {
		case ',':
			nextCh();
			return new TokenInfo(COMMA, line);
        case '.':
            // floats can start with .
            nextCh();
            if (isDigit(ch)) {
                buffer = new StringBuffer();
                buffer.append('.');
                while (isDigit(ch) || ch == '_') {
                    buffer.append(ch);
                    nextCh();
                }
                if (ch == 'e' || ch == 'E') {
                    buffer.append(ch);
                    nextCh();
                    if (ch == '+' || ch == '-') {
                        buffer.append(ch);
                        nextCh();
                    }
                    boolean expDigits = false;
                    while (isDigit(ch) || ch == '_') {
                        if (isDigit(ch)) expDigits = true;
                        buffer.append(ch);
                        nextCh();
                    }
                    if (!expDigits) {
                        reportScannerError("Malformed exponent in numeric literal");
                    }
                }
                if (ch == 'f' || ch == 'F') {
                    buffer.append(ch);
                    nextCh();
                    return new TokenInfo(FLOAT_LITERAL, buffer.toString(), line);
                }
                if (ch == 'd' || ch == 'D') {
                    buffer.append(ch);
                    nextCh();
                    return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                }
                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
            } else {
                return new TokenInfo(DOT, line);
            }
		case '[':
			nextCh();
			return new TokenInfo(LBRACK, line);
		case '{':
			nextCh();
			return new TokenInfo(LCURLY, line);
		case '(':
			nextCh();
			return new TokenInfo(LPAREN, line);
		case ']':
			nextCh();
			return new TokenInfo(RBRACK, line);
		case '}':
			nextCh();
			return new TokenInfo(RCURLY, line);
		case ')':
			nextCh();
			return new TokenInfo(RPAREN, line);
		case ';':
			nextCh();
			return new TokenInfo(SEMI, line);
		case '*':
			nextCh();
			if (ch == '=') {
				nextCh();
				return new TokenInfo(STARASSIGN, line);
			}
			return new TokenInfo(STAR, line);
		case '+':
			nextCh();
			if (ch == '=') {
				nextCh();
				return new TokenInfo(PLUS_ASSIGN, line);
			} else if (ch == '+') {
				nextCh();
				return new TokenInfo(INC, line);
			} else {
				return new TokenInfo(PLUS, line);
			}
		case '-':
			nextCh();
			if (ch == '-') {
				nextCh();
				return new TokenInfo(DEC, line);
			} else if (ch == '=') {
				nextCh();
				return new TokenInfo(MINUS_ASSIGN, line);
			} else if (ch == '>') {
				nextCh();
				return new TokenInfo(ARROW, line);
			} else {
				return new TokenInfo(MINUS, line);
			}
		case '=':
			nextCh();
			if (ch == '=') {
				nextCh();
				return new TokenInfo(EQUAL, line);
			} else {
				return new TokenInfo(ASSIGN, line);
			}
		case '>':
			nextCh();
			if (ch == '>') {
				nextCh();
				if (ch == '=') {
					nextCh();
					return new TokenInfo(RSHIFTASSIGN, line);
				}
				if (ch == '>') {
					nextCh();
					if (ch == '=') {
						nextCh();
						return new TokenInfo(RSHIFTUNSIGNEDASSIGN, line);
					}
					return new TokenInfo(RSHIFTUNSIGNED, line);
				}
				return new TokenInfo(RSHIFT, line);
			}
			if (ch == '=') {
				nextCh();
				return new TokenInfo(GE, line);
			}
			return new TokenInfo(GT, line);
		case '<':
			nextCh();
			if (ch == '<') {
				nextCh();
				if (ch == '=') {
					nextCh();
					return new TokenInfo(LSHIFTASSIGN, line);
				}
				return new TokenInfo(LSHIFT, line);
			} else if (ch == '=') {
				nextCh();
				return new TokenInfo(LE, line);
			} else {
				return new TokenInfo(LT, line);
			}
		case '!':
			nextCh();
			if (ch == '=') {
				nextCh();
				return new TokenInfo(NOTEQUAL, line);
			}
			return new TokenInfo(LNOT, line);
		case '&':
			nextCh();
			if (ch == '&') {
				nextCh();
				return new TokenInfo(LAND, line);
			} else if (ch == '=') {
				nextCh();
				return new TokenInfo(BITANDASSIGN, line);
			} else {
				return new TokenInfo(BITAND, line);
			}

			// -----------------------------------------------------------------
			// TODO
		case '%':
			nextCh();
			if (ch == '=') {
				nextCh();
				return new TokenInfo(MODASSIGN, line);
			} else {
				return new TokenInfo(MOD, line);
			}

		case '~':
			nextCh();
			return new TokenInfo(TILDE, line);

		case '?':
			nextCh();
			return new TokenInfo(QUESTION, line);

		case ':':
			nextCh();
			return new TokenInfo(COLON, line);

		case '|':
			nextCh();
			if (ch == '|') {
				nextCh();
				return new TokenInfo(LOR, line);
			}
			if (ch == '=') {
				nextCh();
				return new TokenInfo(BITOR_ASSIGN, line);
			}
			return new TokenInfo(BITOR, line);

		case '^':
			nextCh();
			if (ch == '=') {
				nextCh();
				return new TokenInfo(BITXORASSIGN, line);
			} else {
				return new TokenInfo(BITXOR, line);
			}

			// -----------------------------------------------------------------

		case '\'':
			buffer = new StringBuffer();
			buffer.append('\'');
			nextCh();
			if (ch == '\\') {
				nextCh();
				buffer.append(escape());
			} else {
				buffer.append(ch);
				nextCh();
			}
			if (ch == '\'') {
				buffer.append('\'');
				nextCh();
				return new TokenInfo(CHAR_LITERAL, buffer.toString(), line);
			} else {
				// Expected a ' ; report error and try to recover.
				reportScannerError(ch + " found by scanner where closing ' was expected");
				while (ch != '\'' && ch != ';' && ch != '\n') {
					nextCh();
				}
				return new TokenInfo(CHAR_LITERAL, buffer.toString(), line);
			}
		case '"':
			buffer = new StringBuffer();
			buffer.append("\"");
			nextCh();
			while (ch != '"' && ch != '\n' && ch != EOFCH) {
				if (ch == '\\') {
					nextCh();
					buffer.append(escape());
				} else {
					buffer.append(ch);
					nextCh();
				}
			}
			if (ch == '\n') {
				reportScannerError("Unexpected end of line found in string");
			} else if (ch == EOFCH) {
				reportScannerError("Unexpected end of file found in string");
			} else {
				// Scan the closing "
				nextCh();
				buffer.append("\"");
			}
			return new TokenInfo(STRING_LITERAL, buffer.toString(), line);
		case EOFCH:
			return new TokenInfo(EOF, line);
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            buffer = new StringBuffer();

            // Booleans for literal type
            boolean isDouble = false;
            boolean isFloat = false;
            boolean isLong = false;

            if (ch == '0') {
                buffer.append(ch);
                nextCh();
                if (ch == 'x' || ch == 'X') {
                    buffer.append(ch);
                    nextCh();

                   
                    boolean hasHexDigitsBeforeDot = false;
                    while (isHexDigit(ch) || ch == '_') {
                        if (isHexDigit(ch)) hasHexDigitsBeforeDot = true;
                        buffer.append(ch);
                        nextCh();
                    }

                    boolean isHexFloat = false;
                    boolean hasHexDigitsAfterDot = false;

                    
                    if (ch == '.') {
                        isHexFloat = true;
                        buffer.append(ch);
                        nextCh();
                        while (isHexDigit(ch) || ch == '_') {
                            if (isHexDigit(ch)) hasHexDigitsAfterDot = true;
                            buffer.append(ch);
                            nextCh();
                        }
                        if (!hasHexDigitsBeforeDot && !hasHexDigitsAfterDot) {
                            reportScannerError("Malformed hex floating literal");
                        }
                    } else if (!hasHexDigitsBeforeDot) {
                            reportScannerError("Malformed hex literal");
                        
                    }

                    if (ch == 'p' || ch == 'P') {
                        isHexFloat = true;
                        buffer.append(ch);
                        nextCh();
                        if (ch == '+' || ch == '-') {
                            buffer.append(ch);
                            nextCh();
                        }
                        boolean expDigits = false;
                        while (isDigit(ch) || ch == '_') {
                            if (isDigit(ch)) expDigits = true;
                            buffer.append(ch);
                            nextCh();
                        }
                        if (!expDigits) {
                            reportScannerError("Malformed hex-float exponent in numeric literal");
                        }
                    }

                    if (isHexFloat) {
                        if (ch == 'f' || ch == 'F') {
                            buffer.append(ch);
                            nextCh();
                            return new TokenInfo(FLOAT_LITERAL, buffer.toString(), line);
                        }
                        if (ch == 'd' || ch == 'D') {
                            buffer.append(ch);
                            nextCh();
                            return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                        }
                        return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                    } else {
                        if (ch == 'l' || ch == 'L') {
                            buffer.append(ch);
                            nextCh();
                            return new TokenInfo(LONG_LITERAL, buffer.toString(), line);
                        }
                        return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                    }
                }else if (ch == 'b' || ch == 'B') {
                    buffer.append(ch);
                    nextCh();
                    boolean binDigits = false;
                    while (ch == '0' || ch == '1' || ch == '_') {
                        if (ch == '0' || ch == '1') binDigits = true;
                        buffer.append(ch);
                        nextCh();
                    }
                    if (!binDigits) {
                        reportScannerError("Malformed binary literal");
                    }
                    if (ch == 'l' || ch == 'L') {
                        buffer.append(ch);
                        nextCh();
                        return new TokenInfo(LONG_LITERAL, buffer.toString(), line);
                    }
                    return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                } else {
                    while (isDigit(ch) || ch == '_') {
                        buffer.append(ch);
                        nextCh();
                    }
                    if (ch == '.' || ch == 'e' || ch == 'E' || ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D') {

                    } else {
                        if (ch == 'l' || ch == 'L') {
                            buffer.append(ch);
                            nextCh();
                            return new TokenInfo(LONG_LITERAL, buffer.toString(), line);
                        }
                        return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                    }
                }
            } else {
                while (isDigit(ch) || ch == '_') {
                    buffer.append(ch);
                    nextCh();
                }
            }

            if (ch == '.') {
                isDouble = true;
                buffer.append(ch);
                nextCh();
                while (isDigit(ch) || ch == '_') {
                    buffer.append(ch);
                    nextCh();
                }
            }

            if (ch == 'e' || ch == 'E') {
                isDouble = true;
                buffer.append(ch);
                nextCh();
                if (ch == '+' || ch == '-') {
                    buffer.append(ch);
                    nextCh();
                }
                boolean expDigits = false;
                while (isDigit(ch) || ch == '_') {
                    if (isDigit(ch)) expDigits = true;
                    buffer.append(ch);
                    nextCh();
                }
                if (!expDigits) {
                    reportScannerError("Malformed exponent in numeric literal");
                }
            }


            if (ch == 'f' || ch == 'F') {
                isFloat = true;
                buffer.append(ch);
                nextCh();
                return new TokenInfo(FLOAT_LITERAL, buffer.toString(), line);
            }
            if (ch == 'd' || ch == 'D') {
                isDouble = true;
                buffer.append(ch);
                nextCh();
                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
            }
            if (ch == 'l' || ch == 'L') {
                buffer.append(ch);
                nextCh();
                return new TokenInfo(LONG_LITERAL, buffer.toString(), line);
            }

            if (isDouble) {
                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
            } else if (isFloat) {
                return new TokenInfo(FLOAT_LITERAL, buffer.toString(), line);
            } else {
                return new TokenInfo(INT_LITERAL, buffer.toString(), line);
            }
		default:
			if (isIdentifierStart(ch)) {
				buffer = new StringBuffer();
				while (isIdentifierPart(ch)) {
					buffer.append(ch);
					nextCh();
				}
				String identifier = buffer.toString();
				if (reserved.containsKey(identifier)) {
					return new TokenInfo(reserved.get(identifier), line);
				} else {
					return new TokenInfo(IDENTIFIER, identifier, line);
				}
			} else {
				reportScannerError("Unidentified input token: '%c'", ch);
				nextCh();
				return getNextToken();
			}
		}
	}

	/**
	 * Returns true if an error has occurred, and false otherwise.
	 *
	 * @return true if an error has occurred, and false otherwise.
	 */
	public boolean errorHasOccurred() {
		return isInError;
	}

	/**
	 * Returns the name of the source file.
	 *
	 * @return the name of the source file.
	 */
	public String fileName() {
		return fileName;
	}

	// Scans and returns an escaped character.
	private String escape() {
		switch (ch) {
		case 'b':
			nextCh();
			return "\\b";
		case 't':
			nextCh();
			return "\\t";
		case 'n':
			nextCh();
			return "\\n";
		case 'f':
			nextCh();
			return "\\f";
		case 'r':
			nextCh();
			return "\\r";
		case '"':
			nextCh();
			return "\\\"";
		case '\'':
			nextCh();
			return "\\'";
		case '\\':
			nextCh();
			return "\\\\";
		default:
			reportScannerError("Badly formed escape: \\%c", ch);
			nextCh();
			return "";
		}
	}

	// Advances ch to the next character from input, and updates the line number.
	private void nextCh() {
		line = input.line();
		try {
			ch = input.nextChar();
		} catch (Exception e) {
			reportScannerError("Unable to read characters from input");
		}
	}

	// Reports a lexical error and records the fact that an error has occurred. This
	// fact can be
	// ascertained from the Scanner by sending it an errorHasOccurred message.
	private void reportScannerError(String message, Object... args) {
		isInError = true;
		System.err.printf("%s:%d: error: ", fileName, line);
		System.err.printf(message, args);
		System.err.println();
	}

	// Returns true if the specified character is a digit (0-9), and false
	// otherwise.
	private boolean isDigit(char c) {
		return (c >= '0' && c <= '9');
	}

	// Returns true if the specified character is a whitespace, and false otherwise.
	private boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\f');
	}

	// Returns true if the specified character can start an identifier name, and
	// false otherwise.
	private boolean isIdentifierStart(char c) {
		return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '$');
	}

	// Returns true if the specified character can be part of an identifier name,
	// and false
	// otherwise.
	private boolean isIdentifierPart(char c) {
		return (isIdentifierStart(c) || isDigit(c));
	}

	// Returns true if the specified character is a hex digit (0-9, a-f, A-F)
	private boolean isHexDigit(char c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}
}

/**
 * A buffered character reader, which abstracts out differences between
 * platforms, mapping all new lines to '\n', and also keeps track of line
 * numbers.
 */
class CharReader {
	// Representation of the end of file as a character.
	public final static char EOFCH = (char) -1;

	// The underlying reader records line numbers.
	private LineNumberReader lineNumberReader;

	// Name of the file that is being read.
	private String fileName;

	/**
	 * Constructs a CharReader from a file name.
	 *
	 * @param fileName the name of the input file.
	 * @throws FileNotFoundException if the file is not found.
	 */
	public CharReader(String fileName) throws FileNotFoundException {
		lineNumberReader = new LineNumberReader(new FileReader(fileName));
		this.fileName = fileName;
	}

	/**
	 * Scans and returns the next character.
	 *
	 * @return the character scanned.
	 * @throws IOException if an I/O error occurs.
	 */
	public char nextChar() throws IOException {
		return (char) lineNumberReader.read();
	}

	/**
	 * Returns the current line number in the source file.
	 *
	 * @return the current line number in the source file.
	 */
	public int line() {
		return lineNumberReader.getLineNumber() + 1; // LineNumberReader counts lines from 0
	}

	/**
	 * Returns the file name.
	 *
	 * @return the file name.
	 */
	public String fileName() {
		return fileName;
	}

	/**
	 * Closes the file.
	 *
	 * @throws IOException if an I/O error occurs.
	 */
	public void close() throws IOException {
		lineNumberReader.close();
	}
}
