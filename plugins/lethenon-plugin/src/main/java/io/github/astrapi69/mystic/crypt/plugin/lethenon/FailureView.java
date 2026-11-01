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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import net.miginfocom.swing.MigLayout;

/**
 * Where a lethenon window shows a failure (draft 07, #710): the cause in bold, what to do below it,
 * the technical reason behind "Show details", closed at first. Everything wraps instead of
 * widening the window, and while a failure shows, the window's answer fields are hidden, so no
 * empty result stands next to it (README defects G4, G5)
 */
final class FailureView extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JTextArea txtFailureCause = LethenonWalletPanel.message("txtFailureCause", true);

	private final JTextArea txtFailureAction = LethenonWalletPanel.message("txtFailureAction", false);

	private final JTextArea txtFailureDetails = LethenonWalletPanel.message("txtFailureDetails",
		false);

	private final JButton btnFailureDetails;

	private final transient List<Component> answer;

	/** Whether each answer component was visible before a failure hid it */
	private final transient Map<Component, Boolean> visibleBefore = new HashMap<>();

	private final Locale locale;

	/**
	 * Instantiates a new {@link FailureView}
	 *
	 * @param answer
	 *            the window's answer components, hidden while a failure shows
	 * @param locale
	 *            the language of the window
	 */
	private FailureView(final List<Component> answer, final Locale locale)
	{
		super(ToolForm.newLayout());
		this.answer = List.copyOf(answer);
		this.locale = locale;
		setName("pnlFailure");
		txtFailureCause.setToolTipText(text("lethenon.failure.tooltip.cause", "what went wrong"));
		txtFailureAction.setToolTipText(text("lethenon.failure.tooltip.action", "what to do now"));
		txtFailureDetails.setToolTipText(text("lethenon.failure.tooltip.details",
			"the technical reason, as the program reported it"));
		btnFailureDetails = LethenonSwing.button("btnFailureDetails",
			AnswerFields.detailsButtonText(false, locale), event -> onDetails(),
			text("lethenon.failure.tooltip.details.button",
				"show or hide the technical reason"));
		add(txtFailureCause, ToolForm.WIDE + ", wmin 0");
		add(txtFailureAction, ToolForm.WIDE + ", wmin 0");
		add(ToolForm.buttons(btnFailureDetails), ToolForm.BUTTON_ROW);
		add(txtFailureDetails, ToolForm.WIDE + ", wmin 0, hidemode 3");
		txtFailureDetails.setVisible(false);
		setVisible(false);
	}

	/**
	 * Adds the view to a window's form below its answer; the form hides invisible components
	 * completely from then on
	 *
	 * @param form
	 *            the window's form, laid out by {@link ToolForm}
	 * @param answer
	 *            the window's answer components
	 * @param locale
	 *            the language of the window
	 * @return the view
	 */
	static FailureView below(final JPanel form, final List<Component> answer, final Locale locale)
	{
		MigLayout layout = (MigLayout)form.getLayout();
		layout.setLayoutConstraints(layout.getLayoutConstraints() + ", hidemode 3");
		FailureView view = new FailureView(answer, locale);
		form.add(view, ToolForm.WIDE + ", wmin 0, hidemode 3");
		return view;
	}

	/**
	 * Shows a failure and hides the answer
	 *
	 * @param failure
	 *            the failure
	 */
	void show(final Failure failure)
	{
		txtFailureCause.setText(failure.cause());
		txtFailureAction.setText(failure.action());
		txtFailureDetails.setText(failure.details());
		txtFailureDetails.setVisible(false);
		btnFailureDetails.setText(AnswerFields.detailsButtonText(false, locale));
		if (!isVisible())
		{
			answer.forEach(component -> visibleBefore.put(component, component.isVisible()));
		}
		answer.forEach(component -> component.setVisible(false));
		setVisible(true);
		revalidateTheForm();
	}

	/** Takes the failure away and shows the answer again */
	void clear()
	{
		if (!isVisible())
		{
			return;
		}
		setVisible(false);
		answer.forEach(component -> component
			.setVisible(visibleBefore.getOrDefault(component, Boolean.TRUE)));
		visibleBefore.clear();
		revalidateTheForm();
	}

	/** Opens or closes the technical reason */
	void onDetails()
	{
		boolean shown = !txtFailureDetails.isVisible();
		txtFailureDetails.setVisible(shown);
		btnFailureDetails.setText(AnswerFields.detailsButtonText(shown, locale));
		revalidateTheForm();
	}

	private void revalidateTheForm()
	{
		if (getParent() != null)
		{
			getParent().revalidate();
			getParent().repaint();
		}
	}

	private String text(final String key, final String defaultValue)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
	}
}
