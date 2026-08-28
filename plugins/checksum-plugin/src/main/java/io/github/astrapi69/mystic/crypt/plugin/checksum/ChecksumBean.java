/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
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
package io.github.astrapi69.mystic.crypt.plugin.checksum;

import java.io.File;
import java.io.Serializable;

import io.github.astrapi69.crypt.api.algorithm.ChecksumAlgorithm;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * Everything the {@link ChecksumPanel} holds: the file whose checksum is computed and the algorithm
 * it is computed with, the checksum file the owner's value was read from, both checksums and what
 * the last comparison of the two found.
 * <p>
 * The panel binds its components to this object, so what the user chose, typed or had computed is
 * readable here at any moment - no button handler has to ask a widget for its content. The text
 * properties start empty rather than {@code null}: a text component without text holds empty text,
 * and the binding would otherwise have to guess which of the two an untouched field means.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChecksumBean implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The file the checksum is computed over, null as long as none was chosen */
	File selectedFile;

	/** The name of that file, as the field under the buttons shows it */
	@Builder.Default
	String selectedFilename = "";

	/** The algorithm the checksum is computed with */
	ChecksumAlgorithm selectedAlgorithm;

	/** The file the owner's checksum was read from, null as long as none was chosen */
	File selectedChecksumFile;

	/** The name of that file, as the field next to it shows it */
	@Builder.Default
	String selectedChecksumFilename = "";

	/** The checksum this tool computed for the selected file */
	@Builder.Default
	String generatedChecksum = "";

	/** The checksum the owner published, typed in or read from a checksum file */
	@Builder.Default
	String ownersChecksum = "";

	/** What the last comparison of the two checksums found */
	@Builder.Default
	String checksumMatchResult = "";
}
