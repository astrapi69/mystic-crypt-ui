package io.github.astrapi69.mystic.crypt.actions;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.swing.action.ToggleFullScreenAction;
import io.github.astrapi69.swing.layout.ScreenSizeExtensions;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ApplicationToggleFullScreenAction extends ToggleFullScreenAction {
    /**
     * Instantiates a new {@link ToggleFullScreenAction} object.
     *
     * @param name  the name
     * @param frame
     */
    public ApplicationToggleFullScreenAction(String name, JFrame frame) {
        super(name, frame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e)
    {
        ScreenSizeExtensions
                .toggleFullScreen(MysticCryptApplicationFrame.getInstance());
    }
}
