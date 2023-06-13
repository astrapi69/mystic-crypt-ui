/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.action;

import java.awt.event.ActionEvent;

import javax.swing.*;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.panel.obfuscate.character.OperationRulePanel;
import io.github.astrapi69.mystic.crypt.util.InternalFrameExtensions;
import io.github.astrapi69.swing.component.factory.JComponentFactory;
import io.github.astrapi69.swing.enumeration.FrameMode;

/**
 * The class {@link NewObfuscationOperationInternalFrameAction}.
 */
public class NewObfuscationOperationInternalFrameAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new new action.
	 *
	 * @param name
	 *            the name
	 */
	public NewObfuscationOperationInternalFrameAction(final String name)
	{
		super(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
		if (!FrameMode.DESKTOP_PANE.equals(instance.getFrameMode()))
		{
			instance.switchToDesktopPane();
		}
		// create internal frame
		final JInternalFrame internalFrame = JComponentFactory
			.newInternalFrame("Obfuscation Operation demo", true, true, true, true);
		final OperationRulePanel component = new OperationRulePanel();
		InternalFrameExtensions.addInternalFrameToMainFrame(component, internalFrame,
			MysticCryptApplicationFrame.getInstance());
	}

}
