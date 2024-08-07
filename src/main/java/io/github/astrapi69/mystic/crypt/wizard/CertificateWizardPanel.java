package io.github.astrapi69.mystic.crypt.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.state.CertificateWizardState;
import io.github.astrapi69.swing.wizard.AbstractWizardPanel;
import io.github.astrapi69.swing.wizard.BaseWizardContentPanel;
import io.github.astrapi69.swing.wizard.NavigationPanel;
import lombok.Getter;

public class CertificateWizardPanel extends AbstractWizardPanel<CertificateInfoModel>
{

	private static final long serialVersionUID = 1L;
	private NavigationPanel<Void> navigationPanel;

	@Getter
	private CertificateWizardContentPanel wizardContentPanel;

	public CertificateWizardPanel(IModel<CertificateInfoModel> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		BaseWizardStateMachineModel<CertificateInfoModel> stateMachineModel = BaseWizardStateMachineModel
			.<CertificateInfoModel> builder().currentState(CertificateWizardState.ISSUER)
			.modelObject(getModelObject()).build();
		IModel<BaseWizardStateMachineModel<CertificateInfoModel>> machineModelIModel = BaseModel
			.of(stateMachineModel);
		setStateMachine(stateMachineModel);
		super.onInitializeComponents();
		wizardContentPanel = new CertificateWizardContentPanel(machineModelIModel);
		navigationPanel = newNavigationPanel();
	}


	@Override
	protected BaseWizardContentPanel<CertificateInfoModel> newWizardContentPanel(
		IModel<BaseWizardStateMachineModel<CertificateInfoModel>> model)
	{
		return new CertificateWizardContentPanel(model);
	}

	protected NavigationPanel<Void> newNavigationPanel()
	{
		final NavigationPanel<Void> navigationPanel = new NavigationPanel<Void>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onCancel()
			{
				CertificateWizardPanel.this.onCancel();
			}

			@Override
			protected void onFinish()
			{
				CertificateWizardPanel.this.onFinish();
			}

			@Override
			protected void onNext()
			{
				CertificateWizardPanel.this.onNext();
			}

			@Override
			protected void onPrevious()
			{
				CertificateWizardPanel.this.onPrevious();
			}
		};
		return navigationPanel;
	}

	@Override
	protected void onAfterInitializeComponents()
	{
		super.onAfterInitializeComponents();
		updateButtonState();
	}


	protected void onCancel()
	{
		getStateMachine().cancel();
		// from here application specific behavior...
	}

	protected void onFinish()
	{
		getStateMachine().finish();
		// from here application specific behavior...
		// TODO after insert to application remove System.exit !!!
		System.exit(0);
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		setLayout(new BorderLayout());
		add(wizardContentPanel, BorderLayout.CENTER);
		add(navigationPanel, BorderLayout.SOUTH);
	}

	protected void onNext()
	{
		BaseWizardStateMachineModel<CertificateInfoModel> stateMachine = getStateMachine();
		stateMachine.next();
		updateButtonState();
		final String name = stateMachine.getCurrentState().getName();
		final CardLayout cardLayout = wizardContentPanel.getCardLayout();
		cardLayout.show(wizardContentPanel, name);
	}

	protected void onPrevious()
	{
		getStateMachine().previous();
		updateButtonState();
		final String name = getStateMachine().getCurrentState().getName();
		final CardLayout cardLayout = wizardContentPanel.getCardLayout();
		cardLayout.show(wizardContentPanel, name);
	}

	protected void updateButtonState()
	{
		if (getStateMachine() != null)
		{
			navigationPanel.getBtnPrevious()
				.setEnabled(getStateMachine().getCurrentState().hasPrevious());
			navigationPanel.getBtnNext().setEnabled(getStateMachine().getCurrentState().hasNext());
		}
	}

}
