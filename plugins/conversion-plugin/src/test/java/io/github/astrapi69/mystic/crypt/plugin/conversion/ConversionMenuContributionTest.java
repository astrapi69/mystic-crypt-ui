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
package io.github.astrapi69.mystic.crypt.plugin.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.Security;

import javax.swing.JMenuItem;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.mystic.crypt.plugin.conversion.wizard.ConversionOperation;
import io.github.astrapi69.mystic.crypt.plugin.conversion.wizard.ConversionWizardModel;

/**
 * Covers what {@link ConversionMenuContribution} does without opening a real dialog: the single menu
 * item it now contributes (replacing the two former ones), and the helpers Finish and the Review step
 * both rely on to fill in what the user left blank - since Finish works from any step of the wizard,
 * not only Review, the same way the certificate wizard's Finish does.
 */
class ConversionMenuContributionTest
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
	void contributesExactlyOneMenuItemReplacingTheTwoFormerOnes()
	{
		ConversionMenuContribution contribution = new ConversionMenuContribution();

		java.util.List<JMenuItem> menuItems = contribution.getMenuItems();

		assertEquals(1, menuItems.size());
		assertEquals("Convert Key/Certificate...", menuItems.get(0).getText());
		assertEquals("Conversion", contribution.getMenuName());
	}

	@Test
	void requireSourceFileRejectsABlankPath()
	{
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
			() -> ConversionMenuContribution.requireSourceFile(" "));

		assertTrue(exception.getMessage().contains("choose a file"), exception.getMessage());
	}

	@Test
	void requireSourceFileAcceptsAPath()
	{
		File source = ConversionMenuContribution.requireSourceFile("/tmp/key.pem");

		assertEquals(new File("/tmp/key.pem"), source);
	}

	@Test
	void requireOperationRejectsNull()
	{
		IllegalStateException exception = assertThrows(IllegalStateException.class,
			() -> ConversionMenuContribution.requireOperation(null));

		assertTrue(exception.getMessage().contains("choose a conversion"), exception.getMessage());
	}

	@Test
	void resolveTargetFileUsesWhatWasTyped()
	{
		File source = new File("/tmp/key.pem");

		File target = ConversionMenuContribution.resolveTargetFile(source, "/tmp/chosen.der",
			ConversionOperation.PEM_TO_DER);

		assertEquals(new File("/tmp/chosen.der"), target);
	}

	@Test
	void resolveTargetFileFallsBackToTheOperationsDefaultWhenNothingWasTyped()
	{
		File source = new File("/tmp/key.pem");

		File target = ConversionMenuContribution.resolveTargetFile(source, "",
			ConversionOperation.PEM_TO_DER);

		assertEquals(ConversionOperation.PEM_TO_DER.defaultTargetFile(source), target);
	}

	@Test
	void buildSummaryNamesEveryFieldOfWhatFinishWouldDo(@TempDir File directory) throws Exception
	{
		File source = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(), source);
		ConversionWizardModel model = new ConversionWizardModel();
		model.setSourceFilePath(source.getAbsolutePath());
		model.setWhatItHolds(ConversionSupport.kindOf(source).description());
		model.setOperation(ConversionOperation.PEM_TO_DER);

		String summary = ConversionMenuContribution.buildSummary(model);

		assertTrue(summary.contains(source.getAbsolutePath()), summary);
		assertTrue(summary.contains("PKCS#1"), summary);
		assertTrue(summary.contains("to DER"), summary);
		assertTrue(summary.contains(ConversionOperation.PEM_TO_DER.defaultTargetFile(source)
			.getAbsolutePath()), summary);
	}
}
