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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.JFileChooserFinder;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.key.reader.CertificateReader;
import io.github.astrapi69.crypt.data.key.reader.EncryptedPrivateKeyReader;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;
import io.github.astrapi69.crypt.data.key.reader.PublicKeyReader;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * What the key generation window writes to disk has to be usable afterwards, so every file it
 * produces is read back here with the readers the rest of the application uses, and the key pair is
 * made to sign and verify something. A file that exists and is unreadable would pass a length check
 * and fail a user.
 */
class KeygenSavesRealArtifactsUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private ApplicationSteps openTheKeyGenerationWindow(final String databaseName) throws Exception
	{
		installPluginRequiringItBuilt(KEYGEN_ZIP);
		File databaseFile = new File(tempHome, databaseName);
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		application.showMainFrame();
		application.openPluginTool("Key Generation", "Key generation demo");
		return application;
	}

	private void generateTheDefaultKeyPair(final FrameFixture frame)
	{
		GuiActionRunner.execute(() -> frame.button("btnGenerate").target().doClick());
		robot.waitForIdle();
	}

	/**
	 * Presses the button with the given text, answers the file chooser with the file a user would
	 * type, and waits for the file the window is expected to write
	 *
	 * @param frame
	 *            the application frame
	 * @param buttonText
	 *            the button to press
	 * @param target
	 *            what is typed into the chooser
	 * @param expected
	 *            the file that has to appear
	 */
	private void saveThrough(final FrameFixture frame, final String buttonText, final File target,
		final File expected)
	{
		SwingUtilities.invokeLater(
			() -> frame.button(JButtonMatcher.withText(buttonText)).target().doClick());
		approveFileChooserWith(target, expected);
	}

	/**
	 * Picks the given file in the chooser and waits for the file the window is expected to write.
	 * The two differ on purpose: a user types a name without an ending, and the window has to add
	 * the one that matches what it writes.
	 *
	 * @param target
	 *            what is typed into the chooser
	 * @param expected
	 *            the file that has to appear
	 */
	private void approveFileChooserWith(final File target, final File expected)
	{
		JFileChooser fileChooser = JFileChooserFinder.findFileChooser()
			.withTimeout(15, TimeUnit.SECONDS).using(robot).target();
		SwingUtilities.invokeLater(() -> {
			fileChooser.setSelectedFile(target);
			fileChooser.approveSelection();
		});
		robot.waitForIdle();
		waitUntilWritten(expected);
	}

	private void waitUntilWritten(final File target)
	{
		org.assertj.swing.timing.Pause.pause(
			new org.assertj.swing.timing.Condition("the file " + target.getName() + " is written")
			{
				@Override
				public boolean test()
				{
					return target.exists() && 0 < target.length();
				}
			}, 15000);
	}

	private PublicKey publicKeyOnScreen(final FrameFixture frame, final String name)
		throws Exception
	{
		String pem = GuiActionRunner
			.execute(() -> frame.textBox("txtPublicKey").target().getText());
		File publicKeyFile = new File(tempHome, name);
		Files.writeString(publicKeyFile.toPath(), pem);
		return PublicKeyReader.readPemPublicKey(publicKeyFile);
	}

	/** Proves the two keys belong together, which a file that merely parses does not */
	private void assertTheyBelongTogether(final PrivateKey privateKey, final PublicKey publicKey)
		throws Exception
	{
		byte[] message = ("signed at " + UUID.randomUUID()).getBytes();
		Signature signer = Signature.getInstance("SHA256withRSA");
		signer.initSign(privateKey);
		signer.update(message);
		byte[] signature = signer.sign();

		Signature verifier = Signature.getInstance("SHA256withRSA");
		verifier.initVerify(publicKey);
		verifier.update(message);
		assertTrue(verifier.verify(signature),
			"the saved private key does not belong to the public key the window shows");
	}

	@Test
	@DisplayName("every file the window saves is a real artifact of the key pair it generated")
	void everyFileTheWindowSavesIsARealArtifact() throws Exception
	{
		ApplicationSteps application = openTheKeyGenerationWindow("keygen-save-artifacts.mcrdb");
		FrameFixture frame = application.showMainFrame();
		generateTheDefaultKeyPair(frame);
		PublicKey onScreen = publicKeyOnScreen(frame, "public-on-screen.pem");

		theSavedPrivateKeyIsTheKeyTheWindowGenerated(frame, onScreen);
		thePasswordProtectedPrivateKeyOpensWithThatPassword(frame, onScreen);
		theSavedCertificateCarriesTheGeneratedKey(frame, onScreen);
		theChosenFormatIsWhatIsWritten(frame, onScreen);
	}

	/**
	 * Choosing the binary encoding has to reach the file, and the ending has to say so: a name that
	 * promises PEM over binary content is refused by whatever it is handed to next
	 *
	 * @param frame
	 *            the application frame
	 * @param onScreen
	 *            the public key the window shows
	 */
	private void theChosenFormatIsWhatIsWritten(final FrameFixture frame, final PublicKey onScreen)
		throws Exception
	{
		GuiActionRunner.execute(() -> frame.comboBox("cmbSaveFormat").target()
			.setSelectedItem(io.github.astrapi69.crypt.api.key.KeyFileFormat.DER));
		robot.waitForIdle();
		File chosen = new File(tempHome, "saved-as-binary");
		File expected = new File(tempHome, "saved-as-binary.der");

		saveThrough(frame, "Save private key", chosen, expected);

		assertFalse(
			new String(java.nio.file.Files.readAllBytes(expected.toPath())).contains("PRIVATE KEY"),
			"the file named .der holds PEM text");
		assertTheyBelongTogether(PrivateKeyReader.readPrivateKey(expected), onScreen);
	}

	/** The plain private key file has to be the partner of the public key on screen */
	private void theSavedPrivateKeyIsTheKeyTheWindowGenerated(final FrameFixture frame,
		final PublicKey onScreen) throws Exception
	{
		File chosen = new File(tempHome, "saved-private-key");
		File privateKeyFile = new File(tempHome, "saved-private-key.pem");

		saveThrough(frame, "Save private key", chosen, privateKeyFile);

		assertTheyBelongTogether(PrivateKeyReader.readPemPrivateKey(privateKeyFile), onScreen);
	}

	/** The protected file has to open with the password that was typed, and hold the same key */
	private void thePasswordProtectedPrivateKeyOpensWithThatPassword(final FrameFixture frame,
		final PublicKey onScreen) throws Exception
	{
		File chosenProtected = new File(tempHome, "saved-protected-key");
		File protectedKeyFile = new File(tempHome, "saved-protected-key.der");
		String password = TestPasswords.throwaway();

		SwingUtilities.invokeLater(() -> frame
			.button(JButtonMatcher.withText("Save private key with password")).target().doClick());
		JOptionPaneFixture passwordDialog = JOptionPaneFinder.findOptionPane()
			.withTimeout(15, TimeUnit.SECONDS).using(robot);
		GuiActionRunner.execute(() -> {
			passwordDialog.robot().finder()
				.find(passwordDialog.target(),
					org.assertj.swing.core.matcher.JTextComponentMatcher.withName("txtPassword"))
				.requestFocusInWindow();
		});
		GuiActionRunner.execute(() -> {
			((javax.swing.JPasswordField)passwordDialog.robot().finder().find(
				passwordDialog.target(),
				org.assertj.swing.core.matcher.JTextComponentMatcher.withName("txtPassword")))
					.setText(password);
			((javax.swing.JPasswordField)passwordDialog.robot().finder().find(
				passwordDialog.target(),
				org.assertj.swing.core.matcher.JTextComponentMatcher.withName("txtRepeatPassword")))
					.setText(password);
		});
		robot.waitForIdle();
		SwingUtilities.invokeLater(() -> passwordDialog.target().setValue("Set password"));
		approveFileChooserWith(chosenProtected, protectedKeyFile);

		PrivateKey opened = EncryptedPrivateKeyReader
			.readPasswordProtectedPrivateKey(protectedKeyFile, password);
		assertTheyBelongTogether(opened, onScreen);
	}

	/** The certificate has to carry the generated key and verify with it */
	private void theSavedCertificateCarriesTheGeneratedKey(final FrameFixture frame,
		final PublicKey onScreen) throws Exception
	{
		File chosenCertificate = new File(tempHome, "saved-certificate");
		File certificateFile = new File(tempHome, "saved-certificate.pem");

		SwingUtilities.invokeLater(
			() -> frame.button(JButtonMatcher.withText("Save certificate...")).target().doClick());
		JOptionPaneFixture certificateDialog = JOptionPaneFinder.findOptionPane()
			.withTimeout(15, TimeUnit.SECONDS).using(robot);
		// nothing is typed on purpose: this is the way the window is used, and pressing OK on the
		// form as it comes up used to end in a null pointer instead of a certificate
		SwingUtilities
			.invokeLater(() -> certificateDialog.target().setValue(JOptionPane.OK_OPTION));
		approveFileChooserWith(chosenCertificate, certificateFile);

		X509Certificate certificate = CertificateReader.readPemCertificate(certificateFile);
		assertArrayEquals(onScreen.getEncoded(), certificate.getPublicKey().getEncoded(),
			"the certificate does not carry the key the window generated");
		certificate.verify(onScreen);
	}

}
