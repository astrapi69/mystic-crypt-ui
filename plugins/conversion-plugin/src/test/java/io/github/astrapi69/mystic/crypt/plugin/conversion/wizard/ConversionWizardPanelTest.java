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
package io.github.astrapi69.mystic.crypt.plugin.conversion.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.writer.PrivateKeyWriter;
import io.github.astrapi69.model.BaseModel;

/**
 * Tests of the assembled wizard: {@link ConversionWizardPanel} wired to its
 * {@link ConversionWizardContentPanel}, walked through Next/Previous the same way the navigation
 * panel's buttons drive it - proof that the state machine, the card layout and the step panels are
 * actually wired together, not just individually correct.
 */
class ConversionWizardPanelTest
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
	void startsOnSourceWithPreviousDisabledAndNextEnabled()
	{
		ConversionWizardPanel wizardPanel = new ConversionWizardPanel(
			BaseModel.of(new ConversionWizardModel()));

		assertEquals(ConversionWizardState.SOURCE, wizardPanel.getStateMachine().getCurrentState());
		assertFalse(wizardPanel.getNavigationPanel().getBtnPrevious().isEnabled());
		assertTrue(wizardPanel.getNavigationPanel().getBtnNext().isEnabled());
	}

	@Test
	void clickingNextWithoutADetectedFileStaysOnSource()
	{
		ConversionWizardPanel wizardPanel = new ConversionWizardPanel(
			BaseModel.of(new ConversionWizardModel()));

		wizardPanel.getNavigationPanel().getBtnNext().doClick();

		assertEquals(ConversionWizardState.SOURCE, wizardPanel.getStateMachine().getCurrentState());
	}

	@Test
	void walkingForwardAndBackReachesEveryStepAndReturns(@TempDir File directory) throws Exception
	{
		File source = new File(directory, "key.pem");
		PrivateKeyWriter.writeInPemFormat(KeyPairFactory.newKeyPair("RSA").getPrivate(), source);
		ConversionWizardModel model = new ConversionWizardModel();
		ConversionWizardPanel wizardPanel = new ConversionWizardPanel(BaseModel.of(model));
		ConversionWizardContentPanel contentPanel = (ConversionWizardContentPanel)wizardPanel
			.getWizardContentPanel();
		assertNotNull(contentPanel.getSourcePanel());

		// Source: detect the file directly through the model, the way SourcePanel's own document
		// listener would once the user types a path
		model.setSourceFilePath(source.getAbsolutePath());
		model.setFileKind(io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionSupport
			.kindOf(source));

		wizardPanel.getNavigationPanel().getBtnNext().doClick();
		assertEquals(ConversionWizardState.TARGET, wizardPanel.getStateMachine().getCurrentState());

		// Target does not advance without a chosen operation
		wizardPanel.getNavigationPanel().getBtnNext().doClick();
		assertEquals(ConversionWizardState.TARGET, wizardPanel.getStateMachine().getCurrentState());

		model.setOperation(ConversionOperation.PEM_TO_DER);
		wizardPanel.getNavigationPanel().getBtnNext().doClick();
		assertEquals(ConversionWizardState.REVIEW, wizardPanel.getStateMachine().getCurrentState());
		assertFalse(wizardPanel.getNavigationPanel().getBtnNext().isEnabled(),
			"Review is the last step");

		wizardPanel.getNavigationPanel().getBtnPrevious().doClick();
		assertEquals(ConversionWizardState.TARGET, wizardPanel.getStateMachine().getCurrentState());
		wizardPanel.getNavigationPanel().getBtnPrevious().doClick();
		assertEquals(ConversionWizardState.SOURCE, wizardPanel.getStateMachine().getCurrentState());
		assertFalse(wizardPanel.getNavigationPanel().getBtnPrevious().isEnabled(),
			"Source is the first step");
	}
}
