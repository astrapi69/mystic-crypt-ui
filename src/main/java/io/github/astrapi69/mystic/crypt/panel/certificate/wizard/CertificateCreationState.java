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
