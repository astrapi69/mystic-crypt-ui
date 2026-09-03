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
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;

import io.github.astrapi69.mystic.crypt.Messages;

/**
 * The "Plugins" tab of the settings dialog: lists the installed pf4j plugins with their id, version
 * and state and lets the user enable, disable and install plugins. Enable/disable state is
 * persisted by pf4j itself (a {@code disabled.txt} in the plugins directory), so it survives
 * restarts.
 */
public class PluginsSettingsPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final transient PluginManager pluginManager;
	private final transient Runnable onPluginsChanged;
	private final PluginTableModel tableModel;
	private final JTable pluginTable;

	public PluginsSettingsPanel(PluginManager pluginManager, Runnable onPluginsChanged)
	{
		super(new BorderLayout(4, 4));
		this.pluginManager = pluginManager;
		this.onPluginsChanged = onPluginsChanged;

		this.tableModel = new PluginTableModel(pluginManager.getPlugins());
		this.pluginTable = new JTable(tableModel);
		this.pluginTable.setName("tblPlugins");
		this.pluginTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		add(new JScrollPane(pluginTable), BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.add(button("btnEnablePlugin", "Enable", e -> enableSelected(), Messages.getString(
			"settings.plugins.tooltip.enable.button", "enables and starts the selected plugin")));
		buttons.add(button("btnDisablePlugin", "Disable", e -> disableSelected(), Messages
			.getString("settings.plugins.tooltip.disable.button", "disables the selected plugin")));
		buttons.add(button("btnInstallPlugin", "Install from Zip...", e -> installFromZip(),
			Messages.getString("settings.plugins.tooltip.install.button",
				"installs a zip file as a new plugin and starts it - only install plugins from sources you trust, a plugin runs with the same access as the application")));
		buttons.add(button("btnOpenPluginsFolder", "Open plugins folder", e -> openPluginsFolder(),
			Messages.getString("settings.plugins.tooltip.open.folder.button",
				"opens the folder the plugins are installed in")));
		buttons.add(button("btnRefreshPlugins", "Refresh", e -> refresh(), Messages
			.getString("settings.plugins.tooltip.refresh.button", "re-reads the plugin list")));
		add(buttons, BorderLayout.SOUTH);
	}

	private static JButton button(String name, String text, java.awt.event.ActionListener listener,
		String tooltip)
	{
		JButton button = new JButton(text);
		button.setName(name);
		button.addActionListener(listener);
		button.setToolTipText(tooltip);
		return button;
	}

	private PluginWrapper getSelectedPlugin()
	{
		int row = pluginTable.getSelectedRow();
		return row < 0 ? null : tableModel.getPluginAt(row);
	}

	private void enableSelected()
	{
		PluginWrapper selected = getSelectedPlugin();
		if (selected != null)
		{
			String pluginId = selected.getPluginId();
			pluginManager.enablePlugin(pluginId);
			pluginManager.startPlugin(pluginId);
			refresh();
			onPluginsChanged.run();
		}
	}

	private void disableSelected()
	{
		PluginWrapper selected = getSelectedPlugin();
		if (selected != null)
		{
			pluginManager.disablePlugin(selected.getPluginId());
			refresh();
			onPluginsChanged.run();
		}
	}

	private void installFromZip()
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Choose a plugin zip to install");
		fileChooser.setFileFilter(new FileNameExtensionFilter("Plugin archives (*.zip)", "zip"));
		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		File zip = fileChooser.getSelectedFile();
		try
		{
			Path target = pluginManager.getPluginsRoot().resolve(zip.getName());
			Files.copy(zip.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
			String pluginId = pluginManager.loadPlugin(target);
			if (pluginId != null)
			{
				pluginManager.startPlugin(pluginId);
			}
			refresh();
			onPluginsChanged.run();
		}
		catch (Exception exception)
		{
			JOptionPane.showMessageDialog(this,
				"Could not install the plugin: " + exception.getMessage(), "Install failed",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	private void openPluginsFolder()
	{
		try
		{
			File folder = pluginManager.getPluginsRoot().toFile();
			if (Desktop.isDesktopSupported())
			{
				Desktop.getDesktop().open(folder);
			}
		}
		catch (Exception exception)
		{
			JOptionPane.showMessageDialog(this,
				"Could not open the plugins folder: " + exception.getMessage(),
				"Open folder failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Re-reads the plugin list from the plugin manager into the table */
	public void refresh()
	{
		tableModel.setPlugins(pluginManager.getPlugins());
	}

	/** The table model over the plugin manager's plugins */
	static final class PluginTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private static final String[] COLUMNS = { "Plugin", "Version", "State" };

		private transient List<PluginWrapper> plugins;

		PluginTableModel(List<PluginWrapper> plugins)
		{
			this.plugins = plugins;
		}

		void setPlugins(List<PluginWrapper> plugins)
		{
			this.plugins = plugins;
			fireTableDataChanged();
		}

		PluginWrapper getPluginAt(int row)
		{
			return plugins.get(row);
		}

		@Override
		public int getRowCount()
		{
			return plugins.size();
		}

		@Override
		public int getColumnCount()
		{
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return COLUMNS[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			PluginWrapper wrapper = plugins.get(rowIndex);
			PluginState state = wrapper.getPluginState();
			return switch (columnIndex)
			{
				case 0 -> wrapper.getPluginId();
				case 1 -> wrapper.getDescriptor().getVersion();
				case 2 -> state == null ? "" : state.toString();
				default -> "";
			};
		}
	}
}
