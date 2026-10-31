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
package io.github.astrapi69.mystic.crypt.plugin.vault;

import java.util.List;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * What the plugin access needs to know about the open vault and may do to it, and nothing more: the
 * seam between {@link MarkedEntryAccess} and the host's model, so the access rules are testable
 * without a window
 */
public interface VaultState
{

	/**
	 * @return whether a vault is open
	 */
	boolean isOpen();

	/**
	 * @return whether the open vault is unlocked
	 */
	boolean isUnlocked();

	/**
	 * @return why the open vault may not be written, or null when it may
	 */
	String readOnlyReason();

	/**
	 * @return the entry lists of every group of the vault, the vault's own lists
	 */
	List<List<MysticCryptEntryModelBean>> entryLists();

	/**
	 * @return the entry list of the root group, the vault's own list
	 */
	List<MysticCryptEntryModelBean> rootEntries();

	/** Marks the vault changed, as the host's own edits do */
	void markChanged();
}
