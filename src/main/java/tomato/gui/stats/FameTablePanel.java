package tomato.gui.stats;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import tomato.backend.data.TomatoData;
import tomato.gui.stats.data.MapFameData;
import tomato.gui.stats.session.FameSessionManager;
import tomato.gui.stats.session.FameSessionViewer;
import tomato.gui.stats.utils.BaseStatsPanel;
import tomato.gui.stats.utils.FormatUtils;
import tomato.gui.stats.utils.UIComponentUtils;
import tomato.realmshark.RealmCharacter;

public class FameTablePanel extends BaseStatsPanel {

    private final JTable fameTable;
    private final HashMap<Integer, ArrayList<MapFameData>> mapFameData;
    private final HashMap<Integer, MapFameData> currentMapData;
    private String currentMapName = "";
    private final DefaultTableModel tableModel;
    private final HashMap<Integer, ArrayList<Fame>> fameData;
    private final HashMap<Integer, Fame> lastFameEntries;
    private final HashMap<Integer, Double> sessionStartFame;
    private final HashMap<Integer, Long> sessionStartTime;
    private final HashMap<Integer, String> characterClassNames;
    private static FameTablePanel INSTANCE;
    private final JLabel infoLabel;
    private final TomatoData tomatoData;
    private int currentCharacterId = -1;
    private int previousCharacterId = -1;
    private final HashMap<String, Boolean> dungeonFilterState;

    public FameTablePanel(TomatoData tomatoData) {
        INSTANCE = this;
        this.tomatoData = tomatoData;
        initializePanel();
    }

    @Override
    protected void initializePanel() {
        setLayout(new BorderLayout());
        fameData = new HashMap<>();
        lastFameEntries = new HashMap<>();
        sessionStartFame = new HashMap<>();
        sessionStartTime = new HashMap<>();
        characterClassNames = new HashMap<>();
        mapFameData = new HashMap<>();
        currentMapData = new HashMap<>();
        dungeonFilterState = new HashMap<>();

        // Create table with column headers
        String[] columnNames = {
            "Character",
            "Initial Fame",
            "Current Fame",
            "Fame/Hour",
            "Session Gain",
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };

        fameTable = new JTable(tableModel);
        fameTable.setAutoCreateRowSorter(true);
        fameTable.getTableHeader().setReorderingAllowed(false);

        // Set column widths
        fameTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        fameTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        fameTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        fameTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        fameTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(fameTable);
        add(scrollPane, BorderLayout.CENTER);

        setupFamePerHourRenderer();
        setupButtonPanel();
    }

    @Override
    protected void updateGUI() {
        refreshPanel();
    }

    private void setupFamePerHourRenderer() {
        // Set custom cell renderer for Fame/Hour column to show tooltip
        fameTable
            .getColumnModel()
            .getColumn(3)
            .setCellRenderer(
                new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                        JTable table,
                        Object value,
                        boolean isSelected,
                        boolean hasFocus,
                        int row,
                        int column
                    ) {
                        Component c = super.getTableCellRendererComponent(
                            table,
                            value,
                            isSelected,
                            hasFocus,
                            row,
                            column
                        );

                        if (value != null) {
                            try {
                                double famePerHour = Double.parseDouble(
                                    value.toString()
                                );
                                double famePerMinute = famePerHour / 60.0;

                                // Get character ID from row data and find session start time
                                int charId = getCharacterIdFromRow(row);

                                String tooltipText = createTooltipText(famePerMinute, charId);
                                setToolTipText(tooltipText);
                            } catch (NumberFormatException e) {
                                setToolTipText(
                                    "Fame per minute: 0.00 | Session start: N/A"
                                );
                            }
                        } else {
                            setToolTipText(
                                "Fame per minute: 0.00 | Session start: N/A"
                            );
                        }

                        return c;
                    }
                }
            );
    }

    private void setupButtonPanel() {
        // Add reset button panel
        JPanel buttonPanel = UIComponentUtils.createButtonPanel();

        JButton resetButton = new JButton("Reset Session");
        resetButton.addActionListener(e -> resetAllSessionsAndClearFile());
        buttonPanel.add(resetButton);

        JButton mapFameButton = new JButton("Show Map Fame");
        mapFameButton.addActionListener(e -> showMapFameTable());
        buttonPanel.add(mapFameButton);

        JButton viewSessionsButton = new JButton("View Saved Sessions");
        viewSessionsButton.addActionListener(e -> viewSavedSessions());
        buttonPanel.add(viewSessionsButton);

        infoLabel = UIComponentUtils.createStandardLabel(
            "Enter Daily Quest Room to load char data | Fame tracking - updates automatically when fame changes"
        );
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.NORTH);
        southPanel.add(infoLabel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
    }

    private int getCharacterIdFromRow(int row) {
        try {
            String charName = (String) tableModel.getValueAt(row, 0);
            if (charName != null) {
                if (charName.startsWith("Char ")) {
                    try {
                        return Integer.parseInt(charName.substring(5));
                    } catch (NumberFormatException e) {
                        // Ignore malformed IDs
                    }
                } else {
                    // Look up charId by class name
                    for (Integer id : characterClassNames.keySet()) {
                        if (characterClassNames.get(id).equals(charName)) {
                            return id;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Handle potential index out of bounds or other exceptions
        }
        return -1;
    }

    private String createTooltipText(double famePerMinute, int charId) {
        try {
            if (charId != -1) {
                Long sessionStart = sessionStartTime.get(charId);
                if (sessionStart != null) {
                    String formattedTime = FormatUtils.formatSessionTime(sessionStart);
                    return String.format(
                        "Fame per minute: %.2f | Session started: %s",
                        famePerMinute,
                        formattedTime
                    );
                } else {
                    return String.format(
                        "Fame per minute: %.2f | Session start: N/A",
                        famePerMinute
                    );
                }
            } else {
                return String.format(
                    "Fame per minute: %.2f | Session start: Unknown character",
                    famePerMinute
                );
            }
        } catch (Exception e) {
            return String.format(
                "Fame per minute: %.2f | Session start: Error",
                famePerMinute
            );
        }
    }
    }

    /**
     * Gets the singleton instance of FameTablePanel for session integration
     */
    public static FameTablePanel getInstance() {
        return INSTANCE;
    }

    /**
     * Method for receiving realm character list info to pre-populate the fame table.
     */
    public static void updateRealmChars() {
        if (INSTANCE != null) {
            INSTANCE.populateFromCharacterData();
        }
    }

    /**
     * Open the saved sessions viewer
     */
    private void viewSavedSessions() {
        FameSessionViewer.openSessionViewer();
    }

    /**
     * Populates the fame table with character data when available.
     */
    private void populateFromCharacterData() {
        if (
            tomatoData == null ||
            tomatoData.chars == null ||
            tomatoData.chars.isEmpty()
        ) {
            // Character data not loaded yet - show informative message
            safeUpdateGUI(() -> {
                if (tableModel.getRowCount() == 0) {
                    infoLabel.setText(
                        "Enter Daily Quest Room to load character data | Fame tracking - updates automatically when fame changes"
                    );
                }
            });
            return;
        }

        safeUpdateGUI(() -> {
            // Clear the initial message
            infoLabel.setText(
                "Fame tracking - updates automatically when fame changes"
            );

            // Pre-populate table with character data
            for (RealmCharacter character : tomatoData.chars) {
                // Skip characters with invalid data
                if (
                    character.charId == 0 ||
                    character.classString == null ||
                    character.classString.isEmpty()
                ) {
                    continue;
                }

                // Check if this character already exists in the table
                boolean characterExists = false;
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    String rowCharName = (String) tableModel.getValueAt(i, 0);
                    if (rowCharName.equals(character.classString)) {
                        characterExists = true;
                        break;
                    }
                }

                if (!characterExists) {
                    // Add character to table with initial data
                    Object[] rowData = {
                        character.classString,
                        formatNumber(character.fame),
                        formatNumber(character.fame),
                        "0.00",
                        "0.00",
                    };
                    tableModel.addRow(rowData);

                    // Store character class name for future reference
                    characterClassNames.put(
                        character.charId,
                        character.classString
                    );

                    // Initialize session data
                    sessionStartFame.put(
                        character.charId,
                        (double) character.fame
                    );
                    sessionStartTime.put(
                        character.charId,
                        System.currentTimeMillis()
                    );
                    lastFameEntries.put(
                        character.charId,
                        new Fame(character.fame, System.currentTimeMillis())
                    );
                }
            }

            // Set current character ID if available and check for character changes
            if (tomatoData.getCharId() != -1) {
                if (
                    tomatoData.getCharId() != currentCharacterId &&
                    currentCharacterId != -1
                ) {
                    // Character changed - reset fame/hour for previous character
                    resetFamePerHourForInactiveCharacters();
                    previousCharacterId = currentCharacterId;
                }
                currentCharacterId = tomatoData.getCharId();
            }

            // If no characters were added but data exists, show appropriate message
            if (tableModel.getRowCount() == 0 && !tomatoData.chars.isEmpty()) {
                infoLabel.setText(
                    "Character data loaded but no valid characters found | Enter Daily Quest Room to refresh"
                );
            }
        });
    }

    public void updateFame(int charId, long fame, long time, String className) {
        safeUpdateGUI(() -> {
            // Check if character has changed
            if (charId != currentCharacterId && currentCharacterId != -1) {
                // Character changed - reset fame/hour for previous character
                resetFamePerHourForInactiveCharacters();
                previousCharacterId = currentCharacterId;
                currentCharacterId = charId;
            } else if (currentCharacterId == -1) {
                // First character update
                currentCharacterId = charId;
            }

            // Store the class name for this character
            if (!className.isEmpty()) {
                characterClassNames.put(charId, className);
            }

            // Store the fame data
            fameData
                .computeIfAbsent(charId, id -> new ArrayList<>())
                .add(new Fame(fame, time));

            // Update or add row for this character
            Fame lastEntry = lastFameEntries.get(charId);
            if (lastEntry == null) {
                // New character
                addCharacterRow(charId, fame, time, className);
            } else {
                // Update existing character
                updateCharacterRow(charId, fame, time, lastEntry, className);
            }

            lastFameEntries.put(charId, new Fame(fame, time));
        });
    }

    private void addCharacterRow(
        int charId,
        long fame,
        long time,
        String className
    ) {
        String displayName = className.isEmpty() ? "Char " + charId : className;
        double initialFame = getSessionStartFame(charId);
        Object[] rowData = {
            displayName,
            formatNumber(initialFame),
            formatNumber(fame),
            "0.00",
            "0.00",
        };
        tableModel.addRow(rowData);

        // Set session start fame and time to current values
        sessionStartFame.put(charId, (double) fame);
        sessionStartTime.put(charId, time);
    }

    private void updateCharacterRow(
        int charId,
        long fame,
        long time,
        Fame lastEntry,
        String className
    ) {
        String displayName = className.isEmpty() ? "Char " + charId : className;
        // Find the row for this character
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String rowCharId = (String) tableModel.getValueAt(i, 0);
            if (rowCharId.equals(displayName)) {
                // Calculate fame gain and rate
                double fameGain = fame - lastEntry.fame;
                long timeDiff = time - lastEntry.time;
                double hoursDiff = timeDiff / 3600000.0; // ms to hours
                double famePerHour = hoursDiff > 0 ? fameGain / hoursDiff : 0;

                // Calculate session-based fame per hour
                Long sessionStart = sessionStartTime.get(charId);
                double sessionFamePerHour = 0;
                if (sessionStart != null && time > sessionStart) {
                    double sessionHoursDiff = (time - sessionStart) / 3600000.0;
                    double sessionFameGain = fame - getSessionStartFame(charId);
                    sessionFamePerHour = sessionHoursDiff > 0
                        ? sessionFameGain / sessionHoursDiff
                        : 0;
                }

                // Update the row - use session-based fame per hour for accuracy
                tableModel.setValueAt(formatNumber(fame), i, 2);
                tableModel.setValueAt(
                    formatFamePerHour(sessionFamePerHour),
                    i,
                    3
                );

                // Update session gain (total gain for this character)
                double sessionGain = fame - getSessionStartFame(charId);
                tableModel.setValueAt(formatNumber(sessionGain), i, 4);

                break;
            }
        }
    }

    private double getSessionStartFame(int charId) {
        return sessionStartFame.getOrDefault(charId, getInitialFame(charId));
    }

    private double getInitialFame(int charId) {
        ArrayList<Fame> entries = fameData.get(charId);
        return entries != null && !entries.isEmpty() ? entries.get(0).fame : 0;
    }

    private String formatNumber(double number) {
        return FormatUtils.formatNumber(number);
    }

    private String formatFamePerHour(double famePerHour) {
        return FormatUtils.formatFamePerHour(famePerHour);
    }

    private void resetAllSessions() {
        for (Integer charId : lastFameEntries.keySet()) {
            Fame lastEntry = lastFameEntries.get(charId);
            if (lastEntry != null) {
                sessionStartFame.put(charId, lastEntry.fame);
                sessionStartTime.put(charId, lastEntry.time);
            }
        }

        // Clear map fame data when resetting sessions
        mapFameData.clear();

        updateAllTables();
    }

    private void resetAllSessionsAndClearFile() {
        resetAllSessions();

        // Clear the current .fame file being written
        try {
            FameTableBridge bridge = FameTableBridge.getInstance();
            if (bridge != null) {
                bridge.clearCurrentSessionFile();
            }
        } catch (Exception e) {
            System.err.println(
                "Error clearing session file: " + e.getMessage()
            );
        }
    }

    private void updateAllTables() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String rowCharId = (String) tableModel.getValueAt(i, 0);
            if (rowCharId.startsWith("Char ")) {
                try {
                    int charId = Integer.parseInt(rowCharId.substring(5));
                    Fame lastEntry = lastFameEntries.get(charId);
                    if (lastEntry != null) {
                        double sessionGain =
                            lastEntry.fame - getSessionStartFame(charId);
                        tableModel.setValueAt(formatNumber(sessionGain), i, 4);
                        // Reset fame/hour to 0 when session is reset
                        tableModel.setValueAt(formatFamePerHour(0), i, 3);
                    }
                } catch (NumberFormatException e) {
                    // Ignore malformed character IDs
                }
            }
        }
    }

    private void resetFamePerHourForInactiveCharacters() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String rowCharName = (String) tableModel.getValueAt(i, 0);
            int rowCharId = getCharacterIdFromRowName(rowCharName);

            // Reset fame/hour to 0 for all inactive characters
            if (rowCharId != currentCharacterId) {
                tableModel.setValueAt(formatFamePerHour(0), i, 3);
            }
        }
    }

    // Method to detect character changes from TomatoData updates
    public void checkForCharacterChange() {
        safeUpdateGUI(() -> {
            if (tomatoData != null && tomatoData.getCharId() != -1) {
                if (
                    tomatoData.getCharId() != currentCharacterId &&
                    currentCharacterId != -1
                ) {
                    // Character changed - reset fame/hour for previous character
                    resetFamePerHourForInactiveCharacters();
                    previousCharacterId = currentCharacterId;
                    currentCharacterId = tomatoData.getCharId();
                } else if (currentCharacterId == -1) {
                    // First character detection
                    currentCharacterId = tomatoData.getCharId();
                }
            }
        });
    }

    private int getCharacterIdFromRowName(String rowCharName) {
        // Check if row name matches "Char X" format
        if (rowCharName.startsWith("Char ")) {
            try {
                return Integer.parseInt(rowCharName.substring(5));
            } catch (NumberFormatException e) {
                // Ignore malformed character IDs
                return -1;
            }
        } else {
            // Look up the character ID from the class names map
            for (Integer charId : characterClassNames.keySet()) {
                String className = characterClassNames.get(charId);
                if (className.equals(rowCharName)) {
                    return charId;
                }
            }
        }
        return -1; // Character ID not found
    }

    // Helper method to get all fame data for a character (useful for potential export)
    public ArrayList<Fame> getFameData(int charId) {
        return fameData.get(charId);
    }

    // Helper method to get current fame for a character
    public Double getCurrentFame(int charId) {
        Fame lastEntry = lastFameEntries.get(charId);
        return lastEntry != null ? lastEntry.fame : null;
    }

    /**
     * Updates map fame tracking when changing maps
     */
    private void updateMapFameTracking(
        int charId,
        String mapName,
        double currentFame,
        long currentTime
    ) {
        // Get or create map fame data list for this character
        ArrayList<MapFameData> charMapData = mapFameData.computeIfAbsent(
            charId,
            k -> new ArrayList<>()
        );

        // Check if we're already tracking a map for this character
        MapFameData currentData = currentMapData.get(charId);
        if (currentData != null) {
            // Update the end time and fame for the current map
            currentData.endTime = currentTime;
            currentData.endFame = currentFame;

            // Only add the completed map data to the list if fame was gained
            // This ensures we capture maps where bosses are killed instantly
            if (currentData.getFameGained() > 0) {
                charMapData.add(currentData);
            }
        }

        // Start tracking new map (but don't add to the list yet - wait for completion)
        if (mapName != null && !mapName.isEmpty()) {
            MapFameData newMapData = new MapFameData(
                mapName,
                currentTime,
                (double) currentFame
            );
            currentMapData.put(charId, newMapData);
        }
    }

    /**
     * Public method to handle map changes and update fame tracking
     */
    public void onMapChange(String newMapName) {
        if (newMapName != null && !newMapName.equals(currentMapName)) {
            // First, complete tracking for the current map
            String oldMapName = currentMapName;
            currentMapName = newMapName;

            // Update map fame tracking for all characters with current fame data
            // This will complete the current map and start tracking the new one
            for (Integer charId : lastFameEntries.keySet()) {
                Fame lastFame = lastFameEntries.get(charId);
                if (lastFame != null) {
                    updateMapFameTracking(
                        charId,
                        newMapName,
                        lastFame.fame,
                        System.currentTimeMillis()
                    );
                }
            }

            // Auto-save session when leaving a map to prevent data loss from crashes
            autoSaveSessionOnMapChange();
        }
    }

    /**
     * Static method to handle map changes from external classes
     */
    public static void handleMapChange(String newMapName) {
        if (INSTANCE != null) {
            INSTANCE.onMapChange(newMapName);
        }
    }

    /**
     * Auto-saves the current session when leaving a map to prevent data loss
     */
    private void autoSaveSessionOnMapChange() {
        try {
            // Get the FameTrackerGUI instance through the bridge
            FameTableBridge bridge = FameTableBridge.getInstance();
            if (bridge != null) {
                bridge.triggerAutoSave();
            }
        } catch (Exception e) {
            // Silent fail for auto-save - don't interrupt user experience
            System.err.println(
                "Map change auto-save failed: " + e.getMessage()
            );
        }
    }

    /**
     * Displays a table showing fame gained per map for the selected character
     */
    private void showMapFameTable() {
        // Check if we have map fame data for any characters
        if (mapFameData.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No map fame data available for any characters.",
                "Map Fame Data",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Get all characters with map fame data
        java.util.List<Integer> charactersWithMapData =
            new java.util.ArrayList<>();
        for (Integer charId : mapFameData.keySet()) {
            ArrayList<MapFameData> charMapData = mapFameData.get(charId);
            if (charMapData != null && !charMapData.isEmpty()) {
                charactersWithMapData.add(charId);
            }
        }

        if (charactersWithMapData.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No map fame data available for any characters.",
                "Map Fame Data",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Let user select which character to view
        String[] characterOptions = new String[charactersWithMapData.size()];
        for (int i = 0; i < charactersWithMapData.size(); i++) {
            int charId = charactersWithMapData.get(i);
            String className = getClassNameForCharacterId(charId);
            characterOptions[i] = className + " (ID: " + charId + ")";
        }

        String selectedCharacter = (String) JOptionPane.showInputDialog(
            this,
            "Select the character to display map fame information:",
            "Character Selection",
            JOptionPane.QUESTION_MESSAGE,
            null,
            characterOptions,
            characterOptions[0]
        );

        if (selectedCharacter == null) {
            return; // User cancelled
        }

        // Extract character ID from selection
        int selectedCharId = extractCharIdFromSelection(selectedCharacter);
        if (selectedCharId == -1) {
            return;
        }

        // Show map fame dialog for selected character
        ArrayList<MapFameData> mapData = mapFameData.get(selectedCharId);
        showMapFameDialog(selectedCharId, mapData);
    }

    /**
     * Shows dialog with map fame data for a specific character
     */
    private void showMapFameDialog(int charId, ArrayList<MapFameData> mapData) {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Map Fame Data - " + getClassNameForCharacterId(charId),
            true
        );
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        // Create main panel with table
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create table model
        String[] columnNames = {
            "Map Name",
            "Time Spent",
            "Fame Gained",
            "Fame/Minute",
        };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Populate table with data
        model.setRowCount(0);
        if (mapData != null) {
            for (MapFameData data : mapData) {
                double fameGained = data.getFameGained();
                long timeSpentMs = data.getTimeSpent();
                double minutesSpent = timeSpentMs / 60000.0;
                double famePerMinute = minutesSpent > 0
                    ? fameGained / minutesSpent
                    : 0;

                model.addRow(
                    new Object[] {
                        data.mapName,
                        data.getTimeSpentFormatted(),
                        String.format("%.1f", fameGained),
                        String.format("%.1f", famePerMinute),
                    }
                );
            }
        }

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(table);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        dialog.add(mainPanel, BorderLayout.CENTER);

        // Create button panel with filter and close buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Add dungeon filter button
        JButton filterButton = new JButton("Dungeon Filter");
        filterButton.addActionListener(e -> {
            showDungeonFilterDialog(dialog, model, mapData);
        });
        buttonPanel.add(filterButton);

        // Add close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Set size after adding all components
        dialog.setSize(800, 500);
        dialog.setVisible(true);
    }

    /**
     * Shows dialog for filtering dungeons in the map fame table
     */
    private void showDungeonFilterDialog(
        JDialog parentDialog,
        DefaultTableModel tableModel,
        ArrayList<MapFameData> originalData
    ) {
        JDialog filterDialog = new JDialog(
            parentDialog,
            "Dungeon Filter",
            true
        );
        filterDialog.setLayout(new BorderLayout());
        filterDialog.setSize(300, 400);
        filterDialog.setLocationRelativeTo(parentDialog);

        // Get unique dungeon names
        java.util.Set<String> dungeonNames = new java.util.HashSet<>();
        for (MapFameData data : originalData) {
            dungeonNames.add(data.mapName);
        }

        // Create checkboxes for each dungeon with persisted state
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
        java.util.Map<String, JCheckBox> checkBoxMap =
            new java.util.HashMap<>();

        for (String dungeonName : dungeonNames) {
            // Use stored filter state or default to true if not set
            boolean isSelected = dungeonFilterState.getOrDefault(
                dungeonName,
                true
            );
            JCheckBox checkBox = new JCheckBox(dungeonName, isSelected);
            checkBoxMap.put(dungeonName, checkBox);
            checkBoxPanel.add(checkBox);
            checkBoxPanel.add(Box.createVerticalStrut(2)); // Small 2px spacing
        }

        JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
        filterDialog.add(scrollPane, BorderLayout.CENTER);

        // Add apply and close buttons
        JPanel buttonPanel = new JPanel();
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            applyDungeonFilter(tableModel, originalData, checkBoxMap);
            // Save filter state for persistence
            for (String dungeonName : checkBoxMap.keySet()) {
                dungeonFilterState.put(
                    dungeonName,
                    checkBoxMap.get(dungeonName).isSelected()
                );
            }
            filterDialog.dispose();
        });
        buttonPanel.add(applyButton);

        JButton resetButton = new JButton("Reset Filters");
        resetButton.addActionListener(e -> {
            // Reset all checkboxes to selected
            for (JCheckBox checkBox : checkBoxMap.values()) {
                checkBox.setSelected(true);
            }
            // Clear filter state
            dungeonFilterState.clear();
        });
        buttonPanel.add(resetButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> filterDialog.dispose());
        buttonPanel.add(closeButton);

        filterDialog.add(buttonPanel, BorderLayout.SOUTH);
        filterDialog.setVisible(true);
    }

    /**
     * Applies the dungeon filter to the table model
     */
    private void applyDungeonFilter(
        DefaultTableModel tableModel,
        ArrayList<MapFameData> originalData,
        java.util.Map<String, JCheckBox> checkBoxMap
    ) {
        // Clear current table data
        tableModel.setRowCount(0);

        // Add filtered data
        for (MapFameData data : originalData) {
            JCheckBox checkBox = checkBoxMap.get(data.mapName);
            if (checkBox != null && checkBox.isSelected()) {
                double fameGained = data.getFameGained();
                long timeSpentMs = data.getTimeSpent();
                double minutesSpent = timeSpentMs / 60000.0;
                double famePerMinute = minutesSpent > 0
                    ? fameGained / minutesSpent
                    : 0;

                tableModel.addRow(
                    new Object[] {
                        data.mapName,
                        data.getTimeSpentFormatted(),
                        String.format("%.1f", fameGained),
                        String.format("%.1f", famePerMinute),
                    }
                );
            }
        }
    }

    /**
     * Resets all dungeon filters to their default state (all enabled).
     */
    public void resetDungeonFilters() {
        dungeonFilterState.clear();
    }

    /**
     * Gets the map fame data for session persistence
     */
    public HashMap<Integer, ArrayList<MapFameData>> getMapFameData() {
        return mapFameData;
    }

    /**
     * Sets the map fame data from a loaded session
     */
    public void setMapFameData(
        HashMap<Integer, ArrayList<MapFameData>> newMapFameData
    ) {
        mapFameData.clear();
        mapFameData.putAll(newMapFameData);
    }

    /**
     * Gets the current map data for session persistence
     */
    public HashMap<Integer, MapFameData> getCurrentMapData() {
        return currentMapData;
    }

    /**
     * Sets the current map data from a loaded session
     */
    public void setCurrentMapData(
        HashMap<Integer, MapFameData> newCurrentMapData
    ) {
        currentMapData.clear();
        currentMapData.putAll(newCurrentMapData);
    }

    /**
     * Get class name for a character ID
     */
    public String getClassNameForCharacterId(int charId) {
        return characterClassNames.getOrDefault(charId, "Char " + charId);
    }

    /**
     * Extract character ID from selection string (format: "ClassName (ID: 123)")
     */
    private int extractCharIdFromSelection(String selection) {
        try {
            int startIndex = selection.lastIndexOf("(ID: ") + 5;
            int endIndex = selection.lastIndexOf(")");
            if (startIndex > 0 && endIndex > startIndex) {
                String idStr = selection.substring(startIndex, endIndex).trim();
                return Integer.parseInt(idStr);
            }
        } catch (Exception e) {
            // Fallback: try to parse the entire string as integer
            try {
                return Integer.parseInt(selection.trim());
            } catch (NumberFormatException ex) {
                // If all fails, return -1
            }
        }
        return -1;
    }
}
