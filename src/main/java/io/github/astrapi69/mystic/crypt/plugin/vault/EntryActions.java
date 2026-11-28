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
package io.github.astrapi69.mystic.crypt.plugin.vault;

import java.awt.Component;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.plugin.api.EntryActionContribution;
import io.github.astrapi69.mystic.crypt.ui.icon.PluginIcon;
import lombok.extern.java.Log;

/**
 * The menu items of the plugins' entry actions (#1120), in the context menu of the entry table.
 * Each hands its plugin a copy of the one selected entry when it is clicked, never before and never
 * another entry; an action that fails is named in a message and leaves the application running.
 */
@Log
public final class EntryActions
{

	private EntryActions()
	{
	}

	/**
	 * An entry action with the id of the plugin that brought it
	 *
	 * @param contribution
	 *            the action
	 * @param pluginId
	 *            its plugin's id, which names its icon
	 */
	public record Offered(EntryActionContribution contribution, String pluginId) {
	}

	/**
	 * The menu items of the offered actions
	 *
	 * @param offered
	 *            the actions of the started plugins
	 * @param selected
	 *            the one selected entry, empty while none or several are selected
	 * @param parent
	 *            the component the menu belongs to
	 * @return one item per action, enabled only while one entry is selected
	 */
	public static List<JMenuItem> menuItems(final List<Offered> offered,
		final Optional<MysticCryptEntryModelBean> selected, final Component parent)
	{
		List<JMenuItem> items = new ArrayList<>();
		for (Offered action : offered)
		{
			JMenuItem item = new JMenuItem(action.contribution().getActionName());
			item.setName("mnuEntryAction-" + action.pluginId());
			Icon icon = PluginIcon.of(action.contribution().getClass(), action.pluginId());
			if (icon != null)
			{
				item.setIcon(icon);
			}
			item.setEnabled(selected.isPresent());
			selected.ifPresent(
				entry -> item.addActionListener(event -> perform(action, entry, parent)));
			items.add(item);
		}
		return items;
	}

	/**
	 * Hands the plugin a copy of the entry, and names a failure in a message instead of letting it
	 * end the application
	 *
	 * @param action
	 *            the clicked action
	 * @param entry
	 *            the vault's entry; only its copy leaves this method
	 * @param parent
	 *            where the message is shown
	 */
	static void perform(final Offered action, final MysticCryptEntryModelBean entry,
		final Component parent)
	{
		perform(action, entry, parent,
			message -> JOptionPane.showMessageDialog(parent, message,
				Messages.getString("entry.action.failed.title", "Plugin action failed"),
				JOptionPane.ERROR_MESSAGE));
	}

	/**
	 * As {@link #perform(Offered, MysticCryptEntryModelBean, Component)}, with the place the
	 * failure is told given
	 *
	 * @param action
	 *            the clicked action
	 * @param entry
	 *            the vault's entry; only its copy leaves this method
	 * @param parent
	 *            the component handed to the action
	 * @param failureTold
	 *            told the sentence that names a failure
	 */
	static void perform(final Offered action, final MysticCryptEntryModelBean entry,
		final Component parent, final Consumer<String> failureTold)
	{
		try
		{
			action.contribution().act(MarkedEntryAccess.copyOf(entry), parent);
		}
		catch (final RuntimeException failure)
		{
			log.log(Level.WARNING, "The entry action of " + action.pluginId() + " failed", failure);
			failureTold.accept(MessageFormat.format(
				Messages.getString("entry.action.failed",
					"The action of the plugin {0} failed: {1}"),
				action.pluginId(), String.valueOf(failure.getMessage())));
		}
	}
}
