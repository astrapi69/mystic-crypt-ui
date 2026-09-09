/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.vault;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Closing a vault, as far as the model is concerned: what is left afterwards is a model with no
 * vault in it (#281).
 * <p>
 * This application could open a vault and it could end, but it could never CLOSE one - the state
 * "no vault open" was unreachable while it ran. Three issues each needed exactly that state (#279,
 * #266, #242) and each of them would otherwise have invented its own version of it, which is how
 * the improvised state move in #270 came about.
 * <p>
 * No Swing type appears here on purpose: what belongs to the screen - taking the view off the
 * desktop, putting the menu back into the public state - is the frame's job, and this half is
 * testable without a display.
 * <p>
 * Closing is also the moment the decrypted vault leaves memory (#242): the entries' passwords are
 * character arrays, they are overwritten here rather than dropped for the collector to find later,
 * and the master password goes the same way. That is the part of #242 a close path can answer; what
 * may stay in memory while a vault is LOCKED rather than closed is the other half and is decided
 * there.
 */
public final class VaultCloseSupport
{

	private VaultCloseSupport()
	{
	}

	/**
	 * Empties the given model so that no vault is open any more: no credentials, no tree, no
	 * entries, not signed in, and nothing left marked as unsaved.
	 * <p>
	 * The dirty flag is cleared LAST and deliberately: whether the pending changes were saved or
	 * discarded is the caller's decision and has already been made by the time this runs. Leaving
	 * it set would make the next thing that asks believe an emptied model has changes worth
	 * writing.
	 *
	 * @param applicationModelBean
	 *            the application model; null is accepted and does nothing
	 */
	public static void closeVault(final ApplicationModelBean applicationModelBean)
	{
		if (applicationModelBean == null)
		{
			return;
		}
		wipeEntries(applicationModelBean.getDataOfNodes());
		wipeTree(applicationModelBean.getRootTreeAsMap());
		forgetTheMasterPassword(applicationModelBean.getMasterPwFileModelBean());
		applicationModelBean.setMasterPwFileModelBean(null);
		applicationModelBean.setRootTreeAsMap(null);
		applicationModelBean.setDataOfNodes(null);
		applicationModelBean.setLastId(null);
		applicationModelBean.setSignedIn(false);
		applicationModelBean.setDirty(false);
	}

	/**
	 * Whether a vault is open at all, locked or not.
	 * <p>
	 * The credentials answer this rather than the signed-in flag: a LOCKED vault is still an open
	 * one, and the flag says only whether it is unlocked. That distinction is what
	 * {@code mayCreateAVault} had to learn the hard way (#279)
	 *
	 * @param applicationModelBean
	 *            the application model
	 * @return true if a vault is open
	 */
	public static boolean aVaultIsOpen(final ApplicationModelBean applicationModelBean)
	{
		return applicationModelBean != null
			&& applicationModelBean.getMasterPwFileModelBean() != null;
	}

	private static void wipeEntries(
		final Map<Long, List<MysticCryptEntryModelBean>> entriesByNodeId)
	{
		if (entriesByNodeId == null)
		{
			return;
		}
		entriesByNodeId.values().forEach(VaultCloseSupport::wipeAll);
	}

	private static void wipeTree(
		final Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> treeAsMap)
	{
		if (treeAsMap == null)
		{
			return;
		}
		treeAsMap.values().stream().filter(node -> node != null && node.getValue() != null)
			.map(node -> node.getValue().getDefaultContent()).forEach(VaultCloseSupport::wipeAll);
	}

	private static void wipeAll(final Collection<MysticCryptEntryModelBean> entries)
	{
		if (entries == null)
		{
			return;
		}
		entries.stream().filter(entry -> entry != null).forEach(VaultCloseSupport::wipe);
	}

	/**
	 * Overwrites the character arrays an entry carries - all six of them (#294). Setting them to
	 * null would leave the content where it was until something else happens to reuse that memory,
	 * which is the whole point of holding it in a character array rather than in a String
	 *
	 * @param entry
	 *            the entry whose content is overwritten
	 */
	private static void wipe(final MysticCryptEntryModelBean entry)
	{
		SecretBuffers.wipe(entry.getPassword());
		entry.setPassword(null);
		SecretBuffers.wipe(entry.getRepeat());
		entry.setRepeat(null);
		SecretBuffers.wipe(entry.getTitle());
		entry.setTitle(null);
		SecretBuffers.wipe(entry.getUserName());
		entry.setUserName(null);
		SecretBuffers.wipe(entry.getUrl());
		entry.setUrl(null);
		SecretBuffers.wipe(entry.getNotes());
		entry.setNotes(null);
	}

	private static void forgetTheMasterPassword(final MasterPwFileModelBean credentials)
	{
		if (credentials == null)
		{
			return;
		}
		SecretBuffers.wipe(credentials.getMasterPw());
		credentials.setMasterPw(null);
	}
}
