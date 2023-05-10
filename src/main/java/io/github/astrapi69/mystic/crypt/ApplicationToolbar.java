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
package io.github.astrapi69.mystic.crypt;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.*;

import lombok.Getter;

public class ApplicationToolbar extends JToolBar
{

	@Getter
	private final Set<JButton> toolbarButtons = new LinkedHashSet<>();

	public ApplicationToolbar()
	{
	}

	public ApplicationToolbar(int orientation)
	{
		super(orientation);
	}

	public ApplicationToolbar(String name)
	{
		super(name);
	}

	public ApplicationToolbar(String name, int orientation)
	{
		super(name, orientation);
	}

	public Component add(Component comp)
	{
		if (comp instanceof JButton)
		{
			toolbarButtons.add((JButton)comp);
		}
		return super.add(comp);
	}
}
