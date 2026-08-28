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
package io.github.astrapi69.mystic.crypt.panel.privatekey;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.PrivateKeyExtensions;
import io.github.astrapi69.crypt.data.key.PublicKeyExtensions;
import io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader;

/**
 * Tests that the two key views of {@link PrivateKeyViewPanel} are bound to
 * {@link PrivateKeyViewPanelModel}: the key texts the application puts into the panel are readable
 * from the model afterwards, instead of only out of the two text areas.
 * <p>
 * The proof is a real key pair: what the model hands out is written to a file and read back as a
 * private key, which only works if the whole PEM travelled through the model.
 */
class PrivateKeyViewPanelBindingTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		// self-contained on purpose: the application registers the provider at startup, but a test
		// must never depend on another class having run first
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	/**
	 * Filling the view the way the application does it (see {@code OpenPrivateKeyAction}) leaves
	 * both key texts in the model, and the private key text is still a key
	 */
	@Test
	void theKeyTextsThePanelIsFilledWithAreReadableFromTheModel(@TempDir File directory)
		throws Exception
	{
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		String privateKeyFormat = PrivateKeyExtensions.toPemFormat(keyPair.getPrivate());
		String publicKeyFormat = PublicKeyExtensions.toPemFormat(keyPair.getPublic());
		PrivateKeyViewPanel panel = new PrivateKeyViewPanel();

		panel.getTxtPrivateKey().setText(privateKeyFormat);
		panel.getTxtPublicKey().setText(publicKeyFormat);

		assertEquals(privateKeyFormat, panel.getPanelModelObject().getPrivateKeyText(),
			"the private key text did not reach the model");
		assertEquals(publicKeyFormat, panel.getPanelModelObject().getPublicKeyText(),
			"the public key text did not reach the model");

		File fromModel = new File(directory, "from-model.pem");
		Files.writeString(fromModel.toPath(), panel.getPanelModelObject().getPrivateKeyText());
		assertArrayEquals(keyPair.getPrivate().getEncoded(),
			PrivateKeyReader.readPemPrivateKey(fromModel).getEncoded(),
			"the private key that came back out of the model is not the one that went in");
	}

	/**
	 * Before a key is opened the model holds empty texts, so whoever reads it never gets a null
	 */
	@Test
	void theModelStartsWithEmptyKeyTexts()
	{
		PrivateKeyViewPanel panel = new PrivateKeyViewPanel();

		assertEquals("", panel.getPanelModelObject().getPrivateKeyText());
		assertEquals("", panel.getPanelModelObject().getPublicKeyText());
	}

	/**
	 * Clearing the views empties the model as well - the application clears both views before it
	 * fills them with the next key, and a stale key must not survive that in the model
	 */
	@Test
	void clearingTheViewsEmptiesTheModel() throws Exception
	{
		PrivateKeyViewPanel panel = new PrivateKeyViewPanel();
		KeyPair keyPair = KeyPairFactory.newKeyPair("RSA");
		panel.getTxtPrivateKey().setText(PrivateKeyExtensions.toPemFormat(keyPair.getPrivate()));
		panel.getTxtPublicKey().setText(PublicKeyExtensions.toPemFormat(keyPair.getPublic()));
		assertTrue(panel.getPanelModelObject().getPrivateKeyText().contains("BEGIN"));

		panel.getTxtPrivateKey().setText("");
		panel.getTxtPublicKey().setText("");

		assertEquals("", panel.getPanelModelObject().getPrivateKeyText());
		assertEquals("", panel.getPanelModelObject().getPublicKeyText());
	}

	/**
	 * Both key views stay read only: they show what was opened or generated and are never typed in
	 */
	@Test
	void bothKeyViewsStayReadOnly()
	{
		PrivateKeyViewPanel panel = new PrivateKeyViewPanel();

		assertEquals(false, panel.getTxtPrivateKey().isEditable());
		assertEquals(false, panel.getTxtPublicKey().isEditable());
	}
}
