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
package io.github.astrapi69.mystic.crypt.plugin.api;

import java.util.List;
import java.util.UUID;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * A plugin's access to the open vault, bound to the plugin's own marker
 * (docs/decisions/plugin-vault-access-2026-10-31.md). The plugin sees and changes only the entries
 * that carry its marker; every other entry is invisible to it. Adding, changing and removing are
 * refused with the reason when no vault is open, when it is locked, or when it is read-only.
 */
public interface VaultAccess
{

	/**
	 * Whether a vault is open and unlocked, so that its entries can be read
	 *
	 * @return true when the plugin can read its entries now
	 */
	boolean isAvailable();

	/**
	 * The entries that carry this plugin's marker, as copies: changing a copy changes nothing in
	 * the vault until it is handed back through {@link #change}
	 *
	 * @return the copies, empty when no vault is available
	 */
	List<MysticCryptEntryModelBean> entries();

	/**
	 * Adds an entry to the root group of the vault; the host gives it an identifier when it has
	 * none and keeps a copy of it
	 *
	 * @param entry
	 *            the entry, carrying this plugin's marker
	 * @throws VaultAccessRefusedException
	 *             when the entry has no marker of this plugin, or the vault may not be written
	 */
	void add(MysticCryptEntryModelBean entry);

	/**
	 * Replaces the vault's entry with the same identifier by the given one
	 *
	 * @param entry
	 *            the changed entry, still carrying this plugin's marker
	 * @throws VaultAccessRefusedException
	 *             when no entry of this plugin has that identifier, the changed entry lost the
	 *             marker, or the vault may not be written
	 */
	void change(MysticCryptEntryModelBean entry);

	/**
	 * Removes the entry with the given identifier
	 *
	 * @param id
	 *            the identifier of one of this plugin's entries
	 * @throws VaultAccessRefusedException
	 *             when no entry of this plugin has that identifier, or the vault may not be written
	 */
	void remove(UUID id);
}
