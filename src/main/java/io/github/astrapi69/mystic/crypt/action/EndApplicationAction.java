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
package io.github.astrapi69.mystic.crypt.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;

/**
 * File > Exit. It ends the application the one way there is - the frame's ending path, which asks
 * about unsaved changes, closes and overwrites the vault, and only then exits.
 * <p>
 * It replaces a library action whose whole body was {@code System.exit(0)}. That one asked nothing,
 * so a user who chose Exit expecting the question every other ending asks lost every change since
 * the last save (#386), and it closed nothing, so the decrypted vault was left for the JVM to
 * release without overwriting (#387). The exit is not called here on purpose: it belongs at the end
 * of the ending path, after the question and the wipe, and a wiring test pins that this class does
 * not reach it any other way.
 */
public class EndApplicationAction extends AbstractAction
{

	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new {@link EndApplicationAction}
	 *
	 * @param name
	 *            the name of the action
	 */
	public EndApplicationAction(final String name)
	{
		super(name);
	}

	@Override
	public void actionPerformed(final ActionEvent actionEvent)
	{
		MysticCryptApplicationFrame.getInstance().endTheApplication();
	}
}
