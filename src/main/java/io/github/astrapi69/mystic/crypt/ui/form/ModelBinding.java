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
package io.github.astrapi69.mystic.crypt.ui.form;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import io.github.astrapi69.swing.base.BasePanel;

/**
 * Answers whether the components of a panel really write into its model.
 * <p>
 * A component that was placed but never bound looks exactly like a bound one: it takes typing,
 * shows what was typed, and writes it into nothing. Whatever reads the model afterwards - a button,
 * a test, the command line side - then finds an empty value, far away from where the mistake was
 * made.
 * <p>
 * Asking a component whether it has a property model does not answer this: the model components
 * give themselves an empty one in their constructor, so every component has one whether it was
 * bound or not. What can be answered is whether changing the component changes the panel's model
 * object, which is the thing the binding is for. That is what this does: it puts a value into the
 * component, looks at the model object before and after, and puts the old value back.
 */
public final class ModelBinding
{

	/** The package the model backed components come from */
	private static final String MODEL_COMPONENT_PACKAGE = "io.github.astrapi69.swing.model.component";

	private ModelBinding()
	{
	}

	/**
	 * The model components of the given panel that do not reach its model object.
	 * <p>
	 * Components of a nested panel are left alone: that panel has a model of its own and is asked
	 * separately.
	 *
	 * @param panel
	 *            the panel to check
	 * @return the components that write into nothing, named by their component name, empty when
	 *         every one of them arrives
	 */
	public static List<String> unboundComponentsOf(final BasePanel<?> panel)
	{
		List<String> unbound = new ArrayList<>();
		for (Component component : modelComponentsOf(panel))
		{
			if (!writesIntoTheModelOf(panel, component))
			{
				unbound.add(nameOf(component));
			}
		}
		return unbound;
	}

	/**
	 * The model backed components of the given panel, nested containers included but nested panels
	 * excluded
	 *
	 * @param panel
	 *            the panel to look through
	 * @return the components
	 */
	public static List<Component> modelComponentsOf(final BasePanel<?> panel)
	{
		List<Component> found = new ArrayList<>();
		collect(panel, found);
		return found;
	}

	/**
	 * Whether changing the given component changes the model object of the given panel
	 *
	 * @param panel
	 *            the panel that holds the model
	 * @param component
	 *            the component to try
	 * @return true when the change arrives in the model
	 */
	public static boolean writesIntoTheModelOf(final BasePanel<?> panel, final Component component)
	{
		Object modelObject = panel.getModelObject();
		if (modelObject == null)
		{
			return false;
		}
		if (component instanceof JTextComponent textComponent)
		{
			return textReachesTheModel(panel, textComponent);
		}
		String before = stateOf(modelObject);
		Runnable putItBack = change(component);
		String after = stateOf(modelObject);
		putItBack.run();
		return !Objects.equals(before, after);
	}

	/**
	 * Whether the given component is one of the model backed components of this application, an
	 * anonymous subclass of one included
	 *
	 * @param component
	 *            the component to ask
	 * @return true when it is one
	 */
	public static boolean isModelComponent(final Component component)
	{
		for (Class<?> type = component.getClass(); type != null; type = type.getSuperclass())
		{
			Package typePackage = type.getPackage();
			if (typePackage != null && typePackage.getName().equals(MODEL_COMPONENT_PACKAGE))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The values a text field is offered, in turn, until one of them arrives in the model.
	 * <p>
	 * A field that parses what is typed - a serial number, a date, a distinguished name - turns a
	 * nonsense value away, and rightly so. Being turned away is not the same as not being bound, so
	 * a field that refuses one value is offered the next.
	 */
	private static final List<String> PROBE_VALUES = List.of("bound?" + UUID.randomUUID(), "12345",
		"CN=probe", java.time.ZonedDateTime.now().plusYears(5).toString(), "2038-01-19");

	/**
	 * Whether changing the given text field reaches the model of the given panel, trying every
	 * probe value before giving up
	 *
	 * @param panel
	 *            the panel that holds the model
	 * @param textComponent
	 *            the field to try
	 * @return true when one of the probe values arrives
	 */
	private static boolean textReachesTheModel(final BasePanel<?> panel,
		final JTextComponent textComponent)
	{
		Object modelObject = panel.getModelObject();
		String before = textComponent.getText();
		try
		{
			for (String probeValue : PROBE_VALUES)
			{
				String stateBefore = stateOf(modelObject);
				textComponent.setText(probeValue);
				if (!Objects.equals(stateBefore, stateOf(modelObject)))
				{
					return true;
				}
			}
			return false;
		}
		finally
		{
			textComponent.setText(before);
		}
	}

	private static Runnable changeText(final JTextComponent textComponent)
	{
		String before = textComponent.getText();
		textComponent.setText(PROBE_VALUES.get(0));
		return () -> textComponent.setText(before);
	}

	private static void collect(final Container container, final List<Component> found)
	{
		for (Component child : container.getComponents())
		{
			if (isModelComponent(child))
			{
				found.add(child);
			}
			// a nested panel carries a model of its own and answers for its own components
			if (child instanceof Container nested && !(child instanceof BasePanel))
			{
				collect(nested, found);
			}
		}
	}

	/**
	 * Puts a different value into the given component and hands back what puts the old one back
	 *
	 * @param component
	 *            the component to change
	 * @return the undo
	 */
	private static Runnable change(final Component component)
	{
		if (component instanceof JTextComponent textComponent)
		{
			return changeText(textComponent);
		}
		if (component instanceof JComboBox<?> comboBox)
		{
			Object before = comboBox.getSelectedItem();
			for (int index = 0; index < comboBox.getItemCount(); index++)
			{
				if (!Objects.equals(comboBox.getItemAt(index), before))
				{
					int chosen = index;
					comboBox.setSelectedIndex(chosen);
					return () -> comboBox.setSelectedItem(before);
				}
			}
			return () -> {
			};
		}
		if (component instanceof javax.swing.AbstractButton button)
		{
			// clicked rather than set: a box bound through an action listener does not hear
			// setSelected, and would look unbound although it is not
			boolean before = button.isSelected();
			button.doClick();
			return () -> {
				if (button.isSelected() != before)
				{
					button.doClick();
				}
			};
		}
		if (component instanceof javax.swing.JSpinner spinner)
		{
			Object before = spinner.getValue();
			Object next = spinner.getModel().getNextValue();
			if (next != null)
			{
				spinner.setValue(next);
			}
			return () -> spinner.setValue(before);
		}
		return () -> {
		};
	}

	/**
	 * What the given model object holds right now, as text, so two moments can be compared
	 *
	 * @param modelObject
	 *            the object to read
	 * @return its state
	 */
	private static String stateOf(final Object modelObject)
	{
		StringBuilder state = new StringBuilder();
		for (Class<?> type = modelObject.getClass(); type != null
			&& !type.equals(Object.class); type = type.getSuperclass())
		{
			for (Field field : type.getDeclaredFields())
			{
				if (field.isSynthetic()
					|| java.lang.reflect.Modifier.isStatic(field.getModifiers()))
				{
					continue;
				}
				state.append(field.getName()).append('=').append(valueOf(field, modelObject))
					.append(';');
			}
		}
		return state.toString();
	}

	private static String valueOf(final Field field, final Object modelObject)
	{
		try
		{
			field.setAccessible(true);
			Object value = field.get(modelObject);
			if (value instanceof char[] characters)
			{
				return String.valueOf(characters);
			}
			return String.valueOf(value);
		}
		catch (final ReflectiveOperationException | RuntimeException thisFieldCannotBeRead)
		{
			// a field that cannot be read is the same in both snapshots and changes no answer
			return "?";
		}
	}

	private static String nameOf(final Component component)
	{
		String name = component.getName();
		return name != null && !name.isBlank()
			? name
			: component.getClass().getSimpleName() + " (unnamed)";
	}

}
