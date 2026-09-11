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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.crypt.api.key.KeyType;
import io.github.astrapi69.crypt.data.model.KeyModel;
import io.github.astrapi69.file.create.model.FileContentInfo;
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
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("an entry".toCharArray()).password(entryPassword).repeat(entryRepeat).build();
		ApplicationModelBean applicationModelBean = openVault();
		MasterPwFileModelBean credentials = applicationModelBean.getMasterPwFileModelBean();
		credentials.setMasterPw(masterPassword);
		applicationModelBean.setDataOfNodes(entriesByNodeId(entry));

		VaultCloseSupport.closeVault(applicationModelBean);

		assertNull(credentials.getMasterPw(),
			"the credentials object outlives this call - the caller may still hold it - so its "
				+ "field is cleared and not only the array behind it");

		assertArrayEquals(new char[entryPassword.length], entryPassword,
			"the entry's password array is overwritten. Setting the field to null would leave the "
				+ "password where it was, which is the whole reason it is a char array");
		assertArrayEquals(new char[entryRepeat.length], entryRepeat,
			"and so is the repeated one - a second copy of the same secret");
		assertArrayEquals(new char[masterPassword.length], masterPassword,
			"and the master password, which opens everything else");
		assertNull(entry.getPassword(),
			"the field is cleared as well as the array. An overwritten array still referenced by a "
				+ "live entry is a secret this application is still holding on to");
		assertNull(entry.getRepeat());
	}

	@Test
	@DisplayName("closeVault overwrites an entry's text, not only its password")
	void closeVault_overwritesTheText_thatUsedToBeUnwipeable()
	{
		char[] title = "the bank".toCharArray();
		char[] userName = "the account holder".toCharArray();
		char[] url = "https://bank.example.org".toCharArray();
		char[] notes = "the recovery codes".toCharArray();
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().title(title)
			.userName(userName).url(url).notes(notes).password(ENTRY_PASSWORD.clone()).build();
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.setDataOfNodes(entriesByNodeId(entry));

		VaultCloseSupport.closeVault(applicationModelBean);

		assertArrayEquals(new char[title.length], title,
			"an entry's title says which service it belongs to, which is worth as much to whoever "
				+ "reads the memory as the password next to it (#294)");
		assertArrayEquals(new char[userName.length], userName);
		assertArrayEquals(new char[url.length], url);
		assertArrayEquals(new char[notes.length], notes,
			"and notes are where people put the things that fit in no other field");
		assertNull(entry.getTitle(), "the fields are cleared as well as the arrays");
		assertNull(entry.getUserName());
		assertNull(entry.getUrl());
		assertNull(entry.getNotes());
	}

	@Test
	@DisplayName("an entry with no text at all is closed without complaint")
	void closeVault_wipesAnEmptyEntry_withoutFailing()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder().build();
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.setDataOfNodes(entriesByNodeId(entry));

		VaultCloseSupport.closeVault(applicationModelBean);

		assertNull(entry.getTitle(),
			"a half-filled entry is an ordinary state - the close path must not be the thing that "
				+ "throws while a vault is being put away");
	}

	@Test
	@DisplayName("closeVault also reaches the entries that hang in the tree")
	void closeVault_overwritesTheSecrets_whenTheEntriesHangInTheTree()
	{
		char[] entryPassword = ENTRY_PASSWORD.clone();
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("a tree entry".toCharArray()).password(entryPassword).build();
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.setRootTreeAsMap(treeHolding(entry));

		VaultCloseSupport.closeVault(applicationModelBean);

		assertArrayEquals(new char[entryPassword.length], entryPassword,
			"the tree is the second place an entry lives in this model, and a secret left in the "
				+ "one the loop forgot is a secret left in memory");
		assertNull(entry.getPassword(), "and the field with it");
	}

	@Test
	@DisplayName("a null among the entries does not stop the wipe")
	void closeVault_wipesTheRest_whenTheModelHoldsANull()
	{
		char[] entryPassword = ENTRY_PASSWORD.clone();
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("a real one".toCharArray()).password(entryPassword).build();
		Map<Long, List<MysticCryptEntryModelBean>> entriesByNodeId = new LinkedHashMap<>();
		entriesByNodeId.put(1L, Arrays.asList(null, entry));
		entriesByNodeId.put(2L, null);
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> treeWithHoles = new LinkedHashMap<>();
		treeWithHoles.put(1L, null);
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.setDataOfNodes(entriesByNodeId);
		applicationModelBean.setRootTreeAsMap(treeWithHoles);

		VaultCloseSupport.closeVault(applicationModelBean);

		assertArrayEquals(new char[entryPassword.length], entryPassword,
			"a null in the list comes out of a file this application did not write itself, and a "
				+ "wipe that stops at the first one leaves every later secret in memory. The tree "
				+ "carries a null node here too - the same file can have holes in either place");
	}

	@Test
	@DisplayName("closeVault overwrites an attachment's bytes, not only the entry's text")
	void closeVault_overwritesAttachments_whichWereMeasuredIntactAfterAClose()
	{
		byte[] attached = "the recovery codes somebody put IN their vault".getBytes();
		FileContentInfo attachment = FileContentInfo.builder().content(attached).build();
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.password(ENTRY_PASSWORD.clone()).resources(new ArrayList<>(List.of(attachment)))
			.build();
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.setDataOfNodes(entriesByNodeId(entry));

		VaultCloseSupport.closeVault(applicationModelBean);

		assertArrayEquals(new byte[attached.length], attached,
			"an attachment is a file somebody put into their password database precisely so it "
				+ "would not lie around elsewhere. It was measured still readable in memory after "
				+ "a close while the passwords beside it were already gone (#242). The buffer is "
				+ "held across the close here on purpose: asserting the FIELD is null would pass "
				+ "on a plain dereference and prove nothing about the bytes");
		assertNull(attachment.getContent(), "and the field is cleared as well as the buffer");
		assertNull(entry.getResources());
	}

	@Test
	@DisplayName("closeVault overwrites the private key a key-file vault was opened with")
	void closeVault_overwritesThePrivateKey_whichIsTheOtherWayIn()
	{
		byte[] encoded = "the encoded private key bytes".getBytes();
		KeyModel privateKeyInfo = KeyModel.builder().encoded(encoded).algorithm("RSA")
			.keyType(KeyType.PRIVATE_KEY).build();
		char[] repeatPassword = "master-password".toCharArray();
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.getMasterPwFileModelBean().setPrivateKeyInfo(privateKeyInfo);
		applicationModelBean.getMasterPwFileModelBean().setRepeatPw(repeatPassword);

		VaultCloseSupport.closeVault(applicationModelBean);

		assertArrayEquals(new byte[encoded.length], encoded,
			"a vault opened with a key file is decrypted by this key. Forgetting the master "
				+ "password and keeping the key forgets one of the two ways in (#242)");
		assertNull(applicationModelBean.getMasterPwFileModelBean(),
			"the holder goes with the credentials; KeyModel declares its fields final, so the "
				+ "overwritten buffer is all this layer can reach and all that matters");
		assertArrayEquals(new char[repeatPassword.length], repeatPassword,
			"the repeated password is the same secret typed twice, and it was left behind");
	}

	@Test
	@DisplayName("an entry's custom properties are dropped - they cannot be overwritten")
	void closeVault_dropsTheProperties_becauseAStringCannotBeWiped()
	{
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.password(ENTRY_PASSWORD.clone()).properties(new ArrayList<>(List.of(KeyValuePair
				.<String, String> builder().key("TOTP seed").value("JBSWY3DPEHPK3PXP").build())))
			.build();
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.setDataOfNodes(entriesByNodeId(entry));

		VaultCloseSupport.closeVault(applicationModelBean);

		assertNull(entry.getProperties(),
			"this assertion is a null check on purpose, and it is the only one here that is. A "
				+ "property value is a String, so dropping it is all this layer can do - the same "
				+ "state the six entry fields were in before #294. It is asserted as a DROP and "
				+ "written up as one rather than counted among the wipes");
	}

	@Test
	@DisplayName("a null attachment does not stop the wipe of the ones behind it")
	void closeVault_wipesEveryAttachment_whenTheListHasAHole()
	{
		byte[] attached = "the second one".getBytes();
		FileContentInfo attachment = FileContentInfo.builder().content(attached).build();
		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.password(ENTRY_PASSWORD.clone())
			.resources(new ArrayList<>(Arrays.asList(null, attachment))).build();
		ApplicationModelBean applicationModelBean = openVault();
		applicationModelBean.setDataOfNodes(entriesByNodeId(entry));

		VaultCloseSupport.closeVault(applicationModelBean);

		assertArrayEquals(new byte[attached.length], attached,
			"a wipe that stops at the first hole leaves every attachment behind it in memory");
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
