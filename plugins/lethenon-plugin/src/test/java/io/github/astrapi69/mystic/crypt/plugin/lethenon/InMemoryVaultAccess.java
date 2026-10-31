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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccess;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccessRefusedException;

/**
 * A vault access in memory that keeps the host's rules (#677): only entries with the marker are
 * read, as copies; a closed vault refuses writes and reads as empty.
 */
final class InMemoryVaultAccess implements VaultAccess
{

	final List<MysticCryptEntryModelBean> stored = new ArrayList<>();

	boolean available = true;

	@Override
	public boolean isAvailable()
	{
		return available;
	}

	@Override
	public List<MysticCryptEntryModelBean> entries()
	{
		if (!available)
		{
			return List.of();
		}
		return stored.stream().filter(InMemoryVaultAccess::isMarked)
			.map(MysticCryptEntryModelBean::duplicate).toList();
	}

	@Override
	public void add(final MysticCryptEntryModelBean entry)
	{
		requireAvailable();
		MysticCryptEntryModelBean copy = entry.duplicate();
		if (copy.getId() == null)
		{
			copy.setId(UUID.randomUUID());
		}
		stored.add(copy);
	}

	@Override
	public void change(final MysticCryptEntryModelBean entry)
	{
		requireAvailable();
		for (int index = 0; index < stored.size(); index++)
		{
			if (stored.get(index).getId().equals(entry.getId()))
			{
				stored.set(index, entry.duplicate());
				return;
			}
		}
		throw new VaultAccessRefusedException("no entry " + entry.getId());
	}

	@Override
	public void remove(final UUID id)
	{
		requireAvailable();
		stored.removeIf(entry -> entry.getId().equals(id));
	}

	private void requireAvailable()
	{
		if (!available)
		{
			throw new VaultAccessRefusedException("the vault is locked");
		}
	}

	private static boolean isMarked(final MysticCryptEntryModelBean entry)
	{
		return VaultEntryProperties.valuesOf(entry).containsKey(WalletVault.WALLET);
	}
}
