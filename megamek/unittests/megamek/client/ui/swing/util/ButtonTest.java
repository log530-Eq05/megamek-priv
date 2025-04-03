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
    private MegaMekGUI megaMekGUI;
    private ActionListener mockListener;


    @BeforeEach
    void setUp() {
        megaMekGUI = mock(MegaMekGUI.class);
        mockListener = mock(ActionListener.class);

        when(megaMekGUI.createButton(anyString(), anyString()))
              .thenAnswer(invocation -> new MegaMekButton(invocation.getArgument(0), invocation.getArgument(1), true));
    }

    @Test
    void testCreateButton() {
        String labelKey = "testLabel";
        String actionCommand = "testCommand";

        MegaMekButton button = megaMekGUI.createButton(labelKey, "MainMenuButton");
        button.setActionCommand(actionCommand);
        button.addActionListener(mockListener);

        assertNotNull(button);
        assertEquals(labelKey, button.getText());
        assertEquals(actionCommand, button.getActionCommand());

        button.doClick();
        verify(mockListener, times(1)).actionPerformed(any());
    }

    @Test
    void testCreateButtonWithoutLabel() {
        MegaMekButton button = megaMekGUI.createButton("", "MainMenuButton");

        assertNotNull(button);
        assertEquals("", button.getText());
    }

    @Test
    void testButtonClickMultipleTimes() {
        MegaMekButton button = megaMekGUI.createButton("MultiClickButton", "MainMenuButton");
        button.addActionListener(mockListener);

        button.doClick();
        button.doClick();
        button.doClick();

        verify(mockListener, times(3)).actionPerformed(any());
    }

}
