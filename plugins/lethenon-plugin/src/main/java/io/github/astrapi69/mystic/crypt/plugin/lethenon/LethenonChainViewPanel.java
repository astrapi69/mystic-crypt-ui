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

import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Shows the blocks of a chain file: their height, the pun each was mined with, whom each paid, how
 * many transfers it carries, when it was made and how hard it was (lethenon#2, milestone 5).
 * <p>
 * Read-only and without a wallet. The rows come from {@link ChainReplaySupport#blocks}, which
 * replays the whole chain before it lists anything, so this panel shows no block of a chain that
 * did not verify - a refused chain empties the table and the reason takes its place.
 */
public class LethenonChainViewPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	private final LethenonChainViewPanelModel modelObject = new LethenonChainViewPanelModel();

	private final ChainBlockTableModel tableModel = new ChainBlockTableModel();

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JTable tblBlocks = new JTable(tableModel);

	private final JLabel lblResult = new JLabel(" ");

	/**
	 * Instantiates a new {@link LethenonChainViewPanel}, prefilled with the chain file from the
	 * plugin's settings
	 */
	public LethenonChainViewPanel()
	{
		super(ToolForm.newLayout());
		txtChainFile.setName("txtChainFile");
		tblBlocks.setName("tblBlocks");
		tblBlocks.setAutoCreateRowSorter(false);
		tblBlocks.setFillsViewportHeight(true);
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));

		txtChainFile.setToolTipText(LethenonMessages.getString("lethenon.tooltip.chain.file",
			"the chain file to replay, written by lethenon's own command line"));
		tblBlocks.setToolTipText(LethenonMessages.getString("lethenon.tooltip.blocks",
			"every block of the chain, genesis first, shown only after the whole chain verified"));
		txtChainFile
			.setPropertyModel(LambdaModel.of(modelObject::getChainFile, modelObject::setChainFile));

		add(new JLabel(LethenonMessages.getString("lethenon.label.chain.file", "Chain file:")));
		add(txtChainFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseChainFile", "...", event -> onBrowseChainFile(),
			LethenonMessages.getString("lethenon.tooltip.browse.chain.file",
				"pick the chain file")),
			OWN_WIDTH);
		add(ToolForm.buttons(LethenonSwing.button("btnShowBlocks",
			LethenonMessages.getString("lethenon.button.show.blocks", "Show the blocks"),
			event -> onShow(), LethenonMessages.getString("lethenon.tooltip.show.blocks",
				"replay the chain and list its blocks"))),
			ToolForm.BUTTON_ROW);
		add(new JScrollPane(tblBlocks), ToolForm.GROWING);
		add(lblResult, ToolForm.RESULT_LINE);

		txtChainFile.setText(LethenonSettingsContribution.chainFile());
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonChainViewPanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Gets the model of the block table, which holds exactly the rows of {@link #getModelObject()}
	 *
	 * @return the table model
	 */
	public ChainBlockTableModel getTableModel()
	{
		return tableModel;
	}

	/**
	 * Replays the named file and lists its blocks. A refused chain is not an error of this panel:
	 * the table is emptied, so no row of an earlier chain stays next to the refusal, and the reason
	 * goes into the result line.
	 */
	void onShow()
	{
		String refusal = LethenonMessages.getString("lethenon.result.refused",
			"the chain was refused");
		try
		{
			List<ChainBlockRow> rows = ChainReplaySupport
				.blocks(Path.of(modelObject.getChainFile().trim()));
			setRows(rows);
			setResult(
				LethenonMessages.getString("lethenon.result.accepted", "the chain was accepted")
					+ ": " + rows.size() + " blocks");
		}
		catch (ChainRejected | IllegalArgumentException refused)
		{
			setRows(List.of());
			setResult(refusal + ": " + refused.getMessage());
		}
		catch (IOException unreadable)
		{
			setRows(List.of());
			setResult(refusal + ": " + modelObject.getChainFile() + " cannot be read - "
				+ unreadable.getMessage());
		}
	}

	private void setRows(final List<ChainBlockRow> rows)
	{
		modelObject.setRows(rows);
		tableModel.setData(modelObject.getRows());
	}

	private void setResult(final String text)
	{
		modelObject.setResultText(text);
		lblResult.setText(text);
	}

	private void onBrowseChainFile()
	{
		LethenonSwing.chooseFile(this, modelObject.getChainFile()).ifPresent(txtChainFile::setText);
	}
}
