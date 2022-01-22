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
package io.github.astrapi69.mystic.crypt.panels.dbtree;

import javax.swing.*;

import io.github.astrapi69.icon.ImageIconFactory;
import io.github.astrapi69.swing.tree.renderer.GenericTreeNodeCellRenderer;
import io.github.astrapi69.throwable.RuntimeExceptionDecorator;

public class JXTreeNodeRemixIconCellRenderer<T> extends GenericTreeNodeCellRenderer<T>
{
	ImageIcon openIcon;
	ImageIcon closedIcon;
	ImageIcon leafIcon;

	public Icon getOpenIcon()
	{
		if (openIcon == null)
		{
			String imagePath = "io/github/astrapi69/remixicon/Document/folder-5-fill.svg";
			openIcon = RuntimeExceptionDecorator
				.decorate(() -> ImageIconFactory.newImageIconFromSVG(imagePath, 16.f, 16.f));
		}
		return openIcon;
	}

	public Icon getLeafIcon()
	{
		if (leafIcon == null)
		{
			String imagePath = "io/github/astrapi69/remixicon/Document/file-text-line.svg";
			leafIcon = RuntimeExceptionDecorator
				.decorate(() -> ImageIconFactory.newImageIconFromSVG(imagePath, 16.f, 16.f));
		}
		return leafIcon;
	}

	public Icon getClosedIcon()
	{
		if (closedIcon == null)
		{
			String imagePath = "io/github/astrapi69/remixicon/Document/folder-3-fill.svg";
			closedIcon = RuntimeExceptionDecorator
				.decorate(() -> ImageIconFactory.newImageIconFromSVG(imagePath, 16.f, 16.f));
		}
		return closedIcon;
	}
}
