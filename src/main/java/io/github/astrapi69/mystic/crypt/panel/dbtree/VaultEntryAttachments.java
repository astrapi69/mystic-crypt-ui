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

import java.util.List;
import java.util.Optional;

import io.github.astrapi69.file.create.model.FileContentInfo;

/**
 * The attachments of a vault entry by name, as plain bytes, for a feature that does not know the
 * entry model. Bytes are copied both ways, so the feature and the entry never share an array. Used
 * by the lethenon wallet tools, which keep the sealed wallet as the attachment
 * {@code wallet.lethwf} (docs/decisions/lethenon-wallet-in-vault-2026-10-30.md).
 */
public final class VaultEntryAttachments
{

	private VaultEntryAttachments()
	{
	}

	/**
	 * The content of the attachment with the given name
	 *
	 * @param entry
	 *            the entry
	 * @param name
	 *            the attachment's name
	 * @return a copy of its content, or empty when the entry has no attachment of that name
	 */
	public static Optional<byte[]> contentOf(final MysticCryptEntryModelBean entry,
		final String name)
	{
		return attachmentNamed(entry.getResources(), name)
			.map(attachment -> attachment.getContent().clone());
	}

	/**
	 * Sets the attachment with the given name: replaces the content of the one already there, or
	 * adds it after the entry's other attachments
	 *
	 * @param entry
	 *            the entry, changed in place
	 * @param name
	 *            the attachment's name
	 * @param content
	 *            the content; the entry keeps a copy
	 */
	public static void put(final MysticCryptEntryModelBean entry, final String name,
		final byte[] content)
	{
		Optional<FileContentInfo> present = attachmentNamed(entry.getResources(), name);
		if (present.isPresent())
		{
			present.get().setContent(content.clone());
			return;
		}
		entry.getResources()
			.add(FileContentInfo.builder().name(name).content(content.clone()).build());
	}

	private static Optional<FileContentInfo> attachmentNamed(
		final List<FileContentInfo> attachments, final String name)
	{
		return attachments.stream().filter(attachment -> name.equals(attachment.getName()))
			.findFirst();
	}
}
