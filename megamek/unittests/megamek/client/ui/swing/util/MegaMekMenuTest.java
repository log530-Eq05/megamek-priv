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
    void setUp()  {

        megaMekGUI = spy(new MegaMekGUI());


        mockFrame = new JFrame();
        Container contentPane = new JPanel();
        mockFrame.setContentPane(contentPane);



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
