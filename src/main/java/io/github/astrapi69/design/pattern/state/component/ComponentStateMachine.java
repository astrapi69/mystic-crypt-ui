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
package io.github.astrapi69.design.pattern.state.component;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * The abstact class {@link ComponentStateMachine} can provide states on buttons. For an example see
 * the unit tests
 *
 * @param <C>
 *            the generic type of the component
 * @param <S>
 *            the generic type of the current state
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class ComponentStateMachine<C, S>
{

	/**
	 * the component
	 */
	@NonNull
	C component;

	/**
	 * the current state, can be a model object or a bean
	 */
	S currentState;

	/**
	 * update the component state
	 */
	protected abstract void updateComponentState();

	/**
	 * Sets the enabled flag for the component
	 * 
	 * @param enabled
	 *            the enabled flag for the component
	 */
	protected abstract void setEnabled(boolean enabled);
}
