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
import java.io.File;

import javax.swing.*;

import org.pf4j.PluginManager;

import io.github.astrapi69.mystic.crypt.lock.PublicAccess;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginSettingsContribution;

/**
 * The settings dialog content: a tabbed pane with a "Plugins" tab (manage/install the pf4j
 * plugins), a "Plugin settings" tab (the configuration each plugin declares for itself) and a
 * "General" tab (look and feel, language). The "General" tab reads from and writes into the shared
 * {@link MysticCryptSettings} passed in; the plugin settings live in their own file per plugin.
 */
public class SettingsPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final transient MysticCryptSettings settings;

	public SettingsPanel(MysticCryptSettings settings, PluginManager pluginManager,
		Runnable onPluginsChanged, File configurationDirectory, boolean signedIn)
	{
		super(new BorderLayout());
		this.settings = settings;

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setName("tabSettings");
		tabbedPane.addTab("Plugins", new PluginsSettingsPanel(pluginManager, onPluginsChanged));
		// Installing and enabling a plugin loads code into the running process, right away and
		// without a restart - so it is not something the application offers while no vault is
		// open. The whole tab, not its buttons: a list of installed plugins without them is worth
		// little, and picking widgets out of dialogs is a list that could never be finished (#232).
		//
		// Asked rather than listed: the menu whitelist covers menu entries, and everything else
		// that offers a privileged capability asks the predicate directly
		tabbedPane.setEnabledAt(0, PublicAccess.isOffered(signedIn, false));
		if (!signedIn)
		{
			tabbedPane.setToolTipTextAt(0,
				"Sign in first: installing or enabling a plugin runs its code in this application");
		}
		tabbedPane.addTab("Plugin settings",
			new PluginSettingsPanel(configurationDirectory, PluginSettingsPanel
				.usable(pluginManager.getExtensions(PluginSettingsContribution.class))));
		tabbedPane.addTab("General", new GeneralSettingsPanel(settings));
		add(tabbedPane, BorderLayout.CENTER);
	}

	public MysticCryptSettings getSettings()
	{
		return settings;
	}
}
