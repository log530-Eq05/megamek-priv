/*
 * Copyright (C) 2020 - The MegaMek Team
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import megamek.common.Compute;
import megamek.common.Configuration;
import megamek.common.Crew;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.MMLogger;
import megamek.common.util.MegaMekFile;
import megamek.common.util.WeightedMap;

/**
 * This class sets up a random name generator that can then
 * be used to generate random pilot names. it will have a couple different
 * settings and flexible input files
 *
 * Files are located in {@link Configuration#namesDir()}. All files are comma spaced csv files
 *
 * The historicalEthnicity.csv file shows the correspondence between the different ethnic names
 * and their numeric code in the database. This file is used to initialize the name mapping, and
 * must be kept current for all additions. The same numeric code MUST be used across all of the
 * files listed below.
 * The numeric codes MUST be listed in exact sequential order (NO skipped numbers) for the load
 * to work correctly.
 *
 * The name database is located in three files: maleGivenNames.csv, femaleGivenNames.csv,
 * and surnames.csv.
 *
 * The database is divided into three fields; an Integer Ethnic Code, a String name, and an Integer weight.
 * The Ethnic Code is an Integer identifying the ethnic group from the historicalEthnicity.csv file the name is from
 * The Name is a String containing either a male/female first name or a surname, dependant on the origin file.
 * The Weight is an Integer that is used to set the generation chance of the name. The higher the number,
 * the more common the name is.
 *
 * Faction files are located in factions subdirectory of {@link Configuration#namesDir()}
 * The faction key is the filename without the extension.
 * The faction files will have varying number of fields depending on how many ethnic groups exist.
 * The faction file does two things:
 * First, it identifies the relative frequency of different ethnic surnames for a faction.
 * Second, it identifies the correspondence between first names and surnames.
 * This allows, for example, for more Japanese first names regardless of surname in the Draconis
 * Combine. There MUST be a line in the Faction file for each ethnic group, although a weight of 0
 * can be used to prevent the generation of a grouping of names
 *
 * This is divided into 3 + n fields, where n is the number of ethnic groups listed in historicalEthnicity.csv,
 * divided into the following groupings:
 * The Integer Ethnic Code is the first field
 * The String Ethnic Name is the second field. This is included for ease of reference, and
 * is NOT used by the generator.
 * The Integer Weight for generating a surname of the specified ethnicity. The higher the number,
 * the more common the surname is for a faction.
 * This is followed by n fields each containing the Integer Weight for generating a given name for
 * the ethnicity with Ethnic Code n. The higher the number for the weight, the more common that
 * given name ethnicity is in generation for the specific ethnicity of the generated surname.
 *
 * @author Jay Lawson (original), Justin Bowen (current version)
 */
public class RandomNameGenerator implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 5765118329881301375L;

    //region Local File Names
    // TODO : Move these so they can be changed on demand
    private static final String DIR_NAME_FACTIONS = "factions"; // Faction Subdirectory
    private static final String GIVEN_NAME_MALE_FILE = "maleGivenNames.csv"; // Male Given Name Filename
    private static final String GIVEN_NAME_FEMALE_FILE = "femaleGivenNames.csv"; // Female Given Name Filename
    private static final String SURNAME_FILE = "surnames.csv"; // Surname List Filename
    private static final String HISTORICAL_ETHNICITY_FILE = "historicalEthnicity.csv"; // Historical Ethnicity Filename
    //endregion Local File Names

    private static RandomNameGenerator rng; // This is using a singleton, because only a single usage of this class is required

    //region Data Maps
    /**
     * femaleGivenNames, maleGivenNames, and surnames contain values in the following format:
     * Map<Integer Ethnic_Code, WeightedMap<String Name>>
     * The ethnic code is an Integer value that is used to determine the ethnicity of the name, while
     * the name is a String value. The name is stored in a WeightedMap for each ethnic code to ensure
     * that there is a range from common to rare names. This is determined based on the input weights
     */
    private static Map<Integer, WeightedMap<String>> femaleGivenNames;
    private static Map<Integer, WeightedMap<String>> maleGivenNames;
    private static Map<Integer, WeightedMap<String>> surnames;

    /**
     * factionGivenNames contains values in the following format:
     * Map<String Faction_Name, Map<Integer Surname_Ethnic_Code, WeightedMap<Integer Given_Name_Ethnic_Code>>>
     * The faction name is the key to determining which list of names should be used, with the default being "General"
     * The Surname Ethnic Code is the code that the surname will be generated from
     * The Given Name Ethnic Code is the code to generate the given name from, from the femaleGivenNames or maleGivenNames
     * maps, and this is weighted to ensure that more common pairings are more common
     */
    private static Map<String, Map<Integer, WeightedMap<Integer>>> factionGivenNames;

    /**
     * factionEthnicCodes contains values in the following format:
     * Map<String Faction_Name, WeightedMap<Integer Surname_Ethnic_Code>>
     * The faction name is the key to determining which list of names should be used, with the default being "General"
     * The Surname Ethnic Code is the code that the surname will be generated from, and
     * this is weighted to ensure that more common pairings for the faction are more common
     */
    private static Map<String, WeightedMap<Integer>> factionEthnicCodes;
    //endregion Data Maps

    //region Faction Keys
    private static final String KEY_DEFAULT_FACTION = "General";
    private static final String KEY_DEFAULT_CLAN = "Clan";
    //endregion Faction Keys

    //region Default Names
    public static final String UNNAMED = "Unnamed";
    public static final String UNNAMED_SURNAME = "Person";
    public static final String UNNAMED_FULL_NAME = "Unnamed Person";
    //endregion Default Names

    @Deprecated // April 23rd, 2020 as part of adding a RandomGenderGenerator to MegaMek
    private int percentFemale;
    private String chosenFaction;

    private static final MMLogger logger = DefaultMmLogger.getInstance();
    private static volatile boolean initialized = false; // volatile to ensure readers get the current version
    //endregion Variable Declarations

    public RandomNameGenerator() {
        percentFemale = 50;
        chosenFaction = KEY_DEFAULT_FACTION;
    }

    //region Name Generators
    /**
     * Generate a single random name
     *
     * @return - a string giving the name
     */
    @Deprecated //17-Feb-2020 as part of the addition of gender tracking to MegaMek
    public String generate() {
        return generate(isFemale());
    }

    public String generate(boolean isFemale) {
        // this is a total hack, but for now lets assume that
        // if the chosenFaction name contains the word "clan"
        // we should only spit out first names
        return generate(isFemale, chosenFaction.toLowerCase().contains("clan"));
    }

    /**
     * Generate a single random name for MegaMek only
     *
     * @param isFemale true if the name should be female, otherwise false
     * @param isClan true if the name should be for a clanner, otherwise false
     * @return - a string containing the randomly generated name
     */
    public String generate(boolean isFemale, boolean isClan) {
        return generate(isFemale, isClan, chosenFaction);
    }

    /**
     * Generate a single random name
     *
     * @param isFemale true if the name should be female, otherwise false
     * @param isClan true if the name should be for a clanner, otherwise false
     * @param faction a string containing the faction key with which to generate the name from.
     *                If the faction is not a key for the <code>factionSurnames</code> Map,
     *                it will instead generate based on the General list
     * @return - a string containing the randomly generated name
     */
    public String generate(boolean isFemale, boolean isClan, String faction) {
        String name = UNNAMED_FULL_NAME;
        if (initialized) {
            // This checks to see if we've got a name map for the faction. If we do not, then we
            // go to check if the person is a clanner. If they are, then they default to the default
            // clan key provided that exists.
            // If the key isn't set by either case above, then the name is generated based on the
            // default faction key
            faction = factionEthnicCodes.containsKey(faction) ? faction
                    : ((isClan && (factionEthnicCodes.containsKey(KEY_DEFAULT_CLAN)))
                        ? KEY_DEFAULT_CLAN : KEY_DEFAULT_FACTION);
            final int ethnicCode = factionEthnicCodes.get(faction).randomItem();
            final int givenNameEthnicCode = factionGivenNames.get(faction).get(ethnicCode).randomItem();

            name = isFemale
                    ? femaleGivenNames.get(givenNameEthnicCode).randomItem()
                    : maleGivenNames.get(givenNameEthnicCode).randomItem();

            if (!isClan) {
                name += " " + surnames.get(ethnicCode).randomItem();
            }
        }
        return name;
    }

    /**
     * Generate a single random name split between a given name and surname
     *
     * @param isFemale true if the name should be female, otherwise false
     * @param isClan true if the name should be for a clanner, otherwise false
     * @param faction a string containing the faction key with which to generate the name from.
     *                If the faction is not a key for the <code>factionSurnames</code> Map,
     *                it will instead generate based on the General list
     * @return - a String[] containing the name,
     *              with the given name at String[0]
     *              and the surname at String[1]
     */
    public String[] generateGivenNameSurnameSplit(boolean isFemale, boolean isClan, String faction) {
        String[] name = { UNNAMED, UNNAMED_SURNAME };
        if (initialized) {
            // This checks to see if we've got a name map for the faction. If we do not, then we
            // go to check if the person is a clanner. If they are, then they default to the default
            // clan key provided that exists.
            // If the key isn't set by either case above, then the name is generated based on the
            // default faction key
            faction = factionEthnicCodes.containsKey(faction) ? faction
                    : ((isClan && (factionEthnicCodes.containsKey(KEY_DEFAULT_CLAN)))
                        ? KEY_DEFAULT_CLAN : KEY_DEFAULT_FACTION);
            final int ethnicCode = factionEthnicCodes.get(faction).randomItem();
            final int givenNameEthnicCode = factionGivenNames.get(faction).get(ethnicCode).randomItem();

            name[0] = isFemale
                    ? femaleGivenNames.get(givenNameEthnicCode).randomItem()
                    : maleGivenNames.get(givenNameEthnicCode).randomItem();

            name[1] = isClan ? "" : surnames.get(ethnicCode).randomItem();
        }
        return name;
    }
    //endregion Name Generators

    //region Getters and Setters
    public Iterator<String> getFactions() {
        return (factionEthnicCodes == null) ? null : factionEthnicCodes.keySet().iterator();
    }

    public String getChosenFaction() {
        return chosenFaction;
    }

    public void setChosenFaction(String chosenFaction) {
        this.chosenFaction = chosenFaction;
    }

    @Deprecated // April 23rd, 2020 as part of adding a RandomGenderGenerator to MegaMek
    public int getPercentFemale() {
        return percentFemale;
    }

    @Deprecated // April 23rd, 2020 as part of adding a RandomGenderGenerator to MegaMek
    public void setPercentFemale(int i) {
        percentFemale = i;
    }

    /**
     * randomly select gender
     *
     * @return true if female
     */
    @Deprecated // March 7th, 2020 by the addition of gender tracking to MegaMek
    public boolean isFemale() {
        return Compute.randomInt(100) < percentFemale;
    }

    /**
     * @return the Crew.G_* type containing the randomly generated gender
     */
    @Deprecated // April 23rd, 2020 as part of adding a RandomGenderGenerator to MegaMek
    public int generateGender() {
        if (Compute.randomInt(100) < percentFemale) {
            return Crew.G_FEMALE;
        } else {
            return Crew.G_MALE;
        }
    }

    /**
     * @return the instance of the RandomNameGenerator to use
     */
    public static synchronized RandomNameGenerator getInstance() {
        // only this code reads and writes `rng`
        if (rng == null) {
            // synchronized ensures this will only be entered exactly once
            rng = new RandomNameGenerator();
            rng.runThreadLoader();
        }
        // when getInstance returns, rng will always be non-null
        return rng;
    }
    //endregion Getters and Setters

    //region Initialization
    private void runThreadLoader() {
        Thread loader = new Thread(() -> rng.populateNames(), "Random Name Generator name initializer");
        loader.setPriority(Thread.NORM_PRIORITY - 1);
        loader.start();
    }

    private void populateNames() {
        //region Variable Instantiation
        int numEthnicCodes = 0;
        //endregion Variable Instantiation

        //region Map Instantiation
        maleGivenNames = new HashMap<>();
        femaleGivenNames = new HashMap<>();
        surnames = new HashMap<>();
        factionGivenNames = new HashMap<>();
        factionEthnicCodes = new HashMap<>();

        // Determine the number of ethnic codes
        File masterAncestryFile = new MegaMekFile(Configuration.namesDir(), HISTORICAL_ETHNICITY_FILE).getFile();
        try (InputStream is = new FileInputStream(masterAncestryFile);
             Scanner input = new Scanner(is, StandardCharsets.UTF_8.name())) {

            while (input.hasNextLine()) {
                input.nextLine();
                numEthnicCodes++;
            }
        } catch (IOException e) {
            logger.error(RandomNameGenerator.class, "populateNames",
                    "Could not find " + masterAncestryFile + "!");
        }

        // Then immediately instantiate the number of weighted maps needed for Given Names and Surnames
        for (int i = 1; i <= numEthnicCodes; i++) {
            maleGivenNames.put(i, new WeightedMap<>());
            femaleGivenNames.put(i, new WeightedMap<>());
            surnames.put(i, new WeightedMap<>());
        }
        //endregion Map Instantiation

        //region Read Names
        readNamesFileToMap(maleGivenNames, GIVEN_NAME_MALE_FILE);
        readNamesFileToMap(femaleGivenNames, GIVEN_NAME_FEMALE_FILE);
        readNamesFileToMap(surnames, SURNAME_FILE);
        //endregion Read Names

        //region Faction Files
        // all faction files should be in the faction directory
        File factionsDir = new MegaMekFile(Configuration.namesDir(), DIR_NAME_FACTIONS).getFile();
        String[] fileNames = factionsDir.list();

        if ((fileNames == null) || (fileNames.length == 0)) {
            //region No Factions Specified
            logger.error(RandomNameGenerator.class, "populateNames",
                    "No faction files found!");

            // We will create a general list where everything is weighted at one to allow players to
            // continue to play with named characters, indexing it at 1
            // Initialize Maps
            factionGivenNames.put(KEY_DEFAULT_FACTION, new HashMap<>());
            factionEthnicCodes.put(KEY_DEFAULT_FACTION, new WeightedMap<>());

            // Add information to maps
            for (int i = 0; i <= numEthnicCodes; i++) {
                factionGivenNames.get(KEY_DEFAULT_FACTION).put(i, new WeightedMap<>());
                factionGivenNames.get(KEY_DEFAULT_FACTION).get(i).add(1, i);
                factionEthnicCodes.get(KEY_DEFAULT_FACTION).add(1, i);
            }
            //endregion No Factions Specified
        } else {
            for (String filename : fileNames) {
                // Determine the key based on the file name
                String key = filename.split("\\.csv")[0];

                // Just check with the ethnic codes, as if it has the key then the two names
                // maps do
                if ((key.length() < 1) || factionEthnicCodes.containsKey(key)) {
                    continue;
                }

                // Initialize Maps
                factionGivenNames.put(key, new HashMap<>());
                factionEthnicCodes.put(key, new WeightedMap<>());

                File factionFile = new MegaMekFile(factionsDir, filename).getFile();
                try (InputStream is = new FileInputStream(factionFile);
                     Scanner input = new Scanner(is, StandardCharsets.UTF_8.name())) {

                    while (input.hasNextLine()) {
                        String[] values = input.nextLine().split(",");
                        int ethnicCode = Integer.parseInt(values[0]);

                        // Add information to maps
                        // The weights for ethnic given names for each surname ethnicity will be
                        // stored in the file at i + 2, so that is where we will parse them from
                        for (int i = 0; i <= numEthnicCodes; i++) {
                            factionGivenNames.get(key).put(ethnicCode, new WeightedMap<>());
                            factionGivenNames.get(key).get(ethnicCode).add(
                                    Integer.parseInt(values[i + 2]), i);
                        }

                        factionEthnicCodes.get(key).add(Integer.parseInt(values[2]), ethnicCode);
                    }
                } catch (IOException fne) {
                    logger.error(RandomNameGenerator.class, "populateNames",
                            "Could not find " + factionFile + "!");
                }
            }
        }
        initialized = true;
        //endregion Faction Files
    }

    private void readNamesFileToMap(Map<Integer, WeightedMap<String>> map, String fileName) {
        int lineNumber = 0;
        File file = new MegaMekFile(Configuration.namesDir(), fileName).getFile();

        try (InputStream is = new FileInputStream(file);
             Scanner input = new Scanner(is, StandardCharsets.UTF_8.name())) {

            input.nextLine(); // this is used to skip over the header line

            while (input.hasNextLine()) {
                lineNumber++;
                String[] values = input.nextLine().split(",");
                if (values.length < 3) {
                    logger.error(RandomNameGenerator.class, "readNamesFileToMap",
                            "Not enough fields in " + file.toString() + " on " + lineNumber);
                    continue;
                }

                map.get(Integer.parseInt(values[0])).add(Integer.parseInt(values[2]), values[1]);
            }
        } catch (IOException e) {
            logger.error(RandomNameGenerator.class, "populateNames",
                    "Could not find " + file + "!");
        }
    }
    //endregion Initialization
}
