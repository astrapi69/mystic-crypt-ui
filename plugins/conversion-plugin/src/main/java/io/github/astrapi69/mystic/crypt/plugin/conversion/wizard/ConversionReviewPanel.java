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
package io.github.astrapi69.mystic.crypt.plugin.conversion.wizard;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionMessages;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMTextArea;
import net.miginfocom.swing.MigLayout;

/**
 * The wizard's last step: a read-only summary of what Finish would do - source path, what it holds,
 * the chosen conversion and the resolved destination. Unlike the certificate wizard's review step,
 * this one needs no editable fields of its own: the wizard model already carries the source path,
 * the target path and the operation directly, so Finish reads the model, not this panel.
 * <p>
 * Whoever opens the wizard fills the summary in through {@link #refresh(String)} once the state
 * machine reaches this step - the same split {@code ReviewPanel} uses in the certificate wizard.
 */
public class ConversionReviewPanel
	extends
		BasePanel<BaseWizardStateMachineModel<ConversionWizardModel>>
{

	private static final long serialVersionUID = 1L;

	private JLabel lblHeader;
	private JMTextArea txtSummary;
	private JScrollPane scrSummary;

	public ConversionReviewPanel(IModel<BaseWizardStateMachineModel<ConversionWizardModel>> model)
	{
		super(model);
	}

	@Override
	protected void onInitializeComponents()
	{
		super.onInitializeComponents();

		lblHeader = new JLabel("Review");
		txtSummary = new JMTextArea(10, 60);
		txtSummary.setName("txtSummary");
		txtSummary.setEditable(false);
		txtSummary.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
		scrSummary = new JScrollPane(txtSummary);

		txtSummary.setToolTipText(ConversionMessages.getString("conversion.wizard.review.tooltip.summary",
			"a read-only summary of what Finish would do"));
	}

	@Override
	protected void onInitializeLayout()
	{
		super.onInitializeLayout();
		setLayout(new MigLayout("wrap 1", "[grow,fill]", "[][grow]"));
		add(lblHeader, "align center, gapbottom 10");
		add(scrSummary, "grow");
	}

	/**
	 * Regenerates the read-only summary
	 *
	 * @param summary
	 *            the summary text to show
	 */
	public void refresh(final String summary)
	{
		txtSummary.setText(summary);
		txtSummary.setCaretPosition(0);
	}
}
