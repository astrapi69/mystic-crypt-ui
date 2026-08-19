package io.github.astrapi69.mystic.crypt.panel.certificate.wizard;

import io.github.astrapi69.swing.base.BaseCardLayoutPanel;

public class CertificateWizardContentPanel extends BaseCardLayoutPanel<Object>
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Initializer block.
	 */
	{
	}

	/**
	 * Instantiates a new wizard content panel.
	 */
	public CertificateWizardContentPanel()
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onInitializeComponents()
	{
		add(new FirstStepPanel(), CertificateCreationState.FIRST.getName());
		add(new SecondStepPanel(), CertificateCreationState.SECOND.getName());
		add(new ThirdStepPanel(), CertificateCreationState.THIRD.getName());

	}

}
