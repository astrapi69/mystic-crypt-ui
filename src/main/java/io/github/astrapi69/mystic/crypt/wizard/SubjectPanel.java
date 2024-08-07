package io.github.astrapi69.mystic.crypt.wizard;

import javax.swing.JLabel;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.panel.certificate.NewCertificateAttributesPanel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.swing.base.BasePanel;
import net.miginfocom.swing.MigLayout;

public class SubjectPanel extends BasePanel<BaseWizardStateMachineModel<CertificateInfoModel>>
{

	private JLabel lblHeader;
	private NewCertificateAttributesPanel newCertificateAttributesPanel;

	public SubjectPanel(IModel<BaseWizardStateMachineModel<CertificateInfoModel>> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();
		lblHeader = new JLabel("Subject");
		newCertificateAttributesPanel = new NewCertificateAttributesPanel(
			BaseModel.of(getModelObject().getModelObject().getSubject()));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		onInitializeMigLayout();
	}

	protected void onInitializeMigLayout()
	{
		MigLayout migLayout = new MigLayout("wrap 1", "[grow, fill]", "[][][grow]");

		setLayout(migLayout);
		add(lblHeader, "cell 0 0, alignx center, gapbottom 10");
		add(newCertificateAttributesPanel, "cell 0 1, grow");

	}

}
