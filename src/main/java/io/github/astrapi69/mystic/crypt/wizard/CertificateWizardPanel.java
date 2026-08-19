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
package io.github.astrapi69.mystic.crypt.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import io.github.astrapi69.design.pattern.observer.event.EventListener;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.design.pattern.state.wizard.model.NavigationEventState;
import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.state.CertificateWizardState;
import io.github.astrapi69.swing.wizard.AbstractWizardPanel;
import io.github.astrapi69.swing.wizard.BaseWizardContentPanel;
import io.github.astrapi69.swing.wizard.NavigationPanel;
import lombok.Getter;

public class CertificateWizardPanel extends AbstractWizardPanel<CertificateInfoModel>
	implements
		EventListener<EventObject<NavigationEventState>>
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
	public void onEvent(final EventObject<NavigationEventState> event)
	{
		final NavigationEventState navigationState = event.getSource();
		if (NavigationEventState.UPDATE.equals(navigationState))
		{
			System.out.println(navigationState);
			updateButtonState();
		}
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
