package io.github.astrapi69.mystic.crypt.panels.signin;

import de.alpharogroup.swing.base.ApplicationFrame;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The abstract class {@link DocumentListenerAdapter} is an adapter for the
 * DocumentListener inteface and provides one callback method that have to be implemented
 */
public abstract class DocumentListenerAdapter implements DocumentListener
{
	/**
	 * {@inheritDoc}
	 */
	@Override public void changedUpdate(DocumentEvent e)
	{
		onDocumentChanged(e);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public void insertUpdate(DocumentEvent e)
	{
		onDocumentChanged(e);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override public void removeUpdate(DocumentEvent e)
	{
		onDocumentChanged(e);
	}

	/**
	 * Callback method that have to be overwritten to provide specific action on change of document.
	 *
	 * @param e - the DocumentEvent from the specific DocumentListener method
	 */
	public abstract void onDocumentChanged(DocumentEvent e);

}
