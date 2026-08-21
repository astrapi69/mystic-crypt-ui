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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation;

import java.awt.Component;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;

import org.pf4j.Extension;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.character.OperationRulePanel;
import io.github.astrapi69.mystic.crypt.plugin.obfuscation.simple.RulePanel;
import io.github.astrapi69.swing.enumeration.FrameMode;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.util.JInternalFrameExtensions;

/**
 * Contributes the obfuscation tools to the host's "Plugins" menu. This is the reference internal
 * plugin: the obfuscation feature used to be wired straight into the application's menu, it now
 * ships as a real pf4j plugin instead, proving the plugin system can deliver a first-class feature.
 * <p>
 * Two tools are contributed:
 * <ul>
 * <li><b>Simple Obfuscation</b> - a plain character-substitution cipher (each character maps to one
 * replacement character)
 * <li><b>Operated Obfuscation</b> - character substitution combined with operations (upper case,
 * lower case, ...)
 * </ul>
 */
@Extension
public class ObfuscationMenuContribution implements PluginMenuContribution
{

	@Override
	public List<JMenuItem> getMenuItems()
	{
		JMenuItem simpleObfuscation = new JMenuItem("Simple Obfuscation");
		simpleObfuscation.addActionListener(
			event -> openInternalFrame("Simple Obfuscation", new RulePanel()));

		JMenuItem operatedObfuscation = new JMenuItem("Operated Obfuscation");
		operatedObfuscation.addActionListener(
			event -> openInternalFrame("Operated Obfuscation", new OperationRulePanel()));

		return List.of(simpleObfuscation, operatedObfuscation);
	}

	private void openInternalFrame(String title, Component panel)
	{
		MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
		if (!FrameMode.DESKTOP_PANE.equals(instance.getFrameMode()))
		{
			instance.switchToDesktopPane();
		}
		JInternalFrame internalFrame = JComponentFactory.newInternalFrame(title, true, true, true,
			true);
		JInternalFrameExtensions.addInternalFrameToMainFrame(panel, internalFrame, instance);
	}

}
