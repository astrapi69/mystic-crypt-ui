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

import org.pf4j.ExtensionPoint;

/**
 * Declared by a plugin that keeps entries in the vault
 * (docs/decisions/plugin-vault-access-2026-10-31.md). The host connects it with a
 * {@link VaultAccess} bound to its marker, but only when the marker lies in the plugin's own
 * namespace: the plugin's id without the {@code -plugin} suffix, then a dot. A plugin with id
 * {@code lethenon-plugin} may mark {@code lethenon.wallet}, never another plugin's entries.
 */
public interface VaultAccessContribution extends ExtensionPoint
{

	/**
	 * The name of the custom property that marks this plugin's entries
	 *
	 * @return the marker, in the plugin's own namespace
	 */
	String getEntryMarker();

	/**
	 * Hands the plugin its access; called once, by the host
	 *
	 * @param access
	 *            the access bound to this plugin's marker
	 */
	void connect(VaultAccess access);

	/**
	 * Called before the host locks or closes the vault, while the entries are still there, so the
	 * plugin can drop and overwrite what it holds. The default holds nothing and does nothing
	 */
	default void vaultClosing()
	{
	}
}
