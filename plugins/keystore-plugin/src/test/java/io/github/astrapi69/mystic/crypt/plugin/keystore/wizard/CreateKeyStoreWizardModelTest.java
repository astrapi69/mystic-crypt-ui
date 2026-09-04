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
package io.github.astrapi69.mystic.crypt.plugin.keystore.wizard;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.mystic.crypt.plugin.keystore.KeyStoreSupport;

/**
 * Tests that {@link CreateKeyStoreWizardModel} starts with sensible defaults and carries every value
 * written into it, since every wizard step reads and writes this model rather than the widgets.
 */
class CreateKeyStoreWizardModelTest
{

	@Test
	void startsWithTheFirstOfferedTypeAndAlgorithmAndNoKeyPairChosen()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();

		assertEquals("", model.getKeyStoreFilePath());
		assertEquals(KeyStoreSupport.USABLE_TYPES.get(0), model.getKeystoreType());
		assertArrayEquals(new char[0], model.getStorePassword());
		assertArrayEquals(new char[0], model.getStorePasswordRepeated());
		assertFalse(model.isAddKeyPairNow());
		assertEquals("", model.getAlias());
		assertEquals("", model.getDistinguishedName());
		assertEquals(KeyStoreSupport.KEY_ALGORITHMS.get(0), model.getKeyAlgorithm());
	}

	@Test
	void carriesEveryValueWrittenIntoIt()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();

		model.setKeyStoreFilePath("/tmp/new.p12");
		model.setKeystoreType(KeystoreType.JKS);
		model.setStorePassword("secret".toCharArray());
		model.setStorePasswordRepeated("secret".toCharArray());
		model.setAddKeyPairNow(true);
		model.setAlias("server");
		model.setDistinguishedName("CN=example.com");
		model.setKeyAlgorithm(KeyPairGeneratorAlgorithm.EC);

		assertEquals("/tmp/new.p12", model.getKeyStoreFilePath());
		assertEquals(KeystoreType.JKS, model.getKeystoreType());
		assertArrayEquals("secret".toCharArray(), model.getStorePassword());
		assertArrayEquals("secret".toCharArray(), model.getStorePasswordRepeated());
		assertEquals(true, model.isAddKeyPairNow());
		assertEquals("server", model.getAlias());
		assertEquals("CN=example.com", model.getDistinguishedName());
		assertEquals(KeyPairGeneratorAlgorithm.EC, model.getKeyAlgorithm());
	}
}
