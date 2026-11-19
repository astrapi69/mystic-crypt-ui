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
import java.io.IOException;
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
import io.github.astrapi69.mystic.crypt.plugin.InvalidPluginZipException;
import io.github.astrapi69.mystic.crypt.plugin.PluginArchives;
import io.github.astrapi69.mystic.crypt.plugin.PluginZipInstallation;

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

	/** The chooser of the install: plugin zips only */
	static JFileChooser zipChooser()
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(Messages.getString("settings.plugins.install.chooser.title",
			"Choose a plugin zip to install"));
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString(
			"settings.plugins.install.chooser.filter", "Plugin archives (*.zip)"), "zip"));
		return fileChooser;
	}

	private void installFromZip()
	{
		JFileChooser fileChooser = zipChooser();
		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		File zip = fileChooser.getSelectedFile();
		try
		{
			Path target = pluginManager.getPluginsRoot().resolve(zip.getName());
			PluginZipInstallation.Decision decision = PluginZipInstallation.decide(pluginManager,
				zip.toPath(), target);
			switch (decision.kind())
			{
				case REFUSED_OTHER_PLUGIN :
					throw new IOException(String.format(Messages.getString(
						"settings.plugins.install.other.plugin",
						"%s holds the plugin %s, but the plugin installed under this name is %s"),
						zip.getName(), decision.pluginId(), decision.otherPluginId()));
				case REFUSED_OTHER_FILE :
					throw new IOException(String.format(
						Messages.getString("settings.plugins.install.other.name",
							"The plugin %s is loaded from another file than %s"),
						decision.pluginId(), zip.getName()));
				case UPDATE_OF_LOADED :
					installOverALoadedPlugin(zip.toPath(), target, decision.loaded());
					break;
				default :
					installNewPlugin(zip.toPath(), target);
					break;
			}
		}
		catch (InvalidPluginZipException invalid)
		{
			showInstallFailed(
				String.format(Messages.getString("settings.plugins.install.not.a.plugin.zip",
					"%s is not a plugin zip: %s"), invalid.getFileName(), invalid.getReason()));
		}
		catch (Exception exception)
		{
			showInstallFailed(exception.getMessage());
		}
	}

	private void showInstallFailed(final String reason)
	{
		JOptionPane.showMessageDialog(this,
			String.format(Messages.getString("settings.plugins.install.failed.message",
				"Could not install the plugin: %s"), reason),
			Messages.getString("settings.plugins.install.failed.title", "Install failed"),
			JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Installs the zip of a plugin that is not loaded: unpacked, loaded and started, or, where its
	 * directory is in use, left for the next start (#998)
	 */
	private void installNewPlugin(final Path zip, final Path target) throws IOException
	{
		Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
		if (PluginArchives.unpackIfChanged(target) == PluginArchives.Outcome.AFTER_RESTART)
		{
			reportUpdateForTheNextStart(Messages.getString(
				"settings.plugins.install.after.restart.message",
				"The plugin is in use. Its new version is installed and runs after the next start of the application."));
			return;
		}
		loadAndStart(target);
	}

	/**
	 * Installs the zip of a plugin that is loaded: the zip alone is put into the plugins directory
	 * and the directory of the running plugin is left as it is, since its class loader reads loose
	 * classes from it and two versions would meet in it. The next start unpacks the zip before any
	 * plugin is loaded (#1045)
	 */
	private void installOverALoadedPlugin(final Path zip, final Path target,
		final PluginWrapper loaded) throws IOException
	{
		if (PluginArchives.isUnpackedFrom(zip, PluginArchives.directoryOf(target)))
		{
			JOptionPane.showMessageDialog(this,
				Messages.getString("settings.plugins.install.unchanged.message",
					"This version of the plugin is already installed. Nothing changed."),
				Messages.getString("settings.plugins.install.unchanged.title",
					"Plugin already installed"),
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
		if (loaded.getPluginState() == PluginState.DISABLED)
		{
			reportUpdateForTheNextStart(Messages.getString(
				"settings.plugins.install.disabled.message",
				"The plugin is disabled and stays disabled. Its new version is installed and is used after the next start once you enable it."));
			return;
		}
		reportUpdateForTheNextStart(Messages.getString(
			"settings.plugins.install.after.restart.message",
			"The plugin is in use. Its new version is installed and runs after the next start of the application."));
	}

	private void loadAndStart(final Path target)
	{
		String pluginId = pluginManager.loadPlugin(target);
		if (pluginId != null)
		{
			pluginManager.startPlugin(pluginId);
		}
		refresh();
		onPluginsChanged.run();
	}

	private void reportUpdateForTheNextStart(final String message)
	{
		JOptionPane.showMessageDialog(this, message,
			Messages.getString("settings.plugins.install.after.restart.title",
				"Plugin updated at the next start"),
			JOptionPane.INFORMATION_MESSAGE);
		refresh();
		onPluginsChanged.run();
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
