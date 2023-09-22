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
import java.io.Serial;

import javax.swing.*;

import io.github.astrapi69.component.model.enumeration.visibility.RenderMode;
import io.github.astrapi69.design.pattern.observer.event.EventObject;
import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.eventbus.ApplicationEventBus;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker;

public class SaveApplicationFileAction extends AbstractAction
{

	/** The Constant serialVersionUID. */
	@Serial
	private static final long serialVersionUID = 1L;

	public SaveApplicationFileAction(final String name)
	{
		super(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		ApplicationXmlFileStoreWorker
			.storeApplicationFile(MysticCryptApplicationFrame.getInstance().getModelObject());
		ApplicationEventBus.getSaveState().fireEvent(new EventObject<>(RenderMode.VIEWABLE));
	}
}
