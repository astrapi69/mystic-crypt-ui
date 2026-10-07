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
package io.github.astrapi69.mystic.crypt.panel.signin;

import java.io.File;
import java.util.Optional;

import io.github.astrapi69.file.create.model.FileInfo;

/**
 * Which key file a remembered sign-in preselects
 */
public final class RememberedKeyFileSupport
{

	private RememberedKeyFileSupport()
	{
	}

	/**
	 * The key file a remembered path names, if there is one.
	 * <p>
	 * A blank path is no path, and only a file is a key file. Both are rules here, not accidents:
	 * the check used to be {@code new File(path).exists()} alone, and it refused the empty path
	 * only because that did not exist. Since JDK 24 the empty abstract pathname is the current
	 * directory, which exists, so a remembered {@code ""} was taken for a key file and the
	 * application ended before the sign-in dialog appeared (#499)
	 *
	 * @param rememberedPath
	 *            the key file path the last sign-in remembered, may be null
	 * @return the key file to preselect, or empty when the path names none
	 */
	public static Optional<FileInfo> keyFileOf(final String rememberedPath)
	{
		if (rememberedPath == null || rememberedPath.isBlank())
		{
			return Optional.empty();
		}
		File keyFile = new File(rememberedPath);
		if (!keyFile.isFile())
		{
			return Optional.empty();
		}
		return Optional.of(FileInfo.toFileInfo(keyFile));
	}
}
