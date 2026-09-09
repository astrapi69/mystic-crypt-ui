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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * What "the vault is closed" means for the model (#281), and that closing is the moment the
 * decrypted content leaves memory rather than being dropped for the collector to find later (#242).
 */
class VaultCloseSupportTest
{

	private static final char[] MASTER_PASSWORD = "master-password".toCharArray();

	private static final char[] ENTRY_PASSWORD = "entry-password".toCharArray();

	@Test
	@DisplayName("closeVault leaves a model with no vault in it")
	void closeVault_leavesNoVaultOpen_whenOneWasOpen()
	{
		ApplicationModelBean applicationModelBean = openVault();

		VaultCloseSupport.closeVault(applicationModelBean);

		assertNull(applicationModelBean.getMasterPwFileModelBean(),
			"the credentials are what says a vault is open at all");
		assertNull(applicationModelBean.getRootTreeAsMap(), "the tree is gone");
		assertNull(applicationModelBean.getDataOfNodes(), "the entries are gone");
		assertNull(applicationModelBean.getLastId(), "and the id counter with them");
		assertFalse(applicationModelBean.isSignedIn(),
			"closing must not leave the application pretending to be signed in - that is the "
				+ "improvised state move #270 was");
		assertFalse(applicationModelBean.isDirty(),
			"whether the pending changes were saved or discarded was decided before this ran; a "
				+ "model with nothing in it must not claim to have changes worth writing");
		assertFalse(VaultCloseSupport.aVaultIsOpen(applicationModelBean),
			"and the predicate the callers ask agrees");
	}

	@Test
	@DisplayName("closeVault overwrites the passwords instead of dropping them")
	void closeVault_overwritesTheSecrets_whenTheVaultHeldEntries()
	{
		char[] entryPassword = ENTRY_PASSWORD.clone();
		char[] entryRepeat = ENTRY_PASSWORD.clone();
		char[] masterPassword = MASTER_PASSWORD.clone();
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().title("an entry")
			.password(entryPassword).repeat(entryRepeat).build();
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.getMasterPwFileModelBean().setMasterPw(masterPassword);
		applicationModelBean.setDataOfNodes(entriesByNodeId(entry));

		VaultCloseSupport.closeVault(applicationModelBean);

		assertArrayEquals(new char[entryPassword.length], entryPassword,
			"the entry's password array is overwritten. Setting the field to null would leave the "
				+ "password where it was, which is the whole reason it is a char array");
		assertArrayEquals(new char[entryRepeat.length], entryRepeat,
			"and so is the repeated one - a second copy of the same secret");
		assertArrayEquals(new char[masterPassword.length], masterPassword,
			"and the master password, which opens everything else");
	}

	@Test
	@DisplayName("closeVault also reaches the entries that hang in the tree")
	void closeVault_overwritesTheSecrets_whenTheEntriesHangInTheTree()
	{
		char[] entryPassword = ENTRY_PASSWORD.clone();
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().title("a tree entry")
			.password(entryPassword).build();
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.setRootTreeAsMap(treeHolding(entry));

		VaultCloseSupport.closeVault(applicationModelBean);

		assertArrayEquals(new char[entryPassword.length], entryPassword,
			"the tree is the second place an entry lives in this model, and a secret left in the "
				+ "one the loop forgot is a secret left in memory");
	}

	@Test
	@DisplayName("aVaultIsOpen reads the credentials, not the signed-in flag")
	void aVaultIsOpen_isTrue_whenTheVaultIsMerelyLocked()
	{
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.setSignedIn(false);

		assertTrue(VaultCloseSupport.aVaultIsOpen(applicationModelBean),
			"a LOCKED vault is still an open one. Reading the signed-in flag here is what let a "
				+ "second vault be created over the first (#279)");
	}

	@Test
	@DisplayName("closing nothing is not an error")
	void closeVault_doesNothing_whenThereIsNoVaultAndNoModel()
	{
		ApplicationModelBean empty = ApplicationModelBean.builder().build();

		assertDoesNotThrow(() -> VaultCloseSupport.closeVault(null),
			"a close path is called from a window-closing listener, where there may be nothing "
				+ "open at all");
		assertDoesNotThrow(() -> VaultCloseSupport.closeVault(empty));
		assertFalse(VaultCloseSupport.aVaultIsOpen(empty));
		assertFalse(VaultCloseSupport.aVaultIsOpen(null));
	}

	private static ApplicationModelBean openVault()
	{
		return ApplicationModelBean.builder()
			.masterPwFileModelBean(MasterPwFileModelBean.builder().masterPw(MASTER_PASSWORD.clone())
				.withMasterPw(true).minPasswordLength(6).build())
			.rootTreeAsMap(new LinkedHashMap<>()).dataOfNodes(new LinkedHashMap<>()).lastId(7L)
			.signedIn(true).dirty(true).build();
	}

	private static Map<Long, List<MysticCryptEntryModelBean>> entriesByNodeId(
		final MysticCryptEntryModelBean entry)
	{
		Map<Long, List<MysticCryptEntryModelBean>> entriesByNodeId = new LinkedHashMap<>();
		entriesByNodeId.put(1L, List.of(entry));
		return entriesByNodeId;
	}

	private static Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> treeHolding(
		final MysticCryptEntryModelBean entry)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> element = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name("a node").build();
		element.setDefaultContent(List.of(entry));
		TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node = TreeIdNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(1L)
			.value(element).build();
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> treeAsMap = new LinkedHashMap<>();
		treeAsMap.put(1L, node);
		return treeAsMap;
	}
}
