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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * The Lethenon chain as an internal plugin: it replays a chain file and reports what it verified.
 * <p>
 * Unlike every other plugin here, this one brings a library the application does not carry - the
 * chain itself, {@code io.github.astrapi69:lethenon}, packaged into this plugin's zip under
 * {@code lib/}. Everything the chain needs underneath (mystic-crypt, crypt-api, crypt-data) the
 * host already has, except the SQLite driver the chain is kept in since lethenon 0.6.0, which comes
 * in the same zip (#581).
 */
public class LethenonPlugin extends Plugin
{

	private static final Logger LOGGER = Logger.getLogger(LethenonPlugin.class.getName());

	/** The SQLite driver lethenon opens its chain database with */
	private static final String SQLITE_DRIVER = "org.sqlite.JDBC";

	/**
	 * Instantiates a new {@link LethenonPlugin}
	 *
	 * @param wrapper
	 *            the plugin wrapper the host passes in
	 */
	public LethenonPlugin(PluginWrapper wrapper)
	{
		super(wrapper);
	}

	@Override
	public void start()
	{
		registerSqliteDriver();
		LOGGER.info("Lethenon plugin started");
	}

	/**
	 * Initialises the SQLite driver through this plugin's class loader, which registers it with
	 * {@link java.sql.DriverManager}. lethenon asks {@code DriverManager} for its connection, and
	 * {@code DriverManager} looks for drivers only through the application's class loader, which
	 * does not see the driver in this plugin's zip. Workaround for lethenon#223, removed once a
	 * lethenon release opens the connection through the driver itself.
	 */
	private static void registerSqliteDriver()
	{
		try
		{
			Class.forName(SQLITE_DRIVER);
		}
		catch (ClassNotFoundException missing)
		{
			LOGGER.log(Level.SEVERE, SQLITE_DRIVER + " is not in this plugin's zip, so no chain "
				+ "file can be opened: reinstall the lethenon plugin", missing);
		}
	}

	@Override
	public void stop()
	{
		LOGGER.info("Lethenon plugin stopped");
	}
}
