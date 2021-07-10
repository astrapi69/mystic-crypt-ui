package io.github.astrapi69.design.pattern.state.component;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * The abstact class {@link ComponentStateMachine} can provide states on buttons. For an example see
 * the unit tests
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
	S current;
	@NonNull C component;

	protected abstract void updateComponentState();

	protected abstract void setEnabled(boolean enabled);
}
