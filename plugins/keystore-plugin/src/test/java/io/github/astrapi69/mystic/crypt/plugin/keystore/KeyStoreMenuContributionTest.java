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
package io.github.astrapi69.mystic.crypt.plugin.keystore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Security;

import javax.swing.JMenuItem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import io.github.astrapi69.mystic.crypt.plugin.keystore.wizard.CreateKeyStoreWizardModel;

/**
 * Covers what {@link KeyStoreMenuContribution} does without opening a real dialog: the two menu
 * items it now contributes ("Manage Key Store" unchanged, plus the new "Create Key Store..."), and
 * the summary the Review step and Finish both rely on.
 */
class KeyStoreMenuContributionTest
{

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	void contributesBothMenuItemsWithManageKeyStoreUnchanged()
	{
		KeyStoreMenuContribution contribution = new KeyStoreMenuContribution();

		java.util.List<JMenuItem> menuItems = contribution.getMenuItems();

		assertEquals(2, menuItems.size());
		assertEquals("Manage Key Store", menuItems.get(0).getText());
		assertEquals("Create Key Store...", menuItems.get(1).getText());
		assertEquals("Key Stores", contribution.getMenuName());
	}

	@Test
	void buildSummaryNamesTheFileAndTypeAndSaysNoKeyPairWhenNoneWasRequested()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		model.setKeyStoreFilePath("/tmp/new.p12");
		model.setKeystoreType(KeystoreType.PKCS12);

		String summary = KeyStoreMenuContribution.buildSummary(model);

		assertTrue(summary.contains("/tmp/new.p12"), summary);
		assertTrue(summary.contains("PKCS12"), summary);
		assertTrue(summary.contains("no key pair"), summary);
		assertFalse(summary.contains("Alias"), summary);
	}

	@Test
	void buildSummaryNamesTheKeyPairWhenOneWasRequested()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		model.setKeyStoreFilePath("/tmp/new.p12");
		model.setKeystoreType(KeystoreType.PKCS12);
		model.setAddKeyPairNow(true);
		model.setAlias("server");
		model.setDistinguishedName("CN=example.com");
		model.setKeyAlgorithm(KeyPairGeneratorAlgorithm.EC);

		String summary = KeyStoreMenuContribution.buildSummary(model);

		assertTrue(summary.contains("server"), summary);
		assertTrue(summary.contains("CN=example.com"), summary);
		assertTrue(summary.contains("EC"), summary);
	}
}
