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

import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.file.create.model.FileContentInfo;
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
 * Closing is also the moment the decrypted vault leaves memory (#242). Everything that is held in a
 * buffer is OVERWRITTEN here rather than dropped for the collector to find later: the entry's six
 * character arrays, its attachments' bytes, the master password, and the private key's encoded
 * bytes. Dropping is not erasing - a reference the collector may or may not get around to says
 * nothing about the memory behind it, which is the whole reason these are buffers.
 * <p>
 * <b>What this cannot erase, and why.</b> Three surfaces are dropped and not overwritten, each for
 * a reason that is not a decision taken here. They are listed rather than left silent, because a
 * gap nobody wrote down reads as one nobody noticed:
 * <ul>
 * <li><b>an entry's custom properties</b> are {@code KeyValuePair<String, String>}, and a String
 * cannot be overwritten. This is the same defect the six entry fields had before #294, and the same
 * remedy applies - it is tracked separately rather than bundled into a close path, because it
 * changes a data model every plugin and the KeePass import touch.
 * <li><b>the text inside a Swing password field</b> lives in a {@code Document} the JDK gives no
 * caller a way to overwrite. {@code getPassword()} hands out a copy; the original stays. Not
 * solvable at this layer.
 * <li><b>the whole-vault plaintext XML</b> on the key-file and legacy read paths, where the library
 * decryptors return a String ({@code PrivateKeyGenericDecryptor<String>},
 * {@code PasswordStringDecryptor}). The password path already avoids this - it works in
 * {@code char[]} end to end and wipes it. Closing that gap means changing the libraries, not this
 * method.
 * </ul>
 * What may stay in memory while a vault is LOCKED rather than closed is the other half of #242 and
 * is decided there.
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
	 * Overwrites everything an entry holds in a buffer: the six character arrays (#294) and the
	 * bytes of every attachment (#242). Setting them to null would leave the content where it was
	 * until something else happens to reuse that memory, which is the whole point of holding it in
	 * an array rather than in a String.
	 * <p>
	 * The custom properties are cleared rather than overwritten - they are Strings, see the class
	 * Javadoc.
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
		wipeAttachments(entry.getResources());
		entry.setResources(null);
		clearProperties(entry);
	}

	/**
	 * Overwrites the bytes of every attachment the entry carries.
	 * <p>
	 * An attachment is a file somebody put INTO their password database, so it is the kind of thing
	 * that is there precisely because it must not lie around elsewhere - a recovery code sheet, a
	 * key, a scan. It was measured intact in memory after a close while the passwords beside it
	 * were already gone (#242).
	 *
	 * @param attachments
	 *            the entry's attachments; null is accepted
	 */
	private static void wipeAttachments(final List<FileContentInfo> attachments)
	{
		if (attachments == null)
		{
			return;
		}
		for (FileContentInfo attachment : attachments)
		{
			if (attachment == null)
			{
				continue;
			}
			SecretBuffers.wipe(attachment.getContent());
			attachment.setContent(null);
		}
	}

	/**
	 * Drops the entry's custom properties. They cannot be overwritten - see the class Javadoc - so
	 * this is a drop and is named as one rather than counted as a wipe
	 *
	 * @param entry
	 *            the entry whose properties are dropped
	 */
	private static void clearProperties(final MysticCryptEntryModelBean entry)
	{
		entry.setProperties(null);
	}

	/**
	 * Overwrites the master password and the encoded bytes of the private key.
	 * <p>
	 * The key was the other half: a vault opened with a key file keeps the decoded private key in
	 * the credentials, and it decrypts the file. Forgetting the password while leaving the key is
	 * forgetting one of two ways in (#242).
	 *
	 * @param credentials
	 *            the credentials to empty; null is accepted
	 */
	private static void forgetTheMasterPassword(final MasterPwFileModelBean credentials)
	{
		if (credentials == null)
		{
			return;
		}
		SecretBuffers.wipe(credentials.getMasterPw());
		credentials.setMasterPw(null);
		SecretBuffers.wipe(credentials.getRepeatPw());
		credentials.setRepeatPw(null);
		KeyModel privateKeyInfo = credentials.getPrivateKeyInfo();
		if (privateKeyInfo != null)
		{
			// the buffer is overwritten but the field cannot be cleared: KeyModel declares its
			// fields final and @NonNull, so the array is the only thing reachable from here. That
			// is enough for what this is about - the bytes are gone, and the empty holder goes
			// with the credentials on the next line
			SecretBuffers.wipe(privateKeyInfo.getEncoded());
		}
		credentials.setPrivateKeyInfo(null);
	}
}
