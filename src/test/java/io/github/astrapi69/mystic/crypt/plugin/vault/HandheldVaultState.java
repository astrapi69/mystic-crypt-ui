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

import java.util.ArrayList;
import java.util.List;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;

/**
 * A vault state the tests set by hand: open or not, locked or not, read-only with a reason or not,
 * two groups of entries, and a count of the times the vault was marked changed.
 */
final class HandheldVaultState implements VaultState
{

	final List<MysticCryptEntryModelBean> root = new ArrayList<>();

	final List<MysticCryptEntryModelBean> group = new ArrayList<>();

	boolean open = true;

	boolean unlocked = true;

	String readOnlyReason;

	int changes;

	@Override
	public boolean isOpen()
	{
		return open;
	}

	@Override
	public boolean isUnlocked()
	{
		return unlocked;
	}

	@Override
	public String readOnlyReason()
	{
		return readOnlyReason;
	}

	@Override
	public List<List<MysticCryptEntryModelBean>> entryLists()
	{
		return List.of(root, group);
	}

	@Override
	public List<MysticCryptEntryModelBean> rootEntries()
	{
		return root;
	}

	@Override
	public void markChanged()
	{
		changes++;
	}
}
