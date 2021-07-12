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
package io.github.astrapi69.mystic.crypt.panels.signin;

import java.beans.PropertyChangeSupport;

import javax.swing.*;

import lombok.Getter;

/**
 * The class {@link CheckBoxModel} decorates a boolean value for a checkbox
 */
public class CheckBoxModel extends JToggleButton.ToggleButtonModel
{

	/**
	 * The current value of the checkbox
	 */
	@Getter
	private boolean checked;

	/**
	 * The property change support object
	 */
	private final PropertyChangeSupport propertySupport;

	/**
	 * initial block
	 */
	{
		this.propertySupport = new PropertyChangeSupport(this);
	}

	/**
	 * Instantiates a new {@link CheckBoxModel}
	 */
	public CheckBoxModel()
	{
	}

	/**
	 * Instantiates a new {@link CheckBoxModel} from the given checked value
	 *
	 * @param checked
	 *            the initial value for checked
	 */
	public CheckBoxModel(boolean checked)
	{
		this.checked = checked;
		setSelected(this.checked);
	}

	/**
	 * Factory method for create a {@link CheckBoxModel} object
	 * @param checked
	 *            the initial value for checked
	 * @return the new {@link CheckBoxModel} object
	 */
	public static CheckBoxModel of(boolean checked){
		return checked ? new CheckBoxModel(checked) : new CheckBoxModel();
	}

	/**
	 * Factory method for create a {@link CheckBoxModel} object
	 *
	 * @return the new {@link CheckBoxModel} object
	 */
	public static CheckBoxModel of()
	{
		return of(false);
	}

	/**
	 * Sets the new checked value
	 *
	 * @param checked
	 *            the new checked value
	 */
	public void setChecked(boolean checked)
	{
		boolean oldValue = this.checked;
		this.checked = checked;
		propertySupport.firePropertyChange("checked", oldValue, this.checked);
	}
}