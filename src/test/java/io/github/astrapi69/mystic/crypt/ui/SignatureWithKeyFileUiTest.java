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
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.crypt.data.key.writer.PublicKeyWriter;

/**
 * Functional end-to-end test of signing the way it is actually done: with a key that already exists
 * on disk, over a file rather than a typed message - and with a signature that is saved and read
 * back. Signing with a throwaway key over a text field is covered by
 * {@link PqcSignaturePluginUiTest}.
 */
class SignatureWithKeyFileUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "signature-keyfile-e2e-pw-123";

	@Test
	void signsAFileWithAKeyFromDiskAndVerifiesItAgain() throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		installPluginRequiringItBuilt(PQC_SIGNATURE_ZIP);

		File databaseFile = new File(tempHome, "signature-keyfile.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		// a key pair on disk, the way one arrives from a key store or a certificate authority
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		File privateKeyFile = new File(tempHome, "signing-key.pem");
		File publicKeyFile = new File(tempHome, "signing-key-public.pem");
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), privateKeyFile);
		PublicKeyWriter.writeInPemFormat(keyPair.getPublic(), publicKeyFile);

		File documentFile = new File(tempHome, "contract.txt");
		Files.writeString(documentFile.toPath(), "this is the document that gets signed");
		File signatureFile = new File(tempHome, "contract.sig");

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Sign and Verify", "Sign and Verify");

		GuiActionRunner.execute(() -> {
			frame.textBox("txtPrivateKeyFile").target().setText(privateKeyFile.getAbsolutePath());
			frame.textBox("txtPublicKeyFile").target().setText(publicKeyFile.getAbsolutePath());
			frame.textBox("txtDataFile").target().setText(documentFile.getAbsolutePath());
			frame.checkBox("chkUseFile").target().setSelected(true);
			frame.textBox("txtSignatureFile").target().setText(signatureFile.getAbsolutePath());
		});
		robot.waitForIdle();

		GuiActionRunner.execute(() -> frame.button("btnSign").target().doClick());
		robot.waitForIdle();
		assertTrue(result(frame).contains("SHA256withRSA"),
			"the algorithm must come from the key itself: " + result(frame));

		GuiActionRunner.execute(() -> frame.button("btnVerify").target().doClick());
		robot.waitForIdle();
		assertEquals("signature is valid", result(frame),
			"a signature over the file must verify against the public key from the file");

		// save it, forget it, read it back - and it must still verify
		GuiActionRunner.execute(() -> frame.button("btnSaveSignature").target().doClick());
		robot.waitForIdle();
		assertTrue(signatureFile.exists(), "saving must write the signature: " + result(frame));

		GuiActionRunner.execute(() -> frame.textBox("txtSignature").target().setText(""));
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnLoadSignature").target().doClick());
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnVerify").target().doClick());
		robot.waitForIdle();
		assertEquals("signature is valid", result(frame),
			"a signature that was saved and read back must still verify");

		// change the document - the same signature must no longer fit
		Files.writeString(documentFile.toPath(), "this is a different document");
		GuiActionRunner.execute(() -> frame.button("btnVerify").target().doClick());
		robot.waitForIdle();
		assertEquals("signature is not valid", result(frame),
			"a changed file must not verify with the old signature");
		assertFalse(result(frame).contains("valid signature"));
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
