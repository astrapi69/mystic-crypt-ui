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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.crypt.api.key.PemType;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionSupport;

/**
 * Tests of the conversion wizard's state machine: which step follows which, and the two places a
 * step refuses to advance until its own step is actually complete - no file detected yet on Source,
 * no conversion chosen yet on Target. Built directly on {@link BaseWizardStateMachineModel} without
 * any Swing component, the same way {@code CertificateWizardState}'s own step-gating (its Dates step
 * only advancing for a v3 certificate) is provable without a display.
 */
class ConversionWizardStateTest
{

	private static BaseWizardStateMachineModel<ConversionWizardModel> machineAt(
		ConversionWizardState state, ConversionWizardModel model)
	{
		return BaseWizardStateMachineModel.<ConversionWizardModel> builder().currentState(state)
			.modelObject(model).build();
	}

	@Test
	void everyStateNameMatchesItsCardLayoutName()
	{
		assertEquals("SOURCE", ConversionWizardState.SOURCE.getName());
		assertEquals("TARGET", ConversionWizardState.TARGET.getName());
		assertEquals("REVIEW", ConversionWizardState.REVIEW.getName());
	}

	@Test
	@DisplayName("Source is first, has no previous step, and does not advance without a detected file")
	void sourceStaysOnItselfWithoutADetectedFile()
	{
		assertTrue(ConversionWizardState.SOURCE.isFirst());
		assertFalse(ConversionWizardState.SOURCE.hasPrevious());
		ConversionWizardModel model = new ConversionWizardModel();
		BaseWizardStateMachineModel<ConversionWizardModel> stateMachine = machineAt(
			ConversionWizardState.SOURCE, model);

		stateMachine.next();

		assertEquals(ConversionWizardState.SOURCE, stateMachine.getCurrentState(),
			"clicking Next with nothing detected yet must not advance the wizard");
	}

	@Test
	@DisplayName("Source advances to Target once a file kind was detected")
	void sourceAdvancesToTargetOnceAFileKindIsDetected()
	{
		ConversionWizardModel model = new ConversionWizardModel();
		model.setFileKind(new ConversionSupport.FileKind(true, PemType.PRIVATE_KEY, "a private key"));
		BaseWizardStateMachineModel<ConversionWizardModel> stateMachine = machineAt(
			ConversionWizardState.SOURCE, model);

		stateMachine.next();

		assertEquals(ConversionWizardState.TARGET, stateMachine.getCurrentState());
	}

	@Test
	@DisplayName("Target does not advance to Review without a chosen conversion")
	void targetStaysOnItselfWithoutAChosenOperation()
	{
		ConversionWizardModel model = new ConversionWizardModel();
		BaseWizardStateMachineModel<ConversionWizardModel> stateMachine = machineAt(
			ConversionWizardState.TARGET, model);

		stateMachine.next();

		assertEquals(ConversionWizardState.TARGET, stateMachine.getCurrentState());
	}

	@Test
	@DisplayName("Target advances to Review once a conversion was chosen")
	void targetAdvancesToReviewOnceAnOperationIsChosen()
	{
		ConversionWizardModel model = new ConversionWizardModel();
		model.setOperation(ConversionOperation.PEM_TO_DER);
		BaseWizardStateMachineModel<ConversionWizardModel> stateMachine = machineAt(
			ConversionWizardState.TARGET, model);

		stateMachine.next();

		assertEquals(ConversionWizardState.REVIEW, stateMachine.getCurrentState());
	}

	@Test
	void targetGoesBackToSource()
	{
		BaseWizardStateMachineModel<ConversionWizardModel> stateMachine = machineAt(
			ConversionWizardState.TARGET, new ConversionWizardModel());

		stateMachine.previous();

		assertEquals(ConversionWizardState.SOURCE, stateMachine.getCurrentState());
	}

	@Test
	@DisplayName("Review is last, has no next step, and going back returns to Target")
	void reviewIsLastAndGoesBackToTarget()
	{
		assertTrue(ConversionWizardState.REVIEW.isLast());
		assertFalse(ConversionWizardState.REVIEW.hasNext());
		BaseWizardStateMachineModel<ConversionWizardModel> stateMachine = machineAt(
			ConversionWizardState.REVIEW, new ConversionWizardModel());

		stateMachine.previous();

		assertEquals(ConversionWizardState.TARGET, stateMachine.getCurrentState());
	}

	@Test
	void targetIsNeitherFirstNorLastAndHasBothNeighbours()
	{
		assertFalse(ConversionWizardState.TARGET.isFirst());
		assertFalse(ConversionWizardState.TARGET.isLast());
		assertTrue(ConversionWizardState.TARGET.hasPrevious());
		assertTrue(ConversionWizardState.TARGET.hasNext());
	}
}
