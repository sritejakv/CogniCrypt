/********************************************************************************
 * Copyright (c) 2015-2018 TU Darmstadt
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package de.cognicrypt.codegenerator.utilities;

import java.util.Hashtable;

/**
 * A simple fuzzy scanner for Java
 */
public class JavaScanner {

	public static final int EOF = -1;

	public static final int EOL = 10;

	public static final int WORD = 0;

	public static final int WHITE = 1;

	public static final int KEY = 2;

	public static final int COMMENT = 3;

	public static final int STRING = 5;

	public static final int OTHER = 6;

	public static final int NUMBER = 7;

	public static final int MAXIMUM_TOKEN = 8;

	protected Hashtable<String, Integer> fgKeys = null;

	protected StringBuffer fBuffer = new StringBuffer();

	protected String fDoc;

	protected int fPos;

	protected int fEnd;

	protected int fStartToken;

	protected boolean fEofSeen = false;

	private final String[] fgKeywords = { "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "continue", "default", "do", "double", "else", "extends", "false", "final", "finally", "float", "for", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while" };

	public JavaScanner() {
		initialize();
	}

	/**
	 * Returns the ending location of the current token in the document.
	 */
	public final int getLength() {
		return this.fPos - this.fStartToken;
	}

	/**
	 * Initialize the lookup table.
	 */
	void initialize() {
		this.fgKeys = new Hashtable<String, Integer>();
		final Integer k = new Integer(KEY);
		for (int i = 0; i < this.fgKeywords.length; i++) {
			this.fgKeys.put(this.fgKeywords[i], k);
		}
	}

	/**
	 * Returns the starting location of the current token in the document.
	 */
	public final int getStartOffset() {
		return this.fStartToken;
	}

	/**
	 * Returns the next lexical token in the document.
	 */

	public int nextToken() {
		int c;
		this.fStartToken = this.fPos;
		while (true) {
			switch (c = read()) {
				case EOF:
					return EOF;
				case '/': // comment
					c = read();
					if (c == '/') {
						while (true) {
							c = read();
							if ((c == EOF) || (c == EOL)) {
								unread(c);
								return COMMENT;
							}
						}
					} else {
						unread(c);
					}
					return OTHER;
				case '\'': // char const
					character: for (;;) {
						c = read();
						switch (c) {
							case '\'':
								return STRING;
							case EOF:
								unread(c);
								return STRING;
							case '\\':
								c = read();
								break;
						}
					}

				case '"': // string
					string: for (;;) {
						c = read();
						switch (c) {
							case '"':
								return STRING;
							case EOF:
								unread(c);
								return STRING;
							case '\\':
								c = read();
								break;
						}
					}

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
					do {
						c = read();
					} while (Character.isDigit((char) c));
					unread(c);
					return NUMBER;
				default:
					if (Character.isWhitespace((char) c)) {
						do {
							c = read();
						} while (Character.isWhitespace((char) c));
						unread(c);
						return WHITE;
					}
					if (Character.isJavaIdentifierStart((char) c)) {
						this.fBuffer.setLength(0);
						do {
							this.fBuffer.append((char) c);
							c = read();
						} while (Character.isJavaIdentifierPart((char) c));
						unread(c);
						final Integer i = this.fgKeys.get(this.fBuffer.toString());
						if (i != null) {
							return i.intValue();
						}
						return WORD;
					}
					return OTHER;
			}
		}
	}

	/**
	 * Returns next character.
	 */
	protected int read() {
		if (this.fPos <= this.fEnd) {
			return this.fDoc.charAt(this.fPos++);
		}
		return EOF;
	}

	public void setRange(final String text) {
		this.fDoc = text;
		this.fPos = 0;
		this.fEnd = this.fDoc.length() - 1;
	}

	protected void unread(final int c) {
		if (c != EOF) {
			this.fPos--;
		}
	}
}
