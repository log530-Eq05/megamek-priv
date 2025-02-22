/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */
package megamek.ai.dataset;

import megamek.common.Entity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Parses a dataset from one or more game_action tsv files and turns it into a training dataset.</p>
 * <p>The dataset currently expected is the {@code game_actions*.tsv} file generated by the host of the game if the
 * option is enabled</p>
 * <p>This Dataset Parser handles multiple files by ingesting them one after the other, and changing the ID of each unit by an offset,
 * allowing to load the full data of all datasets as if it were a single one.</p>
 * @author Luana Coppio
 */
public class DatasetParser {

    private enum LineType {
        MOVE_ACTION_HEADER_V1("PLAYER_ID\tENTITY_ID\tCHASSIS\tMODEL\tFACING\tFROM_X\tFROM_Y\tTO_X\tTO_Y\tHEXES_MOVED\tDISTANCE" +
            "\tMP_USED\tMAX_MP\tMP_P\tHEAT_P\tARMOR_P\tINTERNAL_P\tJUMPING\tPRONE\tLEGAL\tSTEPS"),
        MOVE_ACTION_HEADER_V2(UnitActionField.getPartialHeaderLine(0, 22)),
        MOVE_ACTION_HEADER_V3(UnitActionField.getHeaderLine()),
        STATE_HEADER_V1("ROUND\tPHASE\tPLAYER_ID\tENTITY_ID\tCHASSIS\tMODEL\tTYPE\tROLE\tX\tY\tFACING\tMP\tHEAT\tPRONE\tAIRBORNE" +
            "\tOFF_BOARD\tCRIPPLED\tDESTROYED\tARMOR_P\tINTERNAL_P\tDONE"),
        STATE_HEADER_V2("ROUND\tPHASE\tTEAM_ID\tPLAYER_ID\tENTITY_ID\tCHASSIS\tMODEL\tTYPE\tROLE\tX\tY\tFACING\tMP\tHEAT\tPRONE" +
            "\tAIRBORNE\tOFF_BOARD\tCRIPPLED\tDESTROYED\tARMOR_P\tINTERNAL_P\tDONE"),
        STATE_HEADER_V3(UnitStateField.getHeaderLine()),
        ATTACK_ACTION_HEADER("PLAYER_ID\tENTITY_ID\tCHASSIS\tMODEL\tFACING\tFROM_X\tFROM_Y\tTO_X\tTO_Y\tHEXES_MOVED\tDISTANCE" +
            "\tMP_USED\tMAX_MP\tMP_P\tHEAT_P\tARMOR_P\tINTERNAL_P\tJUMPING\tPRONE\tLEGAL\tSTEPS"),
        ROUND("ROUND"),
        ACTION_HEADER("PLAYER_ID\tENTITY_ID");


        private final String text;
        LineType(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    // attackAction is not being considered right now
    private final List<ActionAndState> actionAndStates = new ArrayList<>();
    private final Map<Integer, Entity> entities = new HashMap<>();
    private final UnitActionSerde unitActionSerde = new UnitActionSerde();
    private final UnitStateSerde unitStateSerde = new UnitStateSerde();
    private int idOffset = 0;
    private int highestEntityId = 0;

    /**
     * Parses a dataset from a file. Can be chained with other parse calls to create a large single training dataset.
     * @param file The file to parse
     * @return The parser instance
     */
    public DatasetParser parse(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(LineType.MOVE_ACTION_HEADER_V1.getText()) || line.startsWith(LineType.MOVE_ACTION_HEADER_V2.getText())
                    || line.startsWith(LineType.MOVE_ACTION_HEADER_V3.getText())) {
                    // Parse action line
                    String actionLine = reader.readLine();
                    if (actionLine == null) break;
                    UnitAction action = parseActionLine(actionLine);

                    // Parse state block
                    line = reader.readLine(); // State header
                    if (line == null) {
                        throw new RuntimeException("Invalid line after action: " + actionLine);
                    } else if (!line.startsWith(LineType.STATE_HEADER_V1.getText()) && !line.startsWith(LineType.STATE_HEADER_V2.getText())
                        && !line.startsWith(LineType.STATE_HEADER_V3.getText())) {
                        throw new RuntimeException("Invalid state header after action: " + line);
                    }

                    List<UnitState> states = new ArrayList<>();
                    Integer currentRound = null;
                    while ((line = reader.readLine()) != null && (!line.startsWith(LineType.ACTION_HEADER.getText()))) {
                        if (line.trim().isEmpty()) continue;
                        if (line.startsWith(LineType.ROUND.getText())) break;
                        UnitState state = parseStateLine(line);
                        if (currentRound == null) {
                            currentRound = state.round();
                        } else if (currentRound != state.round()) {
                            throw new RuntimeException("State block has inconsistent rounds");
                        }
                        states.add(state);
                    }

                    if (currentRound == null) {
                        throw new RuntimeException("State block has no valid states");
                    }
                    actionAndStates.add(new ActionAndState(currentRound, action, states));

                    // If line is an action header, the outer loop will handle it
                    // we are not currently parsing and using attack actions on the dataset right now.
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading file " + file.getName(), e);
        }

        idOffset = highestEntityId + 1;
        return this;
    }

    /**
     * Get the training dataset from the parsed files.
     * This will throw an exception if no actions and states were found or no parsing was done.
     * @return The training dataset
     */
    public TrainingDataset getTrainingDataset() {
        if (actionAndStates.isEmpty()) {
            throw new RuntimeException("No actions and states found in dataset, you need to parse a file first");
        }
        return new TrainingDataset(actionAndStates);
    }

    private UnitAction parseActionLine(String actionLine) {
        try {
            UnitAction unitAction = unitActionSerde.fromTsv(actionLine, idOffset);
            highestEntityId = Math.max(highestEntityId, unitAction.id());
            return unitAction;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing action line: " + actionLine, e);
        }
    }

    private UnitState parseStateLine(String stateLine) {
        try {
            UnitState unitState = unitStateSerde.fromTsv(stateLine, entities, idOffset);
            highestEntityId = Math.max(highestEntityId, unitState.id());
            return unitState;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing state line: " + stateLine, e);
        }
    }
}
