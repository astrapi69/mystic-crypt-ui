/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panels.obfuscate.character;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import de.alpharogroup.collections.list.ListFactory;
import de.alpharogroup.collections.pairs.KeyValuePair;
import io.github.astrapi69.crypto.obfuscation.rule.ObfuscationOperationRule;
import io.github.astrapi69.crypto.obfuscation.rule.Operation;
import io.github.astrapi69.swing.table.model.GenericTableModel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * The class {@link EditableCharacterObfuscationOperationRulesTableModel}.
 */
@Getter
@ToString(callSuper = true)
@Builder
public class EditableCharacterObfuscationOperationRulesTableModel
	extends
		GenericTableModel<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>>
{

	/** The Constant DELETE. */
	public static final String DELETE = "Delete";

	/** The Constant EDIT. */
	public static final String EDIT = "Edit";

	/** The Constant INDEXES. */
	public static final String INDEXES = "Indexes";

	/** The Constant REPLACE_WITH. */
	public static final String OPERATION = "Operation";

	/** The Constant ORIGINAL_CHAR. */
	public static final String ORIGINAL_CHAR = "Original character";

	/** The Constant REPLACE_WITH. */
	public static final String REPLACE_WITH = "Replace with";

	/**
	 * The constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/** The can edit. */
	private final boolean[] canEdit = new boolean[] { false, false, false, false, true, true };

	/** The column names. */
	private final String[] columnNames = { ORIGINAL_CHAR, REPLACE_WITH, INDEXES, OPERATION, EDIT,
			DELETE };

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> getColumnClass(final int c)
	{
		switch (c)
		{
			case 2 :
				return Set.class;
			case 3 :
				return Operation.class;
			case 4 :
			case 5 :
				return ObfuscationOperationRule.class;
			case 0 :
			case 1 :
			default :
				return Character.class;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getColumnCount()
	{
		return columnNames.length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getColumnName(final int col)
	{
		return columnNames[col];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getValueAt(final int row, final int col)
	{
		if (getData().size() > row)
		{
			final KeyValuePair<Character, ObfuscationOperationRule<Character, Character>> permission = getData()
				.get(row);
			ObfuscationOperationRule<Character,Character> value = permission.getValue();
			switch (col)
			{
				case 0 :
					return value.getCharacter();
				case 1 :
					return value.getReplaceWith();
				case 2 :
					return value.getIndexes();
				case 3 :
					return value.getOperation();
				case 4 :
				case 5 :
					return value;
				default :
					return null;
			}
		}
		return null;
	}

	public Optional<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> indexOf(
		Character character)
	{
		final List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> data = getData();
		for (final KeyValuePair<Character, ObfuscationOperationRule<Character, Character>> row : data)
		{
			if (row.getKey().equals(character))
			{
				return Optional.of(row);
			}
		}
		return Optional.empty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCellEditable(final int rowIndex, final int columnIndex)
	{
		return canEdit[columnIndex];
	}

	/**
	 * To map.
	 *
	 * @return the map
	 */
	public Map<Character, ObfuscationOperationRule<Character, Character>> toMap()
	{
		return KeyValuePair.toMap(ListFactory.newArrayList(getData()));
	}

	public List<KeyValuePair<Character, ObfuscationOperationRule<Character, Character>>> toList(BiMap<Character, ObfuscationOperationRule<Character, Character>> biMap){
		return KeyValuePair.toKeyValuePairs(biMap);
	}

	public BiMap<Character, ObfuscationOperationRule<Character, Character>> toBiMap(){
		return HashBiMap
			.create(toMap());
	}

}
