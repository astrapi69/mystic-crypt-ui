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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.data.factory.CertFactory;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.CertificateWriter;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * Functional end-to-end test of taking a key that came from somewhere else into a key store: the
 * usual case of a certificate authority handing out a key and its certificate, which then has to
 * end up in a store the application can use.
 */
class KeyStoreImportUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String STORE_PASSWORD = TestPasswords.throwaway();

	@Test
	void importsAKeyAndItsCertificateThroughTheUi() throws Exception
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
		installPluginRequiringItBuilt(KEYSTORE_ZIP);

		File databaseFile = new File(tempHome, "keystore-import.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		// what a certificate authority hands out: a private key and the certificate for it
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		X500Name name = new X500Name("CN=from the authority, O=elsewhere");
		X509Certificate certificate = CertFactory.newX509CertificateV3(keyPair, name,
			BigInteger.valueOf(7), new Date(System.currentTimeMillis() - 3600_000L),
			new Date(System.currentTimeMillis() + 86_400_000L), name, "SHA256withRSA");
		File privateKeyFile = new File(tempHome, "server-key.pem");
		File certificateFile = new File(tempHome, "server-certificate.pem");
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), privateKeyFile);
		CertificateWriter.writeInPemFormat(certificate, certificateFile);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.openPluginTool("Manage Key Store", "Manage Key Store");

		File storeFile = new File(tempHome, "server.p12");
		GuiActionRunner.execute(() -> {
			frame.textBox("txtKeyStoreFile").target().setText(storeFile.getAbsolutePath());
			frame.robot().finder().findByName("pwdStore", javax.swing.JPasswordField.class, true)
				.setText(STORE_PASSWORD);
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnCreate").target().doClick());
		robot.waitForIdle();
		assertTrue(storeFile.exists(), "creating must write the store: " + result(frame));

		GuiActionRunner.execute(() -> {
			frame.textBox("txtAlias").target().setText("server");
			frame.textBox("txtPrivateKeyFile").target().setText(privateKeyFile.getAbsolutePath());
			frame.textBox("txtCertificateFile").target().setText(certificateFile.getAbsolutePath());
		});
		robot.waitForIdle();
		GuiActionRunner.execute(() -> frame.button("btnImportKeyPair").target().doClick());
		robot.waitForIdle();

		assertEquals(1, rowCount(frame),
			"the imported key must show up as an entry: " + result(frame));
		assertEquals("server", cell(frame, 0, 0));
		assertEquals("private key", cell(frame, 0, 1),
			"an imported key pair is a key entry, not merely a certificate");
		assertTrue(cell(frame, 0, 3).contains("CN=from the authority"),
			"the certificate that came with the key must be the one in the store: "
				+ cell(frame, 0, 3));

		// and the store opens again without anyone having to say what kind it is
		GuiActionRunner.execute(() -> frame.button("btnOpen").target().doClick());
		robot.waitForIdle();
		assertEquals(1, rowCount(frame), "reopening must find the entry: " + result(frame));
	}

	private int rowCount(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.table("tblEntries").target().getRowCount());
	}

	private String cell(FrameFixture frame, int row, int column)
	{
		return GuiActionRunner.execute(
			() -> String.valueOf(frame.table("tblEntries").target().getValueAt(row, column)));
	}

	private String result(FrameFixture frame)
	{
		return GuiActionRunner.execute(() -> frame.label("lblResult").target().getText());
	}
}
