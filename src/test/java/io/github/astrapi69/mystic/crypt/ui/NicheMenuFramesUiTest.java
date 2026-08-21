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

import java.io.File;
import java.security.KeyPair;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.MenuId;

/**
 * End-to-end smoke coverage for the niche menu use cases: every tool the menus offer after sign-in
 * must actually open its internal frame on the desktop pane - checksum verifier, console, key
 * generation, the der-to-pem converter, and the private key viewer (which goes through a file
 * chooser with a real PEM file). Each frame is closed again so equally titled frames stay
 * unambiguous.
 * <p>
 * Obfuscation is intentionally not covered here anymore: it moved out of the built-in menus into
 * the internal obfuscation plugin and now appears under the "Plugins" menu
 */
class NicheMenuFramesUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = "niche-db-pw-123";

	@Test
	void allNicheMenuActionsOpenTheirInternalFrames() throws Exception
	{
		File databaseFile = new File(tempHome, "niche-database.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);

		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);

		application
			.openInternalFrameViaMenu(MenuId.VERIFY_CHECKSUM.propertiesKey(), "Verify checksum")
			.closeInternalFrame("Verify checksum");

		application.openInternalFrameViaMenu(MenuId.CONSOLE.propertiesKey(), "Console")
			.closeInternalFrame("Console");

		application
			.openInternalFrameViaMenu(MenuId.SECRET_KEY_NEW.propertiesKey(), "Key generation demo")
			.closeInternalFrame("Key generation demo");

		application
			.openInternalFrameViaMenu(MenuId.CONVERT.propertiesKey(),
				"Convert *.der-file to *.pem-file")
			.closeInternalFrame("Convert *.der-file to *.pem-file");

		File privateKeyPemFile = new File(tempHome, "niche-private-key.pem");
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		PrivateKeyWriter.writeInPemFormat(keyPair.getPrivate(), privateKeyPemFile);
		application.openPrivateKeyViaMenu(privateKeyPemFile).closeInternalFrame("Private key view");
	}
}
