package tomato.gui.stats;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import tomato.backend.data.TomatoData;
import tomato.realmshark.RealmCharacter;

public class FameTablePanel extends JPanel {

    private final JTable fameTable;
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

    public FameTablePanel(TomatoData tomatoData) {
        INSTANCE = this;
        this.tomatoData = tomatoData;
        setLayout(new BorderLayout());
        fameData = new HashMap<>();
        lastFameEntries = new HashMap<>();
        sessionStartFame = new HashMap<>();
        sessionStartTime = new HashMap<>();
        characterClassNames = new HashMap<>();

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
                                String charName =
                                    (String) tableModel.getValueAt(row, 0);
                                int charId = -1;
                                if (charName.startsWith("Char ")) {
                                    try {
                                        charId = Integer.parseInt(
                                            charName.substring(5)
                                        );
                                    } catch (NumberFormatException e) {
                                        // Ignore malformed IDs
                                    }
                                } else {
                                    // Look up charId by class name
                                    for (Integer id : characterClassNames.keySet()) {
                                        if (
                                            characterClassNames
                                                .get(id)
                                                .equals(charName)
                                        ) {
                                            charId = id;
                                            break;
                                        }
                                    }
                                }

                                String tooltipText;
                                if (charId != -1) {
                                    Long sessionStart = sessionStartTime.get(
                                        charId
                                    );
                                    if (sessionStart != null) {
                                        java.util.Date startDate =
                                            new java.util.Date(sessionStart);
                                        java.text.SimpleDateFormat sdf =
                                            new java.text.SimpleDateFormat(
                                                "yyyy-MM-dd HH:mm:ss"
                                            );
                                        tooltipText = String.format(
                                            "Fame per minute: %.2f | Session started: %s",
                                            famePerMinute,
                                            sdf.format(startDate)
                                        );
                                    } else {
                                        tooltipText = String.format(
                                            "Fame per minute: %.2f | Session start: N/A",
                                            famePerMinute
                                        );
                                    }
                                } else {
                                    tooltipText = String.format(
                                        "Fame per minute: %.2f | Session start: Unknown character",
                                        famePerMinute
                                    );
                                }

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

        // Add some instructions
        // Add reset button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton resetButton = new JButton("Reset Session");
        resetButton.addActionListener(e -> resetSelectedSession());
        buttonPanel.add(resetButton);

        infoLabel = new JLabel(
            "Enter Daily Quest Room to load char data | Fame tracking - updates automatically when fame changes"
        );
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.NORTH);
        southPanel.add(infoLabel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
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
     * Populates the fame table with character data when available.
     */
    private void populateFromCharacterData() {
        if (
            tomatoData == null ||
            tomatoData.chars == null ||
            tomatoData.chars.isEmpty()
        ) {
            // Character data not loaded yet - show informative message
            SwingUtilities.invokeLater(() -> {
                if (tableModel.getRowCount() == 0) {
                    infoLabel.setText(
                        "Enter Daily Quest Room to load character data | Fame tracking - updates automatically when fame changes"
                    );
                }
            });
            return;
        }

        SwingUtilities.invokeLater(() -> {
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
        SwingUtilities.invokeLater(() -> {
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
        // Display exact values for accuracy instead of truncated values
        if (number == (long) number) {
            // Integer value - display without decimals
            return String.format("%d", (long) number);
        } else {
            // Decimal value - display with full precision
            return String.valueOf(number);
        }
    }

    private String formatFamePerHour(double famePerHour) {
        // Format fame/hour values with 2 decimal places for readability
        return String.format("%.2f", famePerHour);
    }

    private void resetSelectedSession() {
        int selectedRow = fameTable.getSelectedRow();
        if (selectedRow >= 0) {
            String charName = (String) tableModel.getValueAt(selectedRow, 0);
            // Extract charId from the display name
            int charId = -1;
            if (charName.startsWith("Char ")) {
                try {
                    charId = Integer.parseInt(charName.substring(5));
                } catch (NumberFormatException ex) {
                    // Ignore malformed IDs
                }
            } else {
                // Look up charId by class name
                for (Integer id : characterClassNames.keySet()) {
                    if (characterClassNames.get(id).equals(charName)) {
                        charId = id;
                        break;
                    }
                }
            }

            if (charId != -1) {
                resetSession(charId);
            }
        }
    }

    private void resetSession(int charId) {
        Fame lastEntry = lastFameEntries.get(charId);
        if (lastEntry != null) {
            sessionStartFame.put(charId, lastEntry.fame);
            sessionStartTime.put(charId, lastEntry.time);
            updateTableForCharacter(charId);
        }
    }

    private void resetAllSessions() {
        for (Integer charId : lastFameEntries.keySet()) {
            Fame lastEntry = lastFameEntries.get(charId);
            if (lastEntry != null) {
                sessionStartFame.put(charId, lastEntry.fame);
                sessionStartTime.put(charId, lastEntry.time);
            }
        }
        updateAllTables();
    }

    private void updateTableForCharacter(int charId) {
        Fame lastEntry = lastFameEntries.get(charId);
        if (lastEntry != null) {
            String className = characterClassNames.getOrDefault(
                charId,
                "Char " + charId
            );
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String rowCharId = (String) tableModel.getValueAt(i, 0);
                if (rowCharId.equals(className)) {
                    double sessionGain =
                        lastEntry.fame - getSessionStartFame(charId);
                    tableModel.setValueAt(formatNumber(sessionGain), i, 4);
                    // Update initial fame column with session start fame
                    tableModel.setValueAt(
                        formatNumber(getSessionStartFame(charId)),
                        i,
                        1
                    );
                    // Reset fame/hour to 0 when session is reset
                    tableModel.setValueAt(formatFamePerHour(0), i, 3);
                    break;
                }
            }
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
        SwingUtilities.invokeLater(() -> {
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
}
