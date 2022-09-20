package io.github.astrapi69.mystic.crypt.panel.mousemove;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MouseMoveSettingsModelBean {
    @Builder.Default
    Integer xAxis = 1;
    @Builder.Default
    Integer yAxis = 1;
    @Builder.Default
    Integer intervalOfSeconds = 60;
}
