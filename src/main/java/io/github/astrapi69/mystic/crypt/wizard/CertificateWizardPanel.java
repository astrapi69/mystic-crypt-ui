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

import io.github.astrapi69.design.pattern.observer.event.EventListener;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.design.pattern.state.wizard.model.NavigationEventState;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.mystic.crypt.wizard.state.CertificateWizardState;
import io.github.astrapi69.swing.wizard.AbstractWizardPanel;
import io.github.astrapi69.swing.wizard.BaseWizardContentPanel;

public class CertificateWizardPanel extends AbstractWizardPanel<CertificateInfoModel>
	implements
		EventListener<EventObject<NavigationEventState>>
{

	private static final long serialVersionUID = 1L;

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
			updateButtonState();
		}
	}

	@Override
	protected void onInitializeComponents()
	{
		BaseWizardStateMachineModel<CertificateInfoModel> stateMachineModel = BaseWizardStateMachineModel
			.<CertificateInfoModel> builder().currentState(CertificateWizardState.ISSUER)
			.modelObject(getModelObject()).build();
		setStateMachine(stateMachineModel);
		super.onInitializeComponents();
	}

	@Override
	protected BaseWizardContentPanel<CertificateInfoModel> newWizardContentPanel(
		IModel<BaseWizardStateMachineModel<CertificateInfoModel>> model)
	{
		return new CertificateWizardContentPanel(model);
	}

	/**
	 * The size this wizard needs to show any of its steps without scrolling, so the window is
	 * opened once at a size that fits all of them
	 *
	 * @return the size, navigation included
	 */
	public java.awt.Dimension preferredSizeForEveryStep()
	{
		java.awt.Dimension content = getWizardContentPanel()instanceof CertificateWizardContentPanel certificateContent
			? certificateContent.preferredSizeForEveryStep()
			: getWizardContentPanel().getPreferredSize();
		java.awt.Dimension navigation = getNavigationPanel().getPreferredSize();
		return new java.awt.Dimension(Math.max(content.width, navigation.width),
			content.height + navigation.height);
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
		// application-specific behavior is provided by whoever opens the wizard (it overrides this
		// method to generate/save the certificate and close its dialog)
	}

}
