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
package io.github.astrapi69.mystic.crypt.plugin.certificate;

import java.util.logging.Logger;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * Main class of the internal certificate plugin. The legacy {@code (PluginWrapper wrapper)}
 * constructor is required: it is what {@code DefaultPluginFactory} instantiates plugin classes with
 * in pf4j 3.15.0, despite being marked deprecated in favor of a not-yet-standard
 * {@code PluginContext} alternative.
 */
public class CertificatePlugin extends Plugin
{

	private static final Logger LOGGER = Logger.getLogger(CertificatePlugin.class.getName());

	public CertificatePlugin(PluginWrapper wrapper)
	{
		super(wrapper);
	}

	@Override
	public void start()
	{
		LOGGER.info("Certificate plugin started");
	}

	@Override
	public void stop()
	{
		LOGGER.info("Certificate plugin stopped");
	}

}
