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
package io.github.astrapi69.mystic.crypt.ui.form;

import io.github.astrapi69.swing.model.component.JMPasswordField;

/**
 * A {@link JMPasswordField} that is the same only as itself.
 * <p>
 * The library field derives {@code equals} and {@code hashCode} from its model, which follows the
 * text, so the field is lost as a key of a hash map after the first keystroke. The layout of a
 * panel keeps its components in such a map: {@code GroupLayout} then finds no springs for the field
 * and throws a {@code NullPointerException} at every layout (#1245). A component is a window-system
 * object, and the same one is meant by the same reference, so identity is the contract this class
 * restores. The gap itself belongs to swing-model-components.
 */
public class LayoutSafePasswordField extends JMPasswordField
{

	private static final long serialVersionUID = 1L;

	@Override
	public boolean equals(final Object other)
	{
		return this == other;
	}

	@Override
	public int hashCode()
	{
		return System.identityHashCode(this);
	}
}
