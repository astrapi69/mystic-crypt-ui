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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.action.CloseApplicationFileAction;
import io.github.astrapi69.mystic.crypt.action.OpenExistingDatabaseAction;
import io.github.astrapi69.mystic.crypt.app.file.xml.PasswordVaultFormat;
import io.github.astrapi69.mystic.crypt.app.file.xml.VaultXmlCodec;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * The whole chain on a vault of an earlier release: open the fixture through the sign-in, change
 * one entry, save, lock, unlock, close the database and open it again; the change is there and
 * nothing else moved (TS-0017, TC-0170).
 * <p>
 * "Nothing else moved" is a fingerprint, not a count of titles: per entry one SHA-256 over every
 * content field, the attachments' names and bytes and the properties, taken from the groups the
 * tree panel holds. The fingerprint of the entries that were not touched is the same after the
 * unlock and after the reopen as before the change, and the file the save wrote is in this
 * version's format. The fixtures were written by the releases themselves, see
 * {@code src/testSupport/resources/compat/vaults/README.md}; a vault of the maintainer's own stays
 * a manual case. A message names the group, the index and a release, never a value.
 */
class AnEarlierVaultKeepsItsChangeThroughTheWholeChainUiTest extends AbstractUiTest
{

	private static final String CHANGED_TITLE = "the mail, changed";

	static Stream<Arguments> fixtures()
	{
		return Stream.of(Arguments.of("8.14", TestPasswords.VAULT_OF_8_14_FIXTURE),
			Arguments.of("7.3", TestPasswords.VAULTS_OF_7X_FIXTURE));
	}

	@ParameterizedTest(name = "a vault of {0}: change, save, lock, unlock, close, reopen")
	@DisplayName("the change survives the whole chain on a vault of an earlier release, and nothing else moves")
	@MethodSource("fixtures")
	void theChange_survivesTheWholeChain_andNothingElseMoves(final String release,
		final String password) throws Exception
	{
		File vault = copyOfTheFixture(release);
		ApplicationSteps application = signInWithExistingDatabase(vault, password);
		FrameFixture frame = application.showMainFrame();
		Map<String, String> asOpened = fingerprint();
		assertEquals(2, asOpened.size(), release + ": the precondition: both entries are there");
		assertEquals("1 attachment, 1 property", whatTheBankCarries(),
			release + ": the precondition: the release's own content is read, not only compared");
		String newPassword = TestPasswords.throwaway();

		application.selectTreeRowByName(frame, "mykeys").selectEntryRowByTitle(frame, "the mail");
		application.openTheEditDialog(frame).typeTitle(CHANGED_TITLE).typePassword(newPassword)
			.ok();
		Map<String, String> afterTheChange = fingerprint();
		assertTrue(application.entryExistsWithTitle(CHANGED_TITLE),
			release + ": the change is in the model");
		assertNotEquals(asOpened.get("mykeys#0"), afterTheChange.get("mykeys#0"),
			release + ": the precondition: the edit changed the entry's fingerprint");
		assertEquals(asOpened.get("root#0"), afterTheChange.get("root#0"),
			release + ": the edit touched nothing else");

		application.saveDatabase();
		application.lockWorkspace();
		application.unlockWorkspace(password);
		assertEquals(afterTheChange, fingerprint(),
			release + ": after the unlock every entry reads as before the lock");

		closeTheDatabase();
		openInTheRunningApplication(vault, password);
		Map<String, String> reopened = fingerprint();
		assertEquals(afterTheChange, reopened,
			release + ": after the close and the reopen every entry reads as saved");
		assertEquals(asOpened.get("root#0"), reopened.get("root#0"),
			release + ": the entry nobody touched, with its attachment and property, is as the "
				+ "release wrote it");
		assertTrue(application.entryExistsWithTitle(CHANGED_TITLE),
			release + ": the change is there after the reopen");
		assertEquals("1 attachment, 1 property", whatTheBankCarries(),
			release + ": the attachment and the property are still there after the reopen");
		assertTrue(PasswordVaultFormat.isCurrentFormat(Files.readAllBytes(vault.toPath())),
			release + ": the file is in this version's format");
		String written = new String(PasswordVaultFormat.decrypt(vault, password.toCharArray()));
		assertTrue(written.contains("formatVersion=\"" + VaultXmlCodec.FORMAT_VERSION + "\""),
			release + ": and says so");
	}

	private File copyOfTheFixture(final String release) throws IOException
	{
		File copy = new File(tempHome, "vault-" + release + ".mcrdb");
		try (InputStream in = getClass()
			.getResourceAsStream("/compat/vaults/vault-" + release + ".mcrdb"))
		{
			assertTrue(in != null, "the fixture of " + release + " is on the class path");
			Files.copy(in, copy.toPath());
		}
		return copy;
	}

	/** Opens a database file through "Open Database..." of the running application */
	private void openInTheRunningApplication(final File databaseFile, final String password)
	{
		SwingUtilities.invokeLater(() -> new OpenExistingDatabaseAction("Open Database File")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		awaitSignInDialog().checkMasterPassword().typeMasterPassword(password)
			.browseApplicationFile(databaseFile).requireOkEnabled().okAndAwaitSignIn();
		new ApplicationSteps(robot).awaitSignedIn();
		robot.waitForIdle();
	}

	/** Closes the open database the way File > Close does */
	private void closeTheDatabase()
	{
		GuiActionRunner.execute(() -> new CloseApplicationFileAction("Close")
			.actionPerformed(new ActionEvent(MysticCryptApplicationFrame.getInstance(), 0, "")));
		robot.waitForIdle();
		assertFalse(
			GuiActionRunner.execute(
				() -> MysticCryptApplicationFrame.getInstance().getModelObject().isSignedIn()),
			"the precondition: no database is open after the close");
	}

	/**
	 * One digest per entry, keyed by the group's name and the entry's index in it
	 *
	 * @return the digests in tree order
	 */
	private static Map<String, String> fingerprint()
	{
		return GuiActionRunner.execute(() -> {
			Map<String, String> digests = new LinkedHashMap<>();
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			root.traverse().stream()
				.filter(
					node -> node.getValue() != null && node.getValue().getDefaultContent() != null)
				.forEach(node -> {
					List<MysticCryptEntryModelBean> entries = node.getValue().getDefaultContent();
					for (int index = 0; index < entries.size(); index++)
					{
						digests.put(node.getValue().getName() + "#" + index,
							digestOf(entries.get(index)));
					}
				});
			return digests;
		});
	}

	/**
	 * What the first entry of the group "root" carries, counted from the model. The fingerprints
	 * compare the vault with itself as it was read, so a reader that dropped a field would drop it
	 * from both sides; this count is the release's own content, written down.
	 *
	 * @return the number of attachments and properties, as text
	 */
	private static String whatTheBankCarries()
	{
		return GuiActionRunner.execute(() -> {
			BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = MysticCryptApplicationFrame
				.getInstance().getApplicationPanel().getSecretKeyTreeWithContentPanel()
				.getModelObject();
			MysticCryptEntryModelBean bank = root.traverse().stream()
				.filter(node -> node.getValue() != null && "root".equals(node.getValue().getName())
					&& node.getValue().getDefaultContent() != null)
				.map(node -> node.getValue().getDefaultContent().getFirst()).findFirst()
				.orElseThrow();
			return orEmpty(bank.getResources()).size() + " attachment, "
				+ orEmpty(bank.getProperties()).size() + " property";
		});
	}

	private static String digestOf(final MysticCryptEntryModelBean entry)
	{
		List<String> fields = new ArrayList<>();
		fields.add(text(entry.getTitle()));
		fields.add(text(entry.getUserName()));
		fields.add(text(entry.getPassword()));
		fields.add(text(entry.getRepeat()));
		fields.add(text(entry.getUrl()));
		fields.add(text(entry.getNotes()));
		fields.add(entry.isExpirable() + "|" + entry.getExpires() + "|" + entry.getIcon() + "|"
			+ entry.getKeePassIconIndex() + "|" + entry.isShowPassword());
		fields.add("resources " + (entry.getResources() == null ? "null" : "listed"));
		for (FileContentInfo resource : orEmpty(entry.getResources()))
		{
			fields.add("resource " + resource.getName() + " "
				+ HexFormat.of().formatHex(resource.getContent()));
		}
		fields.add("properties " + (entry.getProperties() == null ? "null" : "listed"));
		for (KeyValuePair<String, String> property : orEmpty(entry.getProperties()))
		{
			fields.add("property " + property.getKey() + "=" + property.getValue());
		}
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(
				digest.digest(String.join("\0", fields).getBytes(StandardCharsets.UTF_8)));
		}
		catch (java.security.NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("the JDK has no SHA-256", e);
		}
	}

	private static <T> List<T> orEmpty(final List<T> list)
	{
		return list == null ? List.of() : list;
	}

	/** A field's text, "null" for a field the release never wrote, so absent and empty differ */
	private static String text(final char[] characters)
	{
		return characters == null ? "null" : "[" + new String(characters) + "]";
	}
}
