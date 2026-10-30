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

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.Collection;
import java.util.Objects;

import org.assertj.swing.hierarchy.ComponentHierarchy;

/**
 * A component hierarchy that never hands out a null root (#484).
 * <p>
 * When a lookup finds nothing, AssertJ-Swing builds its failure message by printing every root of
 * the hierarchy, and {@code BasicComponentPrinter} refuses a null root with a
 * {@link NullPointerException} - which then stands where the message should, so what the lookup
 * wanted is never said. In 3.17.1 the printer cannot be replaced: it is a final field the finder
 * builds from the hierarchy it is given. So the robot gets this hierarchy instead, and the finder
 * and its printer both see the roots without the null ones. Everything else is handed through.
 */
final class NullRootsSkippingHierarchy implements ComponentHierarchy
{

	private final ComponentHierarchy delegate;

	NullRootsSkippingHierarchy(final ComponentHierarchy delegate)
	{
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public Collection<Container> roots()
	{
		return delegate.roots().stream().filter(Objects::nonNull).toList();
	}

	@Override
	public Collection<Component> childrenOf(final Component component)
	{
		return delegate.childrenOf(component);
	}

	@Override
	public Container parentOf(final Component component)
	{
		return delegate.parentOf(component);
	}

	@Override
	public boolean contains(final Component component)
	{
		return delegate.contains(component);
	}

	@Override
	public void dispose(final Window window)
	{
		delegate.dispose(window);
	}
}
