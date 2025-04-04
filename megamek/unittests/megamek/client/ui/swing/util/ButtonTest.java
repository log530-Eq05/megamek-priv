package megamek.client.ui.swing.util;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.MegaMekGUI;
import megamek.client.ui.swing.widget.MegaMekButton;

import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinSpecification.UIComponents;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ButtonTest {
    private MegaMekGUI megaMekGUI;
    private ActionListener mockListener;


    @BeforeEach
    void setUp() {
        megaMekGUI = spy(MegaMekGUI.class);
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
    void testCreateMenuButtonsReturnsNonEmptyListOfButtons() {
        List<MegaMekButton> buttons = megaMekGUI.createMenuButtons();

        assertNotNull(buttons, "Button list should not be null");
        assertFalse(buttons.isEmpty(), "Button list should not be empty");

        for (MegaMekButton button : buttons) {
            assertNotNull(button, "Button should not be null");
            assertTrue(button instanceof MegaMekButton, "Each item should be a MegaMekButton");
        }
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

    @Test
    void testCalculateButtonSize() {
        List<MegaMekButton> buttons = megaMekGUI.createMenuButtons();
        Dimension screenSize = new Dimension(1024, 768);
        JLabel mockSplash = mock(JLabel.class);
        when(mockSplash.getPreferredSize()).thenReturn(new Dimension(500, 300));

        Dimension buttonSize = megaMekGUI.calculateButtonSize(buttons, screenSize, mockSplash);
        assertNotNull(buttonSize);
        assertTrue(buttonSize.getWidth() > 0);
        assertTrue(buttonSize.getHeight() > 0);
    }


}
