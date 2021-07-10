package io.github.astrapi69.design.pattern.state.component;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.swing.*;

/**
 * The abstact class {@link AbstractJComponentStateMachine}
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@NoArgsConstructor
@SuperBuilder
public abstract class AbstractJComponentStateMachine<T extends JComponent, S> extends ComponentStateMachine<T, S>
{
	public void setEnabled(boolean b)
	{
		component.setEnabled(b);
	}
}
