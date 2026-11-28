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
package io.github.astrapi69.mystic.crypt.ui;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Asks for the menu-designer plugin's menu entry for one test, the way a developer does: with the
 * property the plugin looks at. The tool is for development and an installation does not receive it
 * (architecture.md), so without the property the plugin contributes nothing. The property's
 * previous value is put back after the test.
 */
final class MenuDesignerShown implements BeforeEachCallback, AfterEachCallback
{

	/** The property the plugin looks at, kept here rather than depending on the plugin's class */
	static final String DEVELOPER_PROPERTY = "mystic.crypt.ui.menu.designer";

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
		.create(MenuDesignerShown.class);

	/** The property's value before the test, {@code null} when it was not set */
	private record Original(String value) {
	}

	@Override
	public void beforeEach(final ExtensionContext context)
	{
		context.getStore(NAMESPACE).put(Original.class,
			new Original(System.getProperty(DEVELOPER_PROPERTY)));
		System.setProperty(DEVELOPER_PROPERTY, "true");
	}

	@Override
	public void afterEach(final ExtensionContext context)
	{
		Original original = context.getStore(NAMESPACE).remove(Original.class, Original.class);
		if (original == null || original.value() == null)
		{
			System.clearProperty(DEVELOPER_PROPERTY);
		}
		else
		{
			System.setProperty(DEVELOPER_PROPERTY, original.value());
		}
	}
}
