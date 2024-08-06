package io.github.astrapi69.mystic.crypt.wizard.state;

import io.github.astrapi69.design.pattern.state.wizard.BaseWizardState;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;

public enum CertificateWizardState
	implements BaseWizardState<BaseWizardStateMachineModel<CertificateInfoModel>>
{
	ISSUER {

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(BaseWizardStateMachineModel<CertificateInfoModel> stateMachine)
		{
			stateMachine.setCurrentState(SUBJECT);
		}

		@Override
		public void goPrevious(BaseWizardStateMachineModel<CertificateInfoModel> stateMachine)
		{
			// No previous state
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
		public void cancel(
			BaseWizardStateMachineModel<CertificateInfoModel> x509CertificateV3InfoBaseWizardStateMachineModel)
		{

		}

		@Override
		public void finish(
			BaseWizardStateMachineModel<CertificateInfoModel> x509CertificateV3InfoBaseWizardStateMachineModel)
		{

		}
	},
	SUBJECT {

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(BaseWizardStateMachineModel<CertificateInfoModel> stateMachine)
		{
			stateMachine.setCurrentState(DATES);
		}

		@Override
		public void goPrevious(BaseWizardStateMachineModel<CertificateInfoModel> stateMachine)
		{
			stateMachine.setCurrentState(ISSUER);
		}

		@Override
		public void cancel(
			BaseWizardStateMachineModel<CertificateInfoModel> x509CertificateV3InfoBaseWizardStateMachineModel)
		{

		}

		@Override
		public void finish(
			BaseWizardStateMachineModel<CertificateInfoModel> x509CertificateV3InfoBaseWizardStateMachineModel)
		{

		}

	},
	DATES {

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(BaseWizardStateMachineModel<CertificateInfoModel> stateMachine)
		{
			stateMachine.setCurrentState(EXTENSIONS);
		}

		@Override
		public void goPrevious(BaseWizardStateMachineModel<CertificateInfoModel> stateMachine)
		{
			stateMachine.setCurrentState(SUBJECT);
		}

		@Override
		public void cancel(
			BaseWizardStateMachineModel<CertificateInfoModel> x509CertificateV3InfoBaseWizardStateMachineModel)
		{

		}

		@Override
		public void finish(
			BaseWizardStateMachineModel<CertificateInfoModel> x509CertificateV3InfoBaseWizardStateMachineModel)
		{

		}
	},
	EXTENSIONS {

		@Override
		public String getName()
		{
			return name();
		}

		@Override
		public void goNext(BaseWizardStateMachineModel<CertificateInfoModel> stateMachine)
		{
			// No next state
		}

		@Override
		public void goPrevious(BaseWizardStateMachineModel<CertificateInfoModel> stateMachine)
		{
			stateMachine.setCurrentState(DATES);
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
		public void cancel(
			BaseWizardStateMachineModel<CertificateInfoModel> x509CertificateV3InfoBaseWizardStateMachineModel)
		{

		}

		@Override
		public void finish(
			BaseWizardStateMachineModel<CertificateInfoModel> x509CertificateV3InfoBaseWizardStateMachineModel)
		{

		}
	}
}
