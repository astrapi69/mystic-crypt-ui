package io.github.astrapi69.mystic.crypt.wizard;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.swing.wizard.BaseWizardContentPanel;

public class CertificateWizardContentPanel extends BaseWizardContentPanel<CertificateInfoModel>
{

	private static final long serialVersionUID = 1L;

	public CertificateWizardContentPanel(
		IModel<BaseWizardStateMachineModel<CertificateInfoModel>> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		IModel<BaseWizardStateMachineModel<CertificateInfoModel>> model = getModel();
		add(new IssuerPanel(model), "ISSUER");
		add(new SubjectPanel(model), "SUBJECT");
		add(new DatesPanel(model), "DATES");
		add(new ExtensionsPanel(model), "EXTENSIONS");
	}
}
