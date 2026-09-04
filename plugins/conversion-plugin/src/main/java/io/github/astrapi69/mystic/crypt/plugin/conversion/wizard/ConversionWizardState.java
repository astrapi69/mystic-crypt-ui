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

import io.github.astrapi69.design.pattern.state.wizard.BaseWizardState;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.design.pattern.state.wizard.model.WizardStateInfo;

/**
 * The three steps of the conversion wizard, in the order the user walks them: {@code SOURCE} (pick
 * the file, detect what it holds), {@code TARGET} (pick the conversion and the destination) and
 * {@code REVIEW} (summary and Finish). Mirrors the host certificate wizard's
 * {@code CertificateWizardState} exactly - same state machine library, same per-constant shape.
 */
public enum ConversionWizardState
	implements
		BaseWizardState<BaseWizardStateMachineModel<ConversionWizardModel>>
{

	SOURCE
	{

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{
			if (stateMachine.getModelObject().getFileKind() != null)
			{
				stateMachine.setCurrentState(TARGET);
			}
		}

		@Override
		public void goPrevious(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{
			// no previous state
		}

		@Override
		public boolean hasPrevious()
		{
			return false;
		}

		@Override
		public boolean isFirst()
		{
			return true;
		}

		@Override
		public void cancel(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{

		}

		@Override
		public void finish(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{

		}

		@Override
		public WizardStateInfo getWizardStateInfo()
		{
			return null;
		}

		@Override
		public void setWizardStateInfo(WizardStateInfo wizardStateInfo)
		{

		}

	},
	TARGET
	{

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{
			if (stateMachine.getModelObject().getOperation() != null)
			{
				stateMachine.setCurrentState(REVIEW);
			}
		}

		@Override
		public void goPrevious(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{
			stateMachine.setCurrentState(SOURCE);
		}

		@Override
		public void cancel(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{

		}

		@Override
		public void finish(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{

		}

		@Override
		public WizardStateInfo getWizardStateInfo()
		{
			return null;
		}

		@Override
		public void setWizardStateInfo(WizardStateInfo wizardStateInfo)
		{

		}

	},
	REVIEW
	{

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{
			// no next state
		}

		@Override
		public void goPrevious(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{
			stateMachine.setCurrentState(TARGET);
		}

		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public boolean isLast()
		{
			return true;
		}

		@Override
		public void cancel(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{

		}

		@Override
		public void finish(BaseWizardStateMachineModel<ConversionWizardModel> stateMachine)
		{

		}

		@Override
		public WizardStateInfo getWizardStateInfo()
		{
			return null;
		}

		@Override
		public void setWizardStateInfo(WizardStateInfo wizardStateInfo)
		{

		}

	}
}
