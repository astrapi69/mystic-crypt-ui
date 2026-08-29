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
package io.github.astrapi69.mystic.crypt.ui.attachment;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import io.github.astrapi69.file.create.model.FileContentInfo;

/**
 * What to show when someone opens an attachment.
 * <p>
 * An attachment is whatever was put there: a recovery code list, a key file, a photograph. Text is
 * shown as text; anything else is described, because a megabyte of bytes rendered as characters
 * tells the reader nothing and takes a while doing it.
 */
public final class AttachmentContent
{

	/** Above this many bytes even text is described rather than shown whole */
	public static final int LARGEST_TEXT_SHOWN = 1024 * 1024;

	private AttachmentContent()
	{
	}

	/**
	 * What to put on screen for the given attachment
	 *
	 * @param fileContentInfo
	 *            the attachment
	 * @return its content as text, or a description of it when it is not text
	 */
	public static String readable(final FileContentInfo fileContentInfo)
	{
		byte[] content = fileContentInfo.getContent();
		if (content == null || content.length == 0)
		{
			return "This attachment is empty.";
		}
		if (LARGEST_TEXT_SHOWN < content.length)
		{
			return describe(fileContentInfo, content.length);
		}
		String text = asText(content);
		return text != null ? text : describe(fileContentInfo, content.length);
	}

	/**
	 * The content as text, or null when it is not text
	 *
	 * @param content
	 *            the bytes of the attachment
	 * @return the text, or null
	 */
	public static String asText(final byte[] content)
	{
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);
		String decoded;
		try
		{
			decoded = decoder.decode(ByteBuffer.wrap(content)).toString();
		}
		catch (final CharacterCodingException theseBytesAreNotText)
		{
			return null;
		}
		// a file that decodes but is full of control characters is a binary file that happens to
		// hold no invalid sequence - a png header decodes, and reading it as text helps nobody
		for (int index = 0; index < decoded.length(); index++)
		{
			char character = decoded.charAt(index);
			if (Character.isISOControl(character) && character != '\n' && character != '\r'
				&& character != '\t')
			{
				return null;
			}
		}
		return decoded;
	}

	/**
	 * A description of an attachment that is not shown as text
	 *
	 * @param fileContentInfo
	 *            the attachment
	 * @param length
	 *            how many bytes it holds
	 * @return the description
	 */
	private static String describe(final FileContentInfo fileContentInfo, final int length)
	{
		StringBuilder description = new StringBuilder();
		description.append(fileContentInfo.getName()).append(System.lineSeparator()).append(length)
			.append(" bytes, not shown as text.").append(System.lineSeparator())
			.append("Use \"Save to\" to write it to a file.");
		if (fileContentInfo.getChecksum() != null)
		{
			description.append(System.lineSeparator()).append("Checksum: ")
				.append(fileContentInfo.getChecksum());
		}
		return description.toString();
	}

}
