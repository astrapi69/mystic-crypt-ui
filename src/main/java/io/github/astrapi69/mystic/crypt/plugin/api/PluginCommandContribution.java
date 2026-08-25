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
package io.github.astrapi69.mystic.crypt.plugin.api;

import java.util.List;

import org.pf4j.ExtensionPoint;

/**
 * Extension point that lets a plugin contribute command line commands, so a plugin is reachable
 * without the user interface: {@code java -jar mystic-crypt-ui-all.jar --cli <command> ...}.
 * <p>
 * The returned objects are picocli commands - classes annotated with {@code @Command} that
 * implement {@link Runnable} or {@code Callable<Integer>}. They are declared as {@link Object} on
 * purpose: that keeps this interface free of a compile time dependency on picocli, so a plugin that
 * only contributes menu items never has to know that the command line exists at all.
 * <p>
 * The commands of the mystic-crypt library are always available and need no contribution; this
 * extension point is for what a plugin adds on top of them.
 */
public interface PluginCommandContribution extends ExtensionPoint
{

	/**
	 * Gets the picocli commands this plugin contributes
	 *
	 * @return the commands to add, never {@code null}, empty if this plugin contributes nothing
	 */
	List<Object> getCommands();

}
