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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.file.create.model.FileContentInfo;

/**
 * What a double click on an attachment puts on screen
 */
class AttachmentContentTest
{

	private static FileContentInfo attachment(final String name, final byte[] content)
	{
		return FileContentInfo.builder().name(name).content(content).build();
	}

	@Test
	@DisplayName("text comes back as it was written")
	void textComesBackAsItWasWritten()
	{
		String written = "recovery codes" + System.lineSeparator() + "1234-5678";

		assertEquals(written, AttachmentContent
			.readable(attachment("codes.txt", written.getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	@DisplayName("bytes that are not text are described instead of rendered")
	void bytesThatAreNotTextAreDescribed()
	{
		byte[] png = { (byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

		String shown = AttachmentContent.readable(attachment("photo.png", png));

		assertTrue(shown.contains("photo.png"), "the description has to name the attachment");
		assertTrue(shown.contains("8 bytes"), "the description has to say how much there is");
		assertTrue(shown.contains("Save to"), "the description has to say how to get at it");
	}

	@Test
	@DisplayName("an empty attachment says that it is empty")
	void anEmptyAttachmentSaysThatItIsEmpty()
	{
		assertEquals("This attachment is empty.",
			AttachmentContent.readable(attachment("nothing.bin", new byte[0])));
	}

	@Test
	@DisplayName("bytes that decode but are full of control characters are not text")
	void bytesThatDecodeButAreFullOfControlCharactersAreNotText()
	{
		assertNull(AttachmentContent.asText(new byte[] { 0x00, 0x01, 0x02 }));
	}

	@Test
	@DisplayName("tabs and line breaks are part of text")
	void tabsAndLineBreaksArePartOfText()
	{
		String written = "one\ttwo\nthree\r\nfour";

		assertEquals(written, AttachmentContent.asText(written.getBytes(StandardCharsets.UTF_8)));
	}

}
