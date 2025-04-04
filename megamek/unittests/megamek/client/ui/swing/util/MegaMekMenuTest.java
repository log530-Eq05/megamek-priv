package megamek.client.ui.swing.util;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.MegaMekGUI;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinSpecification.UIComponents;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.spy;


public class MegaMekMenuTest {
    private MegaMekGUI megaMekGUI;
    private JFrame mockFrame;



    @BeforeEach
    void setUp() throws Exception {

        megaMekGUI = spy(new MegaMekGUI());


        mockFrame = new JFrame();
        Container contentPane = new JPanel();
        mockFrame.setContentPane(contentPane);

        var frameField = MegaMekGUI.class.getDeclaredField("frame");
        frameField.setAccessible(true);
        frameField.set(megaMekGUI, mockFrame);
    }

    @Test
    void testShowMainMenuAddsComponentsToFrame() {
       megaMekGUI.showMainMenu();

        Component[] components = mockFrame.getContentPane().getComponents();

        assertNotNull(components, "Content pane components should not be null");
        assertTrue(components.length > 0, "Content pane should have at least one component");

        boolean hasButton = false;
        for (Component comp : components) {
            if (comp instanceof JButton ) {
                hasButton = true;
                break;
            }
        }

        assertTrue(hasButton, "Main menu should contain at least one button");
    }
    @Test
    void testCreateVersionLabel() {
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(UIComponents.MainMenuBorder.getComp(), true);
        JLabel label = megaMekGUI.createVersionLabel(skinSpec);

        assertNotNull(label);
        assertEquals(Messages.getString("MegaMek.Version") + MMConstants.VERSION, label.getText());
        assertEquals(JLabel.CENTER, label.getHorizontalAlignment());
        assertEquals(new Dimension(250, 15), label.getPreferredSize());
    }
}
