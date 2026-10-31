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
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.file.create.model.FileContentInfo;

/**
 * An entry's attachments by name, for a feature that does not know the entry model: the lethenon
 * wallet tools keep the sealed wallet as the attachment {@code wallet.lethwf}
 * (docs/decisions/lethenon-wallet-in-vault-2026-10-30.md).
 */
class VaultEntryAttachmentsTest
{

	private static final String NAME = "wallet.lethwf";

	private static final byte[] SEALED = "LETHWF sealed bytes".getBytes(StandardCharsets.US_ASCII);

	@Test
	@DisplayName("the content of a named attachment is read, and an absent one reads as empty")
	void readsTheNamedAttachmentAndNothingForAnAbsentOne()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		entry.getResources().add(FileContentInfo.builder().name("notes.txt")
			.content("other".getBytes(StandardCharsets.US_ASCII)).build());
		entry.getResources().add(FileContentInfo.builder().name(NAME).content(SEALED).build());

		assertArrayEquals(SEALED, VaultEntryAttachments.contentOf(entry, NAME).orElseThrow());
		assertEquals(Optional.empty(), VaultEntryAttachments.contentOf(entry, "absent.bin"));
	}

	@Test
	@DisplayName("putting an attachment adds it once, replaces one of the same name and keeps the others")
	void putReplacesTheSameNameAndKeepsEveryOtherAttachment()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		entry.getResources().add(FileContentInfo.builder().name("notes.txt")
			.content("other".getBytes(StandardCharsets.US_ASCII)).build());

		VaultEntryAttachments.put(entry, NAME, "first".getBytes(StandardCharsets.US_ASCII));
		VaultEntryAttachments.put(entry, NAME, SEALED);

		List<String> names = entry.getResources().stream().map(FileContentInfo::getName).toList();
		assertEquals(List.of("notes.txt", NAME), names, "one attachment per name, others kept");
		assertArrayEquals(SEALED, VaultEntryAttachments.contentOf(entry, NAME).orElseThrow());
	}

	@Test
	@DisplayName("the bytes read and the bytes put are copies, so neither side changes the other")
	void readAndPutCopyTheBytes()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		byte[] given = SEALED.clone();

		VaultEntryAttachments.put(entry, NAME, given);
		given[0] = 0;
		byte[] read = VaultEntryAttachments.contentOf(entry, NAME).orElseThrow();
		read[1] = 0;

		assertArrayEquals(SEALED, VaultEntryAttachments.contentOf(entry, NAME).orElseThrow(),
			"the entry must keep its own bytes");
		assertTrue(read != entry.getResources().get(0).getContent());
	}
}
