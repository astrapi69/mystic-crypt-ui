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
package io.github.astrapi69.mystic.crypt.settings;

import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

/**
 * The theme of the JDK's Metal look and feel, which the View menu offers as two looks, "Metal" and
 * "Ocean", on the one class {@link MetalLookAndFeel} (#1065). Saved in settings.json as
 * {@code metalTheme} beside the look's name, so the start shows the one that was picked: without it
 * a saved "Metal" came back as the runtime's default theme, which is Ocean
 */
public enum MetalThemeChoice
{

	/** The "Metal" item of the menu: the JDK's original theme, {@link DefaultMetalTheme} */
	STEEL,

	/** The "Ocean" item of the menu, and the runtime's default theme: {@link OceanTheme} */
	OCEAN;

	/**
	 * The theme Metal draws with right now
	 *
	 * @return the theme of the runtime
	 */
	public static MetalThemeChoice ofTheRuntime()
	{
		return MetalLookAndFeel.getCurrentTheme() instanceof OceanTheme ? OCEAN : STEEL;
	}

	/**
	 * Makes this the theme Metal draws with; it takes effect when Metal is set as the look and feel
	 * afterwards
	 */
	public void install()
	{
		MetalLookAndFeel
			.setCurrentTheme(this == OCEAN ? new OceanTheme() : new DefaultMetalTheme());
	}
}
