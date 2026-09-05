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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.model.api.IModel;
import io.github.astrapi69.swing.base.BasePanel;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * A component that was placed but never bound looks exactly like a bound one: it takes typing,
 * shows what was typed, and writes it into nothing.
 * <p>
 * Asking the component whether it has a property model does not tell the two apart - the model
 * components give themselves an empty one in their constructor, so the answer is always yes. What
 * tells them apart is whether a change reaches the model object, and these tests pin that the check
 * finds a component that does not, because a check that cannot report a miss is worth nothing.
 */
class ModelBindingTest
{

	/** What the probe panel holds */
	static final class Held
	{
		private String text;

		private String other;

		private boolean flag;

		private String choice;

		String getText()
		{
			return text;
		}

		void setText(final String text)
		{
			this.text = text;
		}

		String getOther()
		{
			return other;
		}

		void setOther(final String other)
		{
			this.other = other;
		}

		boolean isFlag()
		{
			return flag;
		}

		void setFlag(final boolean flag)
		{
			this.flag = flag;
		}

		String getChoice()
		{
			return choice;
		}

		void setChoice(final String choice)
		{
			this.choice = choice;
		}
	}

	/**
	 * A panel with one of everything, every component bound.
	 * <p>
	 * Whether the second field is bound is decided by an overridable method rather than by a field:
	 * the base class initializes the components from its own constructor, which runs before any
	 * field of a subclass is assigned - a flag would still be false at that moment.
	 */
	static class ProbePanel extends BasePanel<Held>
	{
		private JMTextField txtBound;

		private JMTextField txtForgotten;

		private JMCheckBox cbxFlag;

		private JMComboBox<String, DefaultComboBoxModel<String>> cmbChoice;

		ProbePanel(final IModel<Held> model)
		{
			super(model);
		}

		/** Binds the second field; the forgetful panel below does not */
		protected void bindTheOtherField()
		{
			txtForgotten.setPropertyModel(
				LambdaModel.of(() -> getModelObject().getOther(), getModelObject()::setOther));
		}

		@Override
		protected void onInitializeComponents()
		{
			super.onInitializeComponents();
			txtBound = new JMTextField();
			txtBound.setName("txtBound");
			txtBound.setPropertyModel(
				LambdaModel.of(() -> getModelObject().getText(), getModelObject()::setText));
			txtForgotten = new JMTextField();
			txtForgotten.setName("txtForgotten");
			bindTheOtherField();
			cbxFlag = new JMCheckBox();
			cbxFlag.setName("cbxFlag");
			cbxFlag.setPropertyModel(
				LambdaModel.of(() -> getModelObject().isFlag(), getModelObject()::setFlag));
			cmbChoice = new JMComboBox<>(new DefaultComboBoxModel<>(new String[] { "one", "two" }));
			cmbChoice.setName("cmbChoice");
			cmbChoice.setPropertyModel(
				LambdaModel.of(() -> getModelObject().getChoice(), getModelObject()::setChoice));
			add(txtBound);
			add(txtForgotten);
			add(cbxFlag);
			add(cmbChoice);
		}
	}

	/** The same panel with the second field left unbound, the way it is forgotten in practice */
	static final class ForgetfulProbePanel extends ProbePanel
	{
		ForgetfulProbePanel(final IModel<Held> model)
		{
			super(model);
		}

		@Override
		protected void bindTheOtherField()
		{
			// forgotten on purpose
		}
	}

	private static ProbePanel probePanel(final boolean bindTheField)
	{
		return bindTheField
			? new ProbePanel(BaseModel.of(new Held()))
			: new ForgetfulProbePanel(BaseModel.of(new Held()));
	}

	@Test
	@DisplayName("a field that reaches nothing is reported by its name")
	void aFieldThatReachesNothingIsReportedByItsName()
	{
		assertEquals(List.of("txtForgotten"), ModelBinding.unboundComponentsOf(probePanel(false)),
			"the field that writes into nothing was not found");
	}

	@Test
	@DisplayName("a panel whose components all reach the model reports nothing")
	void aPanelWhoseComponentsAllReachTheModelReportsNothing()
	{
		List<String> unbound = ModelBinding.unboundComponentsOf(probePanel(true));

		assertTrue(unbound.isEmpty(), "a fully bound panel reported: " + unbound);
	}

	@Test
	@DisplayName("the check puts back what it changed")
	void theCheckPutsBackWhatItChanged()
	{
		ProbePanel panel = probePanel(true);
		panel.getModelObject().setText("what was there");
		panel.txtBound.setText("what was there");
		boolean flagBefore = panel.cbxFlag.isSelected();

		ModelBinding.unboundComponentsOf(panel);

		assertEquals("what was there", panel.txtBound.getText(),
			"the check left its probe value in the field");
		assertEquals(flagBefore, panel.cbxFlag.isSelected(),
			"the check left the check box the other way round");
	}

	@Test
	@DisplayName("a component that carries no model at all is not the question")
	void aComponentThatCarriesNoModelAtAllIsNotTheQuestion()
	{
		assertFalse(ModelBinding.isModelComponent(new JTextField()),
			"a plain swing component must not be asked about a binding it cannot have");
	}

	/**
	 * A panel whose unbound field the user cannot type into, the shape of a field filled by a file
	 * chooser or a generator: the binding is the only path from the component into the model
	 */
	static final class ReadOnlyProbePanel extends BasePanel<Held>
	{
		private JMTextField txtReadOnlyAndForgotten;

		ReadOnlyProbePanel(final IModel<Held> model)
		{
			super(model);
		}

		@Override
		protected void onInitializeComponents()
		{
			super.onInitializeComponents();
			txtReadOnlyAndForgotten = new JMTextField();
			txtReadOnlyAndForgotten.setName("txtReadOnlyAndForgotten");
			txtReadOnlyAndForgotten.setEditable(false);
			add(txtReadOnlyAndForgotten);
		}
	}

	@Test
	@DisplayName("a field the user cannot type into is checked like every other one")
	void aFieldTheUserCannotTypeIntoIsCheckedLikeEveryOtherOne()
	{
		assertEquals(List.of("txtReadOnlyAndForgotten"),
			ModelBinding.unboundComponentsOf(new ReadOnlyProbePanel(BaseModel.of(new Held()))),
			"a non editable model component was not asked whether it reaches the model - "
				+ "a field filled by a chooser or a generator is bound by nothing else");
	}

	@Test
	@DisplayName("a subclass of a model component is still a model component")
	void aSubclassOfAModelComponentIsStillAModelComponent()
	{
		assertTrue(ModelBinding.isModelComponent(new JMTextField()
		{
		}), "an anonymous subclass slipped past the check");
	}

}
