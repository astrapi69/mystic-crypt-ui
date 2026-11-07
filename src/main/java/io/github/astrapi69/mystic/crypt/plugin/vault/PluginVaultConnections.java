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
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccessContribution;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultExport;
import lombok.extern.java.Log;

/**
 * Connects the plugins that keep entries in the vault, each with an access bound to a marker in its
 * own namespace, and tells them before the vault locks or closes
 * (docs/decisions/plugin-vault-access-2026-10-31.md)
 */
@Log
public final class PluginVaultConnections
{

	private static final String PLUGIN_SUFFIX = "-plugin";

	private final VaultState vault;

	private final List<VaultAccessContribution> connected = new ArrayList<>();

	/**
	 * The connections over one vault state
	 *
	 * @param vault
	 *            the vault's state
	 */
	public PluginVaultConnections(final VaultState vault)
	{
		this.vault = vault;
	}

	/**
	 * Connects a plugin, when its marker lies in its own namespace
	 *
	 * @param pluginId
	 *            the plugin's id, as its descriptor gives it
	 * @param contribution
	 *            the plugin's declaration
	 * @return true when the plugin is connected, false when its marker is refused
	 */
	public boolean connect(final String pluginId, final VaultAccessContribution contribution)
	{
		String marker = contribution.getEntryMarker();
		if (!isInNamespace(pluginId, marker))
		{
			log.warning("the plugin " + pluginId + " asked for vault entries marked '" + marker
				+ "', outside its namespace '" + namespaceOf(pluginId)
				+ ".': it gets no vault access");
			return false;
		}
		if (connected.stream().noneMatch(known -> known == contribution))
		{
			connected.add(contribution);
			contribution.connect(new MarkedEntryAccess(marker, vault));
		}
		return true;
	}

	/**
	 * Tells every connected plugin that the vault is about to be locked or closed. A plugin that
	 * fails here is logged and does not stop the lock or the others
	 */
	public void vaultClosing()
	{
		for (VaultAccessContribution contribution : connected)
		{
			try
			{
				contribution.vaultClosing();
			}
			catch (RuntimeException failed)
			{
				// the lock must happen whatever a plugin does; its failure is recorded, not obeyed
				log.log(Level.SEVERE, "a plugin failed while the vault was closing", failed);
			}
		}
	}

	/**
	 * The lines the connected plugins give before the whole vault is exported to a KeePass file
	 * (#810)
	 *
	 * @return the lines, in the order the plugins were connected; empty when none gives one
	 */
	public List<String> databaseExportNotices()
	{
		List<String> notices = new ArrayList<>();
		for (VaultAccessContribution contribution : connected)
		{
			noticeOf(contribution, VaultExport.DATABASE).ifPresent(notices::add);
		}
		return notices;
	}

	/**
	 * The line of the plugin that marks an entry, before one of the entry's attachments is saved to
	 * a file (#810)
	 *
	 * @param entry
	 *            the entry whose attachment is about to leave the vault
	 * @return the line, or empty when no connected plugin marks the entry or the plugin gives none
	 */
	public Optional<String> attachmentExportNotice(final MysticCryptEntryModelBean entry)
	{
		Set<String> names = VaultEntryProperties.valuesOf(entry).keySet();
		for (VaultAccessContribution contribution : connected)
		{
			if (names.contains(contribution.getEntryMarker()))
			{
				return noticeOf(contribution, VaultExport.ATTACHMENT);
			}
		}
		return Optional.empty();
	}

	/**
	 * A plugin's line, or none when it gives a blank one or fails: the line informs the user, and a
	 * plugin that cannot give it does not stop the export
	 */
	private static Optional<String> noticeOf(final VaultAccessContribution contribution,
		final VaultExport export)
	{
		try
		{
			Optional<String> notice = contribution.exportNotice(export);
			return notice == null ? Optional.empty() : notice.filter(line -> !line.isBlank());
		}
		catch (RuntimeException failed)
		{
			log.log(Level.SEVERE, "a plugin failed to give its line for the export of " + export,
				failed);
			return Optional.empty();
		}
	}

	private static boolean isInNamespace(final String pluginId, final String marker)
	{
		String prefix = namespaceOf(pluginId) + ".";
		return marker != null && marker.startsWith(prefix) && prefix.length() < marker.length();
	}

	private static String namespaceOf(final String pluginId)
	{
		return pluginId.endsWith(PLUGIN_SUFFIX)
			? pluginId.substring(0, pluginId.length() - PLUGIN_SUFFIX.length())
			: pluginId;
	}
}
