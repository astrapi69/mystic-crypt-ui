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
package io.github.astrapi69.mystic.crypt.panel.certificate.wizard;

import io.github.astrapi69.design.pattern.state.wizard.BaseWizardState;
import io.github.astrapi69.design.pattern.state.wizard.BaseWizardStateMachine;
import io.github.astrapi69.design.pattern.state.wizard.model.WizardStateInfo;

public enum CertificateCreationState implements BaseWizardState<BaseWizardStateMachine>
{

	/** The cancel {@link CertificateCreationState} object. */
	CANCELED {
		@Override
		public void cancel(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.CANCELED);
		}

		@Override
		public void finish(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.FINISHED);
		}

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(final BaseWizardStateMachine stateMachine)
		{
		}

		@Override
		public void goPrevious(final BaseWizardStateMachine stateMachine)
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

	/** The finish {@link CertificateCreationState} object. */
	FINISHED {
		@Override
		public void cancel(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.CANCELED);
		}

		@Override
		public void finish(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.FINISHED);
		}

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(final BaseWizardStateMachine stateMachine)
		{
		}

		@Override
		public void goPrevious(final BaseWizardStateMachine stateMachine)
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

	/** The first {@link CertificateCreationState} object. */
	FIRST {
		@Override
		public void cancel(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.CANCELED);
		}

		@Override
		public void finish(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.FINISHED);
		}

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.SECOND);
		}

		@Override
		public void goPrevious(final BaseWizardStateMachine input)
		{
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
		public WizardStateInfo getWizardStateInfo()
		{
			return null;
		}

		@Override
		public void setWizardStateInfo(WizardStateInfo wizardStateInfo)
		{

		}


	},

	/** The second {@link CertificateCreationState} object. */
	SECOND {
		@Override
		public void cancel(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.CANCELED);
		}

		@Override
		public void finish(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.FINISHED);
		}

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.THIRD);
		}

		@Override
		public void goPrevious(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.FIRST);
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

	/** The third {@link CertificateCreationState} object. */
	THIRD {
		@Override
		public void cancel(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.CANCELED);
		}

		@Override
		public void finish(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(CertificateCreationState.FINISHED);
		}

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(final BaseWizardStateMachine stateMachine)
		{
		}

		@Override
		public void goPrevious(final BaseWizardStateMachine stateMachine)
		{
			stateMachine.setCurrentState(SECOND);
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
