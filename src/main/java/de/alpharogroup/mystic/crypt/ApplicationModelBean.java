package de.alpharogroup.mystic.crypt;

import de.alpharogroup.mystic.crypt.panels.signin.MasterPwFileModelBean;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * The class {@link ApplicationModelBean} holds application specific data
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationModelBean
{
	MasterPwFileModelBean masterPwFileModelBean;
	
	
}
