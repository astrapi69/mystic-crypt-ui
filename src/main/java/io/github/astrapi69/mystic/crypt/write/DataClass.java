/**
 * The MIT License
 *
 * Copyright (C) 2026 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.write;

/**
 * What a file about to be overwritten holds, which is what decides how hard the question is (#300).
 * <p>
 * The application answered "this file already exists" in three different ways across 23 call sites,
 * from five separately written guards to fourteen silent overwrites, and the guards' messages had
 * already drifted apart. The classification is what a caller knows and a helper cannot guess, so
 * the caller names it and the helper decides and phrases.
 */
public enum DataClass
{

	/**
	 * Something that can be produced again: a checksum file, an exported PEM, a converted key, a
	 * generated rule file. Recomputing it after the source changed is the ordinary case, so the
	 * question is a confirmation rather than a warning.
	 */
	DERIVED,

	/**
	 * A vault or key material: the original may be the only copy, and replacing it has no undo. The
	 * question names the file and says what is lost, and a mis-click on the wrong button costs
	 * somebody their database.
	 */
	IRREPLACEABLE
}
