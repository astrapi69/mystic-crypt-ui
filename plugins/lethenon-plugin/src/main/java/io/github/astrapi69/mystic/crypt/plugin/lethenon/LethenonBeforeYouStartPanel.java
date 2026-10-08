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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import io.github.astrapi69.browser.BrowserControlExtensions;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;

/**
 * "Before you start" (#540): what a public start of lethenon's main chain needs an authorisation
 * for under MiCA and what it does not, in short, with the notice that it is not legal advice and a
 * button for each of the five launch documents of lethenon#141, which carry the provisions, their
 * sources and the date they were last reviewed.
 * <p>
 * It opens from the Lethenon menu, and the mining window, which starts a new chain, shows it before
 * the choice between the test network and the main chain.
 */
public class LethenonBeforeYouStartPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** The height the table asks for before the window gives it more */
	private static final Dimension TABLE_VIEWPORT = new Dimension(640, 200);

	private final LethenonBeforeYouStartPanelModel modelObject;

	private final LaunchCheckTableModel tableModel = new LaunchCheckTableModel();

	private final JTable tblLaunchChecks = new JTable(tableModel);

	private final JLabel lblNotLegalAdvice = new JLabel(
		LaunchChecklist.notice(LethenonMessages::getString));

	/**
	 * Instantiates a new {@link LethenonBeforeYouStartPanel} with the short checklist in the
	 * language of the plugin's text
	 */
	public LethenonBeforeYouStartPanel()
	{
		super(ToolForm.newLayout());
		modelObject = new LethenonBeforeYouStartPanelModel(
			LaunchChecklist.checks(LethenonMessages::getString), List.of(LaunchDocument.values()));
		setName("pnlBeforeYouStart");
		nameTheComponents();
		layOut();
		tableModel.setData(modelObject.getChecks());
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object the panel shows
	 */
	public LethenonBeforeYouStartPanelModel getModelObject()
	{
		return modelObject;
	}

	private void nameTheComponents()
	{
		lblNotLegalAdvice.setName("lblNotLegalAdvice");
		lblNotLegalAdvice.setFont(lblNotLegalAdvice.getFont().deriveFont(Font.BOLD));
		tblLaunchChecks.setName("tblLaunchChecks");
		tblLaunchChecks.setDefaultRenderer(Object.class, new WrappingCellRenderer());
		tblLaunchChecks.setRowSelectionAllowed(false);
		tblLaunchChecks.setFocusable(false);
		tblLaunchChecks.setPreferredScrollableViewportSize(TABLE_VIEWPORT);
		tblLaunchChecks.getTableHeader().setReorderingAllowed(false);
	}

	private void layOut()
	{
		add(lblNotLegalAdvice, ToolForm.WIDE);
		add(ToolForm.intro(LethenonMessages.getString(LaunchChecklist.KEY_PREFIX + "intro",
			"What a public start of lethenon's main chain runs into under MiCA, the EU's "
				+ "Regulation (EU) 2023/1114, in short. The documents below give every provision "
				+ "with its source and the date they were last reviewed.")),
			ToolForm.INTRO_ROW);
		add(new JScrollPane(tblLaunchChecks), ToolForm.GROWING);
		add(new JLabel(LethenonMessages.getString(LaunchChecklist.KEY_PREFIX + "before.the.start",
			"Before the start: a lawyer, and a tax advisor in the country of residence.")),
			ToolForm.WIDE);
		add(ToolForm.buttons(documentButtons()), ToolForm.BUTTON_ROW);
	}

	private JButton[] documentButtons()
	{
		String opens = LethenonMessages.getString(LaunchChecklist.KEY_PREFIX + "tooltip.document",
			"opens in the browser:");
		return modelObject.getDocuments().stream()
			.map(document -> LethenonSwing.button(document.componentName(),
				document.title(LethenonMessages::getString), event -> open(document),
				opens + " " + document.url()))
			.toArray(JButton[]::new);
	}

	private void open(final LaunchDocument document)
	{
		BrowserControlExtensions.displayURLonStandardBrowser(this, document.url());
	}
}
