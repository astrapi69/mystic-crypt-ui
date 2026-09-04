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

import java.util.Arrays;

import io.github.astrapi69.design.pattern.state.wizard.BaseWizardState;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.design.pattern.state.wizard.model.WizardStateInfo;

/**
 * The three steps of the "Create Key Store..." wizard, in the order the user walks them:
 * {@code STORE} (file location, type, password), {@code ENTRY} (an optional first key pair) and
 * {@code REVIEW} (summary and Finish). Mirrors the conversion plugin's own
 * {@code ConversionWizardState} exactly - same state machine library, same per-constant shape.
 * <p>
 * Every constant overrides {@link #getName()} to its own {@link Enum#name()}: the wizard's card
 * layout looks a step up by that name, and the default from {@link WizardStateInfo} would otherwise
 * be {@code null} since none of these states carry one.
 */
public enum CreateKeyStoreWizardState
	implements
		BaseWizardState<BaseWizardStateMachineModel<CreateKeyStoreWizardModel>>
{

	STORE
	{

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
		{
			if (isComplete(stateMachine.getModelObject()))
			{
				stateMachine.setCurrentState(ENTRY);
			}
		}

		/**
		 * The Store step is complete once a file path was typed and the two password fields are
		 * both filled in and agree with each other - a typo in a brand-new store's password that
		 * nothing catches locks the store immediately
		 */
		private boolean isComplete(CreateKeyStoreWizardModel model)
		{
			return model.getKeyStoreFilePath() != null && !model.getKeyStoreFilePath().isBlank()
				&& model.getStorePassword().length > 0 && model.getStorePasswordRepeated().length > 0
				&& Arrays.equals(model.getStorePassword(), model.getStorePasswordRepeated());
		}

		@Override
		public void goPrevious(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
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
		public void cancel(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
		{

		}

		@Override
		public void finish(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
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
	ENTRY
	{

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
		{
			CreateKeyStoreWizardModel model = stateMachine.getModelObject();
			if (!model.isAddKeyPairNow() || isComplete(model))
			{
				stateMachine.setCurrentState(REVIEW);
			}
		}

		/**
		 * When a key pair was asked for, both the alias and the certificate subject have to be
		 * filled in before there is anything for Finish to generate
		 */
		private boolean isComplete(CreateKeyStoreWizardModel model)
		{
			return model.getAlias() != null && !model.getAlias().isBlank()
				&& model.getDistinguishedName() != null && !model.getDistinguishedName().isBlank();
		}

		@Override
		public void goPrevious(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
		{
			stateMachine.setCurrentState(STORE);
		}

		@Override
		public void cancel(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
		{

		}

		@Override
		public void finish(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
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
		public void goNext(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
		{
			// no next state
		}

		@Override
		public void goPrevious(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
		{
			stateMachine.setCurrentState(ENTRY);
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
		public void cancel(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
		{

		}

		@Override
		public void finish(BaseWizardStateMachineModel<CreateKeyStoreWizardModel> stateMachine)
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
