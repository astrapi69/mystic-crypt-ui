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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.model.BaseModel;

/**
 * Tests of the assembled wizard: {@link CreateKeyStoreWizardPanel} wired to its
 * {@link CreateKeyStoreWizardContentPanel}, walked through Next/Previous the same way the navigation
 * panel's buttons drive it - proof that the state machine, the card layout and the step panels are
 * actually wired together, not just individually correct.
 */
class CreateKeyStoreWizardPanelTest
{

	@Test
	void startsOnStoreWithPreviousDisabledAndNextEnabled()
	{
		CreateKeyStoreWizardPanel wizardPanel = new CreateKeyStoreWizardPanel(
			BaseModel.of(new CreateKeyStoreWizardModel()));

		assertEquals(CreateKeyStoreWizardState.STORE, wizardPanel.getStateMachine().getCurrentState());
		assertFalse(wizardPanel.getNavigationPanel().getBtnPrevious().isEnabled());
		assertTrue(wizardPanel.getNavigationPanel().getBtnNext().isEnabled());
	}

	@Test
	void clickingNextWithoutACompletedStoreStepStaysOnStore()
	{
		CreateKeyStoreWizardPanel wizardPanel = new CreateKeyStoreWizardPanel(
			BaseModel.of(new CreateKeyStoreWizardModel()));

		wizardPanel.getNavigationPanel().getBtnNext().doClick();

		assertEquals(CreateKeyStoreWizardState.STORE, wizardPanel.getStateMachine().getCurrentState());
	}

	@Test
	void walkingForwardAndBackReachesEveryStepAndReturns()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		CreateKeyStoreWizardPanel wizardPanel = new CreateKeyStoreWizardPanel(BaseModel.of(model));
		CreateKeyStoreWizardContentPanel contentPanel = (CreateKeyStoreWizardContentPanel)wizardPanel
			.getWizardContentPanel();
		assertNotNull(contentPanel.getStorePanel());

		model.setKeyStoreFilePath("/tmp/new.p12");
		model.setStorePassword("secret".toCharArray());
		model.setStorePasswordRepeated("secret".toCharArray());

		wizardPanel.getNavigationPanel().getBtnNext().doClick();
		assertEquals(CreateKeyStoreWizardState.ENTRY, wizardPanel.getStateMachine().getCurrentState());

		// Entry advances without any of its own fields as long as no key pair was requested
		wizardPanel.getNavigationPanel().getBtnNext().doClick();
		assertEquals(CreateKeyStoreWizardState.REVIEW,
			wizardPanel.getStateMachine().getCurrentState());
		assertFalse(wizardPanel.getNavigationPanel().getBtnNext().isEnabled(),
			"Review is the last step");

		wizardPanel.getNavigationPanel().getBtnPrevious().doClick();
		assertEquals(CreateKeyStoreWizardState.ENTRY, wizardPanel.getStateMachine().getCurrentState());
		wizardPanel.getNavigationPanel().getBtnPrevious().doClick();
		assertEquals(CreateKeyStoreWizardState.STORE, wizardPanel.getStateMachine().getCurrentState());
		assertFalse(wizardPanel.getNavigationPanel().getBtnPrevious().isEnabled(),
			"Store is the first step");
	}

	@Test
	void entryDoesNotAdvanceWhenAKeyPairIsRequestedButIncomplete()
	{
		CreateKeyStoreWizardModel model = new CreateKeyStoreWizardModel();
		model.setKeyStoreFilePath("/tmp/new.p12");
		model.setStorePassword("secret".toCharArray());
		model.setStorePasswordRepeated("secret".toCharArray());
		model.setAddKeyPairNow(true);
		CreateKeyStoreWizardPanel wizardPanel = new CreateKeyStoreWizardPanel(BaseModel.of(model));
		wizardPanel.getNavigationPanel().getBtnNext().doClick();
		assertEquals(CreateKeyStoreWizardState.ENTRY, wizardPanel.getStateMachine().getCurrentState());

		wizardPanel.getNavigationPanel().getBtnNext().doClick();

		assertEquals(CreateKeyStoreWizardState.ENTRY, wizardPanel.getStateMachine().getCurrentState());
	}
}
