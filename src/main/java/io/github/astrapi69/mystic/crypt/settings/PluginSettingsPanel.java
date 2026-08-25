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
package io.github.astrapi69.mystic.crypt.settings;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;

/**
 * The "Plugin settings" tab: on the left the plugins that have settings, on the right their keys
 * and values, editable in place.
 * <p>
 * Values are written when "Apply" is pressed, so an accidental keystroke does not silently change
 * how a plugin behaves; "Reset to defaults" removes the stored file and shows the declared defaults
 * again.
 */
public class PluginSettingsPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final String[] COLUMNS = { "setting", "value" };

	private final transient File configurationDirectory;

	private final transient List<PluginSettingsContribution> contributions;

	private final DefaultListModel<String> pluginNames = new DefaultListModel<>();

	private final JList<String> lstPluginSettings = new JList<>(pluginNames);

	private final DefaultTableModel valueModel = new DefaultTableModel(COLUMNS, 0)
	{
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int row, int column)
		{
			// the key names the plugin declared are fixed; only the value is the user's to change
			return column == 1;
		}
	};

	private final JTable tblPluginSettings = new JTable(valueModel);

	private final JLabel lblPluginSettingsResult = new JLabel(" ");

	public PluginSettingsPanel(File configurationDirectory,
		List<PluginSettingsContribution> contributions)
	{
		super(new BorderLayout(4, 4));
		this.configurationDirectory = configurationDirectory;
		this.contributions = contributions != null ? contributions : List.of();

		lstPluginSettings.setName("lstPluginSettings");
		lstPluginSettings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tblPluginSettings.setName("tblPluginSettings");
		lblPluginSettingsResult.setName("lblPluginSettingsResult");

		for (PluginSettingsContribution contribution : this.contributions)
		{
			pluginNames.addElement(contribution.getDisplayName());
		}
		lstPluginSettings.addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting())
			{
				showSelectedSettings();
			}
		});

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			new JScrollPane(lstPluginSettings), new JScrollPane(tblPluginSettings));
		splitPane.setDividerLocation(180);
		add(splitPane, BorderLayout.CENTER);

		JPanel south = new JPanel(new BorderLayout());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.add(button("btnApplyPluginSettings", "Apply", event -> applySelectedSettings()));
		buttons.add(button("btnResetPluginSettings", "Reset to defaults",
			event -> resetSelectedSettings()));
		south.add(buttons, BorderLayout.WEST);
		south.add(lblPluginSettingsResult, BorderLayout.SOUTH);
		add(south, BorderLayout.SOUTH);

		if (!this.contributions.isEmpty())
		{
			lstPluginSettings.setSelectedIndex(0);
		}
		else
		{
			lblPluginSettingsResult.setText("no plugin brings its own settings");
		}
	}

	/**
	 * The contribution the user is looking at, or {@code null} when nothing is selected
	 *
	 * @return the selected contribution
	 */
	public PluginSettingsContribution getSelectedContribution()
	{
		int index = lstPluginSettings.getSelectedIndex();
		return index < 0 || index >= contributions.size() ? null : contributions.get(index);
	}

	/**
	 * The values as they are currently shown in the table, which is what "Apply" would store
	 *
	 * @return the shown settings
	 */
	public Map<String, String> getShownValues()
	{
		Map<String, String> values = new LinkedHashMap<>();
		for (int row = 0; row < valueModel.getRowCount(); row++)
		{
			values.put(String.valueOf(valueModel.getValueAt(row, 0)),
				String.valueOf(valueModel.getValueAt(row, 1)));
		}
		return values;
	}

	/** Puts a value into the table, as editing the cell would */
	public void setShownValue(String key, String value)
	{
		for (int row = 0; row < valueModel.getRowCount(); row++)
		{
			if (key.equals(valueModel.getValueAt(row, 0)))
			{
				valueModel.setValueAt(value, row, 1);
				return;
			}
		}
	}

	/** The message shown below the table */
	public String getResultText()
	{
		return lblPluginSettingsResult.getText();
	}

	private void showSelectedSettings()
	{
		valueModel.setRowCount(0);
		PluginSettingsContribution contribution = getSelectedContribution();
		if (contribution == null)
		{
			return;
		}
		Map<String, String> defaults = contribution.getDefaults();
		Map<String, String> stored = PluginSettings.load(configurationDirectory,
			contribution.getPluginId(), defaults);
		for (Map.Entry<String, String> entry : stored.entrySet())
		{
			valueModel.addRow(new Object[] { entry.getKey(), entry.getValue() });
		}
		lblPluginSettingsResult
			.setText(contribution.getDisplayName() + ": " + stored.size() + " settings");
	}

	private void applySelectedSettings()
	{
		PluginSettingsContribution contribution = getSelectedContribution();
		if (contribution == null)
		{
			return;
		}
		stopEditing();
		try
		{
			PluginSettings.save(configurationDirectory, contribution.getPluginId(),
				contribution.getDefaults(), getShownValues());
			lblPluginSettingsResult
				.setText("saved the settings of " + contribution.getDisplayName());
		}
		catch (Exception exception)
		{
			lblPluginSettingsResult.setText("not saved: " + exception);
		}
	}

	private void resetSelectedSettings()
	{
		PluginSettingsContribution contribution = getSelectedContribution();
		if (contribution == null)
		{
			return;
		}
		stopEditing();
		try
		{
			PluginSettings.reset(configurationDirectory, contribution.getPluginId());
			showSelectedSettings();
			lblPluginSettingsResult
				.setText("the defaults of " + contribution.getDisplayName() + " apply again");
		}
		catch (Exception exception)
		{
			lblPluginSettingsResult.setText("not reset: " + exception);
		}
	}

	private void stopEditing()
	{
		// a cell that is still being edited would otherwise keep the typed value out of the model
		if (tblPluginSettings.isEditing())
		{
			tblPluginSettings.getCellEditor().stopCellEditing();
		}
	}

	/**
	 * Collects the settings contributions of the started plugins, skipping one that fails instead
	 * of losing the whole tab over it
	 *
	 * @param contributions
	 *            the contributions the plugin manager reports
	 * @return the usable contributions
	 */
	public static List<PluginSettingsContribution> usable(
		List<PluginSettingsContribution> contributions)
	{
		List<PluginSettingsContribution> usable = new ArrayList<>();
		if (contributions == null)
		{
			return usable;
		}
		for (PluginSettingsContribution contribution : contributions)
		{
			try
			{
				if (contribution.getPluginId() != null && contribution.getDefaults() != null
					&& !contribution.getDefaults().isEmpty())
				{
					usable.add(contribution);
				}
			}
			catch (RuntimeException exception)
			{
				System.err.println("the settings of " + contribution.getClass().getName()
					+ " are not available: " + exception);
			}
		}
		return usable;
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		return button;
	}
}
