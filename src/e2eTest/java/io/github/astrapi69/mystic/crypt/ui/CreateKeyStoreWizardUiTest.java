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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.Security;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.crypt.data.factory.KeyStoreFactory;
import io.github.astrapi69.crypt.data.key.KeyStoreExtensions;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of the key store plugin's guided "Create Key Store..." wizard (issue
 * #191), driving the real UI Store -> Entry -> Review -> Finish. The unchanged "Manage Key Store"
 * panel keeps its own coverage in {@link KeyStorePluginUiTest}/{@link KeyStoreImportUiTest} - this
 * file only covers the new wizard.
 * <p>
 * The plugin's own {@code KeyStoreSupport} lives in a separate, isolated Gradle build loaded at
 * runtime through pf4j and is not on this module's compile classpath, so stores are read back
 * afterwards straight through the same crypt-data classes that class itself wraps
 * ({@link KeyStoreFactory}, {@link KeyStoreExtensions}) - the same library, just called directly.
 */
class CreateKeyStoreWizardUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String STORE_PASSWORD = TestPasswords.throwaway();

	@Test
	@DisplayName("walking Store -> Entry (skip key pair) -> Review -> Finish creates a real, empty, readable store")
	void finishWithoutAKeyPairCreatesAnEmptyReadableStore() throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		installPluginRequiringItBuilt(KEYSTORE_ZIP);
		File databaseFile = new File(tempHome, "keystore-wizard-empty.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		DialogFixture wizard = application.openKeyStoreWizard();

		File storeFile = new File(tempHome, "empty-wizard.p12");
		fillStoreStep(wizard, storeFile, STORE_PASSWORD, STORE_PASSWORD);
		click(wizard, "Next");
		assertTrue(isShowing(wizard, "chkAddKeyPairNow"), "Next must move the wizard to Entry");

		// leave "Add a key pair now" unchecked - the store stays empty
		click(wizard, "Next");
		assertTrue(isShowing(wizard, "txtSummary"), "Next must move the wizard to Review");

		click(wizard, "Finish");

		assertFalse(wizard.target().isShowing(), "the wizard must close after a successful Finish");
		assertTrue(storeFile.exists(), "the store must actually be written to " + storeFile);
		KeyStore reopened = KeyStoreFactory.loadKeyStore(storeFile, KeystoreType.PKCS12.getType(),
			STORE_PASSWORD);
		assertEquals(0, Collections.list(reopened.aliases()).size(),
			"skipping the Entry step must produce an empty store");
	}

	@Test
	@DisplayName("walking Store -> Entry (with a key pair) -> Review -> Finish creates a store with exactly that entry")
	void finishWithAKeyPairCreatesAStoreWithExactlyThatEntry() throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		installPluginRequiringItBuilt(KEYSTORE_ZIP);
		File databaseFile = new File(tempHome, "keystore-wizard-entry.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		DialogFixture wizard = application.openKeyStoreWizard();

		File storeFile = new File(tempHome, "entry-wizard.p12");
		fillStoreStep(wizard, storeFile, STORE_PASSWORD, STORE_PASSWORD);
		click(wizard, "Next");
		assertTrue(isShowing(wizard, "chkAddKeyPairNow"), "Next must move the wizard to Entry");

		GuiActionRunner.execute(() -> ((JCheckBox)named(wizard, "chkAddKeyPairNow")).doClick());
		GuiActionRunner
			.execute(() -> ((JTextField)named(wizard, "txtAlias")).setText("wizard-server"));
		GuiActionRunner.execute(() -> ((JTextField)named(wizard, "txtDistinguishedName"))
			.setText("CN=wizard.example.com"));
		robot.waitForIdle();

		click(wizard, "Next");
		assertTrue(isShowing(wizard, "txtSummary"), "Next must move the wizard to Review");
		String summary = GuiActionRunner
			.execute(() -> ((javax.swing.JTextArea)named(wizard, "txtSummary")).getText());
		assertTrue(summary.contains("wizard-server"), summary);

		click(wizard, "Finish");

		assertFalse(wizard.target().isShowing(), "the wizard must close after a successful Finish");
		KeyStore reopened = KeyStoreFactory.loadKeyStore(storeFile, KeystoreType.PKCS12.getType(),
			STORE_PASSWORD);
		assertEquals(List.of("wizard-server"), Collections.list(reopened.aliases()),
			"Finish must add exactly the one requested key pair");
		assertEquals(KeyPairGeneratorAlgorithm.RSA.name(), KeyStoreExtensions
			.getPrivateKey(reopened, "wizard-server", STORE_PASSWORD.toCharArray()).getAlgorithm(),
			"the entry must be readable back with the algorithm the Entry step defaulted to");
	}

	@Test
	@DisplayName("Store does not advance when the repeated password does not match")
	void storeStepRefusesToAdvanceOnAPasswordMismatch() throws Exception
	{
		installPluginRequiringItBuilt(KEYSTORE_ZIP);
		File databaseFile = new File(tempHome, "keystore-wizard-mismatch.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		DialogFixture wizard = application.openKeyStoreWizard();

		File storeFile = new File(tempHome, "mismatch-wizard.p12");
		fillStoreStep(wizard, storeFile, STORE_PASSWORD, "not-the-same-password");

		click(wizard, "Next");

		assertTrue(isShowing(wizard, "txtKeyStoreFile"),
			"a mismatched repeated password must not let the wizard advance past Store");

		GuiActionRunner.execute(() -> wizard.target().dispose());
		robot.waitForIdle();
	}

	@Test
	@DisplayName("Finish refuses to overwrite a file that already exists at the target path")
	void finishRefusesToOverwriteAnExistingFile() throws Exception
	{
		installPluginRequiringItBuilt(KEYSTORE_ZIP);
		File databaseFile = new File(tempHome, "keystore-wizard-overwrite.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		DialogFixture wizard = application.openKeyStoreWizard();

		File storeFile = new File(tempHome, "already-there-wizard.p12");
		String originalContent = "whatever was already there must survive";
		Files.writeString(storeFile.toPath(), originalContent, StandardCharsets.UTF_8);

		fillStoreStep(wizard, storeFile, STORE_PASSWORD, STORE_PASSWORD);
		click(wizard, "Next");
		click(wizard, "Next");
		assertTrue(isShowing(wizard, "txtSummary"), "Next must move the wizard to Review");

		// fired without waiting for it to finish: the click itself blocks on the EDT until the
		// error dialog it triggers is dismissed, so a synchronous click() here would deadlock the
		// test the same way it would deadlock the real application if nothing ever answered it
		javax.swing.SwingUtilities.invokeLater(() -> onlyOneMatching(wizard, "Finish").doClick());

		DialogFixture failureDialog = application.findDialogWithTitle("Key store creation failed");
		assertTrue(failureDialog.target().isShowing(),
			"an existing file at the target path must be refused with a dialog naming the reason");
		failureDialog.close();
		robot.waitForIdle();

		assertTrue(wizard.target().isShowing(),
			"the wizard must stay open after a refused Finish, nothing was saved to act on");
		assertEquals(originalContent, Files.readString(storeFile.toPath(), StandardCharsets.UTF_8),
			"the file that was already there must not have been touched");

		GuiActionRunner.execute(() -> wizard.target().dispose());
		robot.waitForIdle();
	}

	/**
	 * Fills the Store step's file path and both password fields, the way a user types into them
	 */
	private void fillStoreStep(DialogFixture wizard, File storeFile, String password,
		String repeatedPassword)
	{
		GuiActionRunner.execute(() -> ((JTextField)named(wizard, "txtKeyStoreFile"))
			.setText(storeFile.getAbsolutePath()));
		GuiActionRunner
			.execute(() -> ((JPasswordField)named(wizard, "pwdStorePassword")).setText(password));
		GuiActionRunner.execute(() -> ((JPasswordField)named(wizard, "pwdStorePasswordRepeated"))
			.setText(repeatedPassword));
		robot.waitForIdle();
	}

	/**
	 * Whether the named field of a step is currently on screen - the step panels sit in a
	 * {@link java.awt.CardLayout}, which hides every step but the current one, so a field's own
	 * {@code isShowing()} says which step the wizard is actually on without needing a name on the
	 * card panel itself (the wizard content panel only names its cards for the layout, not the
	 * panels it holds)
	 */
	private boolean isShowing(DialogFixture wizard, String fieldName)
	{
		return GuiActionRunner.execute(() -> named(wizard, fieldName).isShowing());
	}

	private java.awt.Component named(final DialogFixture wizard, final String name)
	{
		return wizard.robot().finder().find(wizard.target(),
			component -> name.equals(component.getName()));
	}

	private void click(final DialogFixture wizard, final String buttonText)
	{
		GuiActionRunner.execute(() -> {
			JButton button = (JButton)wizard.robot().finder().find(wizard.target(),
				JButtonMatcher.withText(buttonText));
			button.doClick();
			return null;
		});
		robot.waitForIdle();
	}

	/**
	 * The wizard must hold exactly one button with this text - {@code find} (as opposed to
	 * {@code findAll}) fails on its own, with every match listed, if there is more than one
	 */
	private JButton onlyOneMatching(final DialogFixture wizard, final String buttonText)
	{
		return (JButton)wizard.robot().finder().find(wizard.target(),
			JButtonMatcher.withText(buttonText));
	}
}
