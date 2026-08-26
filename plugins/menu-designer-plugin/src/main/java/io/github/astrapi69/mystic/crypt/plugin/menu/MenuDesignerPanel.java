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
package io.github.astrapi69.mystic.crypt.plugin.menu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.List;

import javax.swing.*;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * Tool panel for viewing and editing the application menu as xml.
 * <p>
 * "Export" writes the menu bar that is currently in place into the editor, "Validate" checks the
 * edited xml without touching the application, "Apply" rebuilds the live menu bar from it and "Save"
 * persists it so it is applied on the next start. Every item keeps the action it already has,
 * because the layout only refers to action ids that are harvested from the running menu; unknown
 * ids produce a disabled item instead of breaking the menu. "Reset" removes a persisted layout
 * again.
 */
public class MenuDesignerPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** The width the xml editor keeps even in a layout that is built from minimum sizes */
	private static final int MINIMUM_EDITOR_WIDTH = 320;

	/** The height the xml editor keeps even in a layout that is built from minimum sizes */
	private static final int MINIMUM_EDITOR_HEIGHT = 160;

	private final JTextArea txtMenuXml = new JTextArea(24, 90);
	private final JLabel lblResult = new JLabel(" ");

	public MenuDesignerPanel()
	{
		super(new BorderLayout());

		txtMenuXml.setName("txtMenuXml");
		txtMenuXml.setFont(new Font("monospaced", Font.PLAIN, 12));
		lblResult.setName("lblResult");

		JButton btnExport = button("btnExport", "Export current menu", event -> onExport());
		JButton btnValidate = button("btnValidate", "Validate", event -> onValidate());
		JButton btnApply = button("btnApply", "Apply now", event -> onApply());
		JButton btnSave = button("btnSave", "Save as my menu", event -> onSave());
		JButton btnReset = button("btnReset", "Reset to standard", event -> onReset());

		JPanel buttons = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(4, 6, 4, 6);
		constraints.gridy = 0;
		for (JButton button : new JButton[] { btnExport, btnValidate, btnApply, btnSave, btnReset })
		{
			buttons.add(button, constraints);
		}

		JPanel south = new JPanel(new BorderLayout());
		south.add(buttons, BorderLayout.NORTH);
		south.add(lblResult, BorderLayout.SOUTH);

		add(editorScrollPane(), BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);

		if (MenuDesignerSettingsContribution.exportOnOpen())
		{
			onExport();
		}
	}

	/**
	 * Builds the scroll pane that carries the xml editor.
	 * <p>
	 * A viewport reports a minimum size of a few pixels, so without a stated minimum the scroll
	 * pane claims 22 x 22 px and any container that honours minimum sizes is free to squeeze the
	 * editor away entirely. The honest minimum belongs on the scroll pane rather than on the text
	 * area, whose own size is the size of the text it holds.
	 *
	 * @return the scroll pane around the xml editor, with a minimum size that stays readable
	 */
	private JScrollPane editorScrollPane()
	{
		JScrollPane editor = new JScrollPane(txtMenuXml);
		editor.setMinimumSize(new Dimension(MINIMUM_EDITOR_WIDTH, MINIMUM_EDITOR_HEIGHT));
		return editor;
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		return button;
	}

	private void onExport()
	{
		try
		{
			txtMenuXml.setText(MysticCryptApplicationFrame.getInstance().exportCurrentMenuXml());
			txtMenuXml.setCaretPosition(0);
			lblResult.setText("exported the current menu");
		}
		catch (Exception exception)
		{
			lblResult.setText("export failed: " + exception.getMessage());
		}
	}

	private void onValidate()
	{
		try
		{
			List<String> errors = io.github.astrapi69.mystic.crypt.menu.MenuLayoutSupport
				.validate(txtMenuXml.getText());
			lblResult.setText(
				errors.isEmpty() ? "valid" : errors.size() + " problem(s): " + errors.get(0));
		}
		catch (Exception exception)
		{
			lblResult.setText("invalid: " + exception.getMessage());
		}
	}

	private void onApply()
	{
		try
		{
			MysticCryptApplicationFrame.getInstance().applyMenuXml(txtMenuXml.getText());
			lblResult.setText("applied to the running menu");
		}
		catch (Exception exception)
		{
			lblResult.setText("apply failed: " + exception.getMessage());
		}
	}

	private void onSave()
	{
		try
		{
			Path file = MysticCryptApplicationFrame.getInstance()
				.saveMenuLayout(txtMenuXml.getText());
			lblResult.setText("saved to " + file);
		}
		catch (Exception exception)
		{
			lblResult.setText("save failed: " + exception.getMessage());
		}
	}

	private void onReset()
	{
		try
		{
			boolean removed = MysticCryptApplicationFrame.getInstance().resetMenuLayout();
			lblResult.setText(removed
				? "removed the saved menu, the standard menu returns on the next start"
				: "there was no saved menu");
		}
		catch (Exception exception)
		{
			lblResult.setText("reset failed: " + exception.getMessage());
		}
	}
}
