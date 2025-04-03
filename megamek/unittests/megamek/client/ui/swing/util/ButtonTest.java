package megamek.client.ui.swing.util;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.MegaMekGUI;
import megamek.client.ui.swing.widget.MegaMekButton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.ActionListener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ButtonTest {

    private ActionListener mockListener;

    @BeforeEach
    void setUp() {
        MegaMekGUI megaMekGUI = mock(MegaMekGUI.class);
        mockListener = mock(ActionListener.class);
        Messages messagesMock = mock(Messages.class);
    }

    @Test
    void testCreateButton() {
        String labelKey = "testLabel";
        String actionCommand = "testCommand";


        MegaMekButton button = new MegaMekButton(labelKey, "MainMenuButton", true);
        button.setActionCommand(actionCommand);
        button.addActionListener(mockListener);

        assertNotNull(button);
        assertEquals("testLabel", button.getText());
        assertEquals("testCommand", button.getActionCommand());

        button.doClick();
        verify(mockListener, times(1)).actionPerformed(any());
    }
}
