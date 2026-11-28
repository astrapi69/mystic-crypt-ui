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
package io.github.astrapi69.mystic.crypt.plugin.wifiqr;

import java.util.logging.Logger;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * The Wi-Fi QR plugin (#1120): it shows a Wi-Fi entry of the vault as a QR code a phone scans to
 * join the network. It brings no menu; its one action sits in the context menu of the entry table
 */
public class WifiQrPlugin extends Plugin
{

	private static final Logger LOGGER = Logger.getLogger(WifiQrPlugin.class.getName());

	/**
	 * Instantiates the plugin
	 *
	 * @param wrapper
	 *            the plugin's wrapper, handed over by pf4j
	 */
	public WifiQrPlugin(final PluginWrapper wrapper)
	{
		super(wrapper);
	}

	@Override
	public void start()
	{
		LOGGER.info("Wi-Fi QR plugin started");
	}

	@Override
	public void stop()
	{
		LOGGER.info("Wi-Fi QR plugin stopped");
	}
}
