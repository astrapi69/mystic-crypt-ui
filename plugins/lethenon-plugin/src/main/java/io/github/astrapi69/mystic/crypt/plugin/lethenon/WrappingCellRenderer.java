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

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableCellRenderer;

/**
 * Shows a cell's text wrapped at word boundaries over as many lines as it needs, and makes the row
 * tall enough for the tallest of its cells. A sentence in a table cell otherwise ends in an
 * ellipsis at the column's edge, and the "Before you start" view (#540) is sentences.
 * <p>
 * Neither swing-table-components nor swing-renderer has a wrapping renderer (searched 2026-10-08),
 * so it is written here.
 */
final class WrappingCellRenderer extends JTextArea implements TableCellRenderer
{

	private static final long serialVersionUID = 1L;

	/** Creates a renderer that wraps at word boundaries */
	WrappingCellRenderer()
	{
		setLineWrap(true);
		setWrapStyleWord(true);
		setOpaque(true);
		setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
	}

	@Override
	public Component getTableCellRendererComponent(final JTable table, final Object value,
		final boolean selected, final boolean focused, final int row, final int column)
	{
		setText(value == null ? "" : value.toString());
		setFont(table.getFont());
		setForeground(selected ? table.getSelectionForeground() : table.getForeground());
		setBackground(selected ? table.getSelectionBackground() : table.getBackground());
		setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);
		int height = getPreferredSize().height;
		if (table.getRowHeight(row) < height)
		{
			table.setRowHeight(row, height);
		}
		return this;
	}
}
