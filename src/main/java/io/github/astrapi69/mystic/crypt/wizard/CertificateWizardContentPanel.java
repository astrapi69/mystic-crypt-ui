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

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.wizard.model.CertificateInfoModel;
import io.github.astrapi69.swing.wizard.BaseWizardContentPanel;

public class CertificateWizardContentPanel extends BaseWizardContentPanel<CertificateInfoModel>
{

	private static final long serialVersionUID = 1L;

	private ReviewPanel reviewPanel;

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
		reviewPanel = new ReviewPanel(model);
		add(reviewPanel, "REVIEW");
	}

	/**
	 * The review step, so whoever opens the wizard can push a preview and defaults into it once the
	 * user reaches it
	 *
	 * @return the review step's panel
	 */
	public ReviewPanel getReviewPanel()
	{
		return reviewPanel;
	}

	/**
	 * The size the step that is currently on screen asks for.
	 * <p>
	 * The steps sit in a card layout, which asks for the size of the largest of them whatever is
	 * being shown. In a scroll pane that produces a scroll bar on a short step, and scrolling it
	 * reveals nothing but the empty space another step would have needed.
	 *
	 * @return what the visible step asks for, insets included
	 */
	@Override
	public java.awt.Dimension getPreferredSize()
	{
		for (java.awt.Component step : getComponents())
		{
			if (step.isVisible())
			{
				java.awt.Dimension wanted = step.getPreferredSize();
				java.awt.Insets insets = getInsets();
				return new java.awt.Dimension(wanted.width + insets.left + insets.right,
					wanted.height + insets.top + insets.bottom);
			}
		}
		return super.getPreferredSize();
	}

	/**
	 * The size that holds every step, so a window can be opened once at a size that fits all of
	 * them and does not resize itself while the user walks through
	 *
	 * @return the size of the largest step in each direction, insets included
	 */
	public java.awt.Dimension preferredSizeForEveryStep()
	{
		java.awt.Insets insets = getInsets();
		int width = 0;
		int height = 0;
		for (java.awt.Component step : getComponents())
		{
			java.awt.Dimension wanted = step.getPreferredSize();
			width = Math.max(width, wanted.width);
			height = Math.max(height, wanted.height);
		}
		return new java.awt.Dimension(width + insets.left + insets.right,
			height + insets.top + insets.bottom);
	}

}
