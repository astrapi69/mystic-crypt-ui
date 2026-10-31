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
package io.github.astrapi69.mystic.crypt.plugin.releasekey;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * The release key status view, stage 1 of #642: what a GnuPG home holds - every key, what it is
 * for, when it expires, whether its secret part is in the home or offline, and whether gpg keeps a
 * revocation certificate for it. It reads, it never writes; the configured home is read as the view
 * opens, and again on Read.
 */
public class ReleaseKeyStatusPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** The size the table asks for, so the window opens wide enough to read a row */
	private static final Dimension PREFERRED_TABLE_SIZE = new Dimension(900, 180);

	/** The height the table keeps when the window has nothing left to give */
	private static final int MINIMUM_TABLE_HEIGHT = 48;

	/** Everything the user typed and the last reading; the fields below write into it */
	private final transient ReleaseKeyStatusPanelModel modelObject = new ReleaseKeyStatusPanelModel();

	private final JMTextField txtGnuPgHome = new JMTextField(40);
	private final JMTextField txtReleaseKey = new JMTextField(40);
	private final DefaultTableModel keyModel = new DefaultTableModel(KeyRows.COLUMNS.toArray(), 0)
	{
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int row, int column)
		{
			// the table shows what the home holds; it is read, never edited
			return false;
		}
	};
	private final JTable tblKeys = new JTable(keyModel);
	private final JLabel lblResult = new JLabel(" ");

	/**
	 * Builds the view and reads the configured home
	 */
	public ReleaseKeyStatusPanel()
	{
		super(ToolForm.newLayout());
		modelObject.setGnuPgHomePath(ReleaseKeySettingsContribution.gnuPgHome().toString());
		modelObject.setReleaseKey(ReleaseKeySettingsContribution.releaseKey());
		txtGnuPgHome.setPropertyModel(
			LambdaModel.of(modelObject::getGnuPgHomePath, modelObject::setGnuPgHomePath));
		txtReleaseKey.setPropertyModel(
			LambdaModel.of(modelObject::getReleaseKey, modelObject::setReleaseKey));
		describeComponents();
		layoutComponents();
		onRead();
	}

	/**
	 * Gets what the view holds
	 *
	 * @return the model the fields are bound to
	 */
	public ReleaseKeyStatusPanelModel getModelObject()
	{
		return modelObject;
	}

	private void describeComponents()
	{
		txtGnuPgHome.setName("txtGnuPgHome");
		txtReleaseKey.setName("txtReleaseKey");
		tblKeys.setName("tblKeys");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		txtGnuPgHome.setToolTipText(ReleaseKeyMessages.getString("releasekey.tooltip.home",
			"the GnuPG home to read, for example ~/.gnupg"));
		txtReleaseKey.setToolTipText(ReleaseKeyMessages.getString("releasekey.tooltip.release.key",
			"the fingerprint or long key id of the release key; left blank, every certificate of "
				+ "the home is shown"));
	}

	private void layoutComponents()
	{
		ToolForm.sized(txtGnuPgHome);
		ToolForm.sized(txtReleaseKey);
		add(ToolForm.intro(ReleaseKeyMessages.getString("releasekey.intro",
			"What a GnuPG home holds, read with Bouncy Castle. Nothing is written to it and "
				+ "nothing is decrypted.")),
			ToolForm.INTRO_ROW);
		add(new JLabel(ReleaseKeyMessages.getString("releasekey.label.home", "GnuPG home:")));
		add(txtGnuPgHome, ToolForm.FIELD + ", split 2");
		add(button("btnBrowse", "...", event -> onBrowse(), ReleaseKeyMessages
			.getString("releasekey.tooltip.browse", "choose the GnuPG home")));
		add(new JLabel(
			ReleaseKeyMessages.getString("releasekey.label.release.key", "Release key:")));
		add(txtReleaseKey, ToolForm.FIELD);
		add(ToolForm.buttons(
			button("btnRead", ReleaseKeyMessages.getString("releasekey.button.read", "Read"),
				event -> onRead(), ReleaseKeyMessages.getString("releasekey.tooltip.read",
					"reads the GnuPG home above again"))),
			ToolForm.BUTTON_ROW);
		JScrollPane tableScrollPane = new JScrollPane(tblKeys);
		tableScrollPane.setPreferredSize(PREFERRED_TABLE_SIZE);
		tableScrollPane
			.setMinimumSize(new Dimension(ToolForm.MINIMUM_FIELD_WIDTH, MINIMUM_TABLE_HEIGHT));
		add(tableScrollPane, ToolForm.GROWING);
		add(lblResult, ToolForm.RESULT_LINE);
	}

	private void onBrowse()
	{
		JFileChooser chooser = new JFileChooser(
			ReleaseKeySettingsContribution.homeOf(modelObject.getGnuPgHomePath()).toFile());
		chooser.setDialogTitle(
			ReleaseKeyMessages.getString("releasekey.chooser.title", "GnuPG home"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		// ~/.gnupg is a hidden directory, and a chooser that hides it cannot choose it
		chooser.setFileHidingEnabled(false);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File chosen = chooser.getSelectedFile();
			modelObject.setGnuPgHomePath(chosen.getAbsolutePath());
			txtGnuPgHome.setText(modelObject.getGnuPgHomePath());
		}
	}

	private void onRead()
	{
		StatusReport report = ReleaseKeyStatus.read(
			ReleaseKeySettingsContribution.homeOf(modelObject.getGnuPgHomePath()),
			modelObject.getReleaseKey());
		modelObject.setReport(report);
		keyModel.setRowCount(0);
		for (List<String> row : KeyRows.of(report.certificates(), LocalDate.now(ZoneOffset.UTC)))
		{
			keyModel.addRow(row.toArray());
		}
		lblResult.setText(report.message());
	}

	private static JButton button(final String name, final String text,
		final ActionListener listener, final String tooltip)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		button.setToolTipText(tooltip);
		return button;
	}
}
