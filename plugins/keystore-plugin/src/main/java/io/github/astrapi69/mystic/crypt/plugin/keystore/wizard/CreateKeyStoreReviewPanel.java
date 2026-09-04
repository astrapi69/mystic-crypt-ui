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
package io.github.astrapi69.mystic.crypt.plugin.keystore.wizard;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import io.github.astrapi69.design.pattern.state.wizard.model.BaseWizardStateMachineModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.mystic.crypt.plugin.keystore.KeyStoreMessages;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMTextArea;
import net.miginfocom.swing.MigLayout;

/**
 * The wizard's last step: a read-only summary of what Finish would do - the file, its type and,
 * when one was configured, the first key pair's alias, subject and algorithm.
 * <p>
 * Named {@code CreateKeyStoreReviewPanel} rather than plain {@code ReviewPanel} to stay unambiguous
 * next to the host's own {@code io.github.astrapi69.mystic.crypt.wizard.ReviewPanel}, the same
 * reasoning the conversion plugin's {@code ConversionReviewPanel} already applied.
 * <p>
 * Because the wizard's domain model already carries everything Finish needs, this step keeps no
 * form state of its own - whoever opens the wizard fills the summary in through {@link #refresh}
 * once the state machine reaches this step, the same split {@code ConversionReviewPanel} uses.
 */
public class CreateKeyStoreReviewPanel
	extends
		BasePanel<BaseWizardStateMachineModel<CreateKeyStoreWizardModel>>
{

	private static final long serialVersionUID = 1L;

	private JLabel lblHeader;
	private JMTextArea txtSummary;
	private JScrollPane scrSummary;

	public CreateKeyStoreReviewPanel(
		IModel<BaseWizardStateMachineModel<CreateKeyStoreWizardModel>> model)
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

		txtSummary.setToolTipText(KeyStoreMessages.getString("keystore.wizard.review.tooltip.summary",
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
