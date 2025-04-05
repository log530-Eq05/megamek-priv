/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.Team;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;

public class PlayerListDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 7270469195373150106L;

    private JList<String> playerList = new JList<>(new DefaultListModel<>());

    private Client client;
    private JButton butOkay;
    private boolean modal;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public PlayerListDialog(JFrame parent, Client client, boolean modal) {
        super(parent, "", false);
        this.setTitle(Messages.getString("PlayerListDialog.title"));
        this.client = client;
        this.modal = modal;

        add(playerList, BorderLayout.CENTER);
        add(Box.createHorizontalStrut(20), BorderLayout.LINE_START);
        add(Box.createHorizontalStrut(20), BorderLayout.LINE_END);

        butOkay = new JButton(Messages.getString("Okay"));
        butOkay.addActionListener(this);
        add(butOkay, BorderLayout.PAGE_END);

        // closing the window is the same as hitting butOkay
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                actionPerformed(new ActionEvent(butOkay,
                      ActionEvent.ACTION_PERFORMED, butOkay.getText()));
            }
        });

        refreshPlayerList();
        setMinimumSize(new Dimension(300, 260));
        pack();
        setResizable(false);

        if (modal) {
            setModal(true);
            setLocation(parent.getLocation().x + (parent.getSize().width / 2) - (getSize().width / 2),
                  parent.getLocation().y + (parent.getSize().height / 2) - (getSize().height / 2));
        } else {
            setModal(false);
            setLocation(GUIP.getPlayerListPosX(), GUIP.getPlayerListPosY());
        }
    }

    public void refreshPlayerList(JList<String> playerList,
          Client client) {
        refreshPlayerList(playerList, client, false);
    }

    /** @return The game's players list sorted by id. */
    private static List<Player> sortedPlayerList(IGame game) {
        List<Player> playerList = game.getPlayersList();
        playerList.sort(Comparator.comparingInt(Player::getId));
        return playerList;
    }

    /**
     * Part Refractored below
     */
    public void refreshPlayerList(JList<String> playerList, Client client, boolean displayTeam) {
        DefaultListModel<String> model = (DefaultListModel<String>) playerList.getModel();
        model.removeAllElements();

        for (Player player : sortedPlayerList(client.getGame())) {
            model.addElement(getPlayerDisplayString(player, client, displayTeam));
        }
    }

    private String getPlayerDisplayString(Player player, Client client, boolean displayTeam) {
        StringBuffer playerDisplay = new StringBuffer(String.format("%-12s", player.getName()));

        playerDisplay.append(getTeamInfo(player, client, displayTeam));
        playerDisplay.append(getPlayerTypeInfo(player));
        playerDisplay.append(getVisibilityInfo(player));

        return playerDisplay.toString();
    }

    private String getTeamInfo(Player player, Client client, boolean displayTeam) {
        if (!displayTeam) return "";

        Team team = client.getGame().getTeamForPlayer(player);
        if (team == null) {
            return Messages.getString("PlayerListDialog.TeamLess");
        } else if (team.getId() == Player.TEAM_NONE) {
            return Messages.getString("PlayerListDialog.NoTeam");
        } else {
            return MessageFormat.format(Messages.getString("PlayerListDialog.Team"), team.getId());
        }
    }

    private String getPlayerTypeInfo(Player player) {
        StringBuilder info = new StringBuilder();

        if (player.isGameMaster()) {
            info.append(Messages.getString("PlayerListDialog.player_gm"));
        }

        if (player.isGhost()) {
            info.append(Messages.getString("PlayerListDialog.player_ghost"));
        } else if (player.isBot()) {
            info.append(Messages.getString("PlayerListDialog.player_bot"));
        } else {
            info.append(Messages.getString("PlayerListDialog.player_human"));
        }

        if (player.isObserver()) {
            info.append(Messages.getString("PlayerListDialog.player_observer"));
        } else if (player.isDone()) {
            info.append(Messages.getString("PlayerListDialog.player_done"));
        }

        return info.toString();
    }

    private String getVisibilityInfo(Player player) {
        StringBuilder visibilityInfo = new StringBuilder();

        if (player.getSeeAll()) {
            visibilityInfo.append(Messages.getString("PlayerListDialog.player_seeall"));
        }
        if (player.getSingleBlind()) {
            visibilityInfo.append(Messages.getString("PlayerListDialog.player_singleblind"));
        }
        if (player.canIgnoreDoubleBlind()) {
            visibilityInfo.append(Messages.getString("PlayerListDialog.player_ignoreDoubleBlind"));
        }

        return visibilityInfo.toString();
    }
    /**
     * Part Refractored above
     */

    public void refreshPlayerList() {
        refreshPlayerList(playerList, client, true);
        pack();
    }

    public Player getSelected() {
        if (!playerList.isSelectionEmpty()) {
            return sortedPlayerList(client.getGame()).get(playerList.getSelectedIndex());
        }

        return null;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(butOkay)) {
            setVisible(false);
            if (!modal) {
                GUIP.setPlayerListEnabled(false);
            }
        }
    }

    public void saveSettings() {
        GUIP.setPlayerListPosX(getLocation().x);
        GUIP.setPlayerListPosY(getLocation().y);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if ((e.getID() == WindowEvent.WINDOW_DEACTIVATED) || (e.getID() == WindowEvent.WINDOW_CLOSING)) {
            if (!modal) {
                saveSettings();
            }
        }
    }
}

