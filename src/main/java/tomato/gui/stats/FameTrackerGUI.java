package tomato.gui.stats;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import tomato.gui.stats.data.MapFameData;
import tomato.gui.stats.session.FameSession;
import tomato.gui.stats.session.FameSessionManager;

public class FameTrackerGUI extends JPanel {

    private static FameTrackerGUI INSTANCE;

    private final ArrayList<Fame> scores;
    private final HashMap<Integer, ArrayList<Fame>> fameList;
    private final GraphPanel graphPanel;

    // Navigation components
    private JButton prevButton, liveButton, nextButton;
    private JLabel sessionLabel;

    // Session management
    private ArrayList<FameSession> savedSessions;
    private int currentSessionIndex = -1; // -1 = Live mode
    private FameSession currentLiveSession;
    private boolean isLiveMode = true;
    private boolean fameGainedSinceLastSave = false;

    public FameTrackerGUI() {
        INSTANCE = this;
        setLayout(new BorderLayout());

        scores = new ArrayList<>();
        fameList = new HashMap<>();
        savedSessions = new ArrayList<>();

        graphPanel = new GraphPanel(scores);

        // Create a new live session
        currentLiveSession = new FameSession(
            "Live_" + System.currentTimeMillis()
        );

        // Create navigation panel (similar to DPS GUI)
        JPanel navigationPanel = createNavigationPanel();

        // Create container panel with navigation above graph
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(navigationPanel, BorderLayout.NORTH);
        containerPanel.add(graphPanel, BorderLayout.CENTER);

        add(containerPanel);

        // Load existing sessions
        loadSavedSessions();
        updateNavigation();
    }

    private JPanel createNavigationPanel() {
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

        // Add time range buttons from graph panel
        navPanel.add(graphPanel.createTimeRangeButtons());

        // Add navigation buttons (similar to DPS GUI)
        prevButton = new JButton("<");
        liveButton = new JButton("Live");
        nextButton = new JButton(">");
        sessionLabel = new JLabel("Live");

        // Style buttons
        Font buttonFont = new Font("Arial", Font.PLAIN, 10);
        Dimension buttonSize = new Dimension(30, 20);
        prevButton.setFont(buttonFont);
        prevButton.setPreferredSize(buttonSize);
        liveButton.setFont(buttonFont);
        liveButton.setPreferredSize(new Dimension(40, 20));
        nextButton.setFont(buttonFont);
        nextButton.setPreferredSize(buttonSize);
        sessionLabel.setFont(new Font("Arial", Font.PLAIN, 10));

        // Add action listeners
        prevButton.addActionListener(e -> previousSession());
        liveButton.addActionListener(e -> setLiveMode());
        nextButton.addActionListener(e -> nextSession());

        navPanel.add(prevButton);
        navPanel.add(liveButton);
        navPanel.add(sessionLabel);
        navPanel.add(nextButton);

        return navPanel;
    }

    private void loadSavedSessions() {
        // Load all saved sessions from disk
        java.util.List<java.io.File> sessionFiles =
            FameSessionManager.getSavedSessions();
        for (java.io.File file : sessionFiles) {
            FameSession session = FameSessionManager.loadSession(file);
            if (session != null) {
                savedSessions.add(session);
            }
        }
    }

    public void previousSession() {
        if (savedSessions.isEmpty()) return;

        if (isLiveMode) {
            // Switch from live to last session
            currentSessionIndex = savedSessions.size() - 1;
            isLiveMode = false;
            loadSession(savedSessions.get(currentSessionIndex));
        } else if (currentSessionIndex > 0) {
            currentSessionIndex--;
            loadSession(savedSessions.get(currentSessionIndex));
        }
        updateNavigation();
    }

    public void nextSession() {
        if (savedSessions.isEmpty()) return;

        if (isLiveMode) {
            // Switch from live to first session
            currentSessionIndex = 0;
            isLiveMode = false;
            loadSession(savedSessions.get(currentSessionIndex));
        } else if (currentSessionIndex < savedSessions.size() - 1) {
            currentSessionIndex++;
            loadSession(savedSessions.get(currentSessionIndex));
        }
        updateNavigation();
    }

    public void setLiveMode() {
        if (!isLiveMode) {
            isLiveMode = true;
            currentSessionIndex = -1;
            clearData();
            updateNavigation();
            fameGainedSinceLastSave = false; // Reset flag when switching to live mode
        }
    }

    private void loadSession(FameSession session) {
        if (session == null) return;

        // Clear current data
        clearData();

        // Load session data
        HashMap<Integer, ArrayList<Fame>> loadedData =
            FameSessionManager.extractFameData(session);
        fameList.putAll(loadedData);

        // Load map fame data into FameTablePanel if available
        try {
            FameTablePanel fameTablePanel = FameTablePanel.getInstance();
            if (fameTablePanel != null) {
                HashMap<Integer, ArrayList<MapFameData>> loadedMapData =
                    FameSessionManager.extractMapFameData(session);
                fameTablePanel.setMapFameData(loadedMapData);
            }
        } catch (Exception e) {
            System.err.println(
                "Error loading map fame data: " + e.getMessage()
            );
        }

        // Update graph with first character's data (or empty if no data)
        if (!fameList.isEmpty()) {
            Integer firstCharId = fameList.keySet().iterator().next();
            graphPanel.setScores(fameList.get(firstCharId));
        } else {
            graphPanel.setScores(new ArrayList<>());
        }

        graphPanel.repaint();
    }

    private void clearData() {
        fameList.clear();
        scores.clear();
        graphPanel.setScores(new ArrayList<>());
        graphPanel.repaint();
    }

    private void updateNavigation() {
        if (isLiveMode) {
            sessionLabel.setText("Live");
            liveButton.setText("Live");
        } else {
            sessionLabel.setText(
                (currentSessionIndex + 1) + "/" + savedSessions.size()
            );
            liveButton.setText(">>>");
        }

        // Update button states
        prevButton.setEnabled(
            !savedSessions.isEmpty() && (!isLiveMode || currentSessionIndex > 0)
        );
        nextButton.setEnabled(
            !savedSessions.isEmpty() &&
            (!isLiveMode || currentSessionIndex < savedSessions.size() - 1)
        );
    }

    public static void updateFame(int charId, long fame, long time) {
        INSTANCE.update(charId, fame, time);
    }

    private void update(int charId, long fame, long time) {
        if (!isLiveMode) {
            // Don't update data when viewing old sessions
            return;
        }

        // Track that fame has been gained since last save
        fameGainedSinceLastSave = true;

        fameList
            .computeIfAbsent(charId, e -> new ArrayList<>())
            .add(new Fame(fame, time));
        graphPanel.setScores(fameList.get(charId));
        graphPanel.repaint();

        // Update live session data
        currentLiveSession.setCharacterFameData(
            FameSessionManager.convertToSessionFormat(fameList)
        );

        // Store class names for all characters in the session
        HashMap<Integer, String> classNames = new HashMap<>();
        FameTablePanel fameTablePanel = FameTablePanel.getInstance();
        if (fameTablePanel != null) {
            for (Integer characterId : fameList.keySet()) {
                String className = fameTablePanel.getClassNameForCharacterId(
                    characterId
                );
                classNames.put(characterId, className);
            }
            currentLiveSession.setCharacterClassNames(classNames);
        }

        // Include map fame data from FameTablePanel if available
        try {
            if (fameTablePanel != null) {
                HashMap<Integer, ArrayList<MapFameData>> mapFameData =
                    fameTablePanel.getMapFameData();
                currentLiveSession.setCharacterMapFameData(
                    FameSessionManager.convertMapDataToSessionFormat(
                        mapFameData
                    )
                );
            }
        } catch (Exception e) {
            System.err.println(
                "Error updating live session map data: " + e.getMessage()
            );
        }
    }

    /**
     * Auto-saves the live session to prevent data loss
     */
    private void autoSaveLiveSession() {
        try {
            // Only auto-save if we have meaningful data AND fame has been gained
            if (!fameList.isEmpty() && fameGainedSinceLastSave) {
                FameSessionManager.saveSession(currentLiveSession);
                fameGainedSinceLastSave = false; // Reset flag after successful save
            }
        } catch (Exception e) {
            // Silent fail for auto-save - don't interrupt user experience
            System.err.println("Auto-save failed: " + e.getMessage());
        }
    }

    /**
     * Public method to trigger auto-save from external components
     */
    public void triggerAutoSave() {
        if (isLiveMode) {
            autoSaveLiveSession();
        }
    }

    /**
     * Manually save the current live session with a custom name
     */
    public void saveCurrentSession(String sessionName) {
        if (!isLiveMode) {
            JOptionPane.showMessageDialog(
                this,
                "Cannot save while viewing historical sessions. Switch to Live mode first.",
                "Save Error",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (sessionName != null && !sessionName.trim().isEmpty()) {
            currentLiveSession.setSessionName(sessionName.trim());
            if (FameSessionManager.saveSession(currentLiveSession)) {
                // Add to saved sessions list and update navigation
                savedSessions.add(currentLiveSession);
                updateNavigation();

                JOptionPane.showMessageDialog(
                    this,
                    "Session saved successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );

                // Create new live session for continuing tracking
                currentLiveSession = new FameSession(
                    "Live_" + System.currentTimeMillis()
                );
                fameGainedSinceLastSave = false; // Reset flag for new session
            }
        }
    }

    /**
     * Gets the current fame data for external access
     */
    public HashMap<Integer, ArrayList<Fame>> getFameData() {
        return fameList;
    }

    /**
     * Check if we're in live mode
     */
    public boolean isLiveMode() {
        return isLiveMode;
    }

    /**
     * Get the current session index (-1 for live mode)
     */
    public int getCurrentSessionIndex() {
        return currentSessionIndex;
    }

    /**
     * Get the number of saved sessions
     */
    public int getSavedSessionCount() {
        return savedSessions.size();
    }

    /**
     * Clear the current session file being written and start fresh
     */
    public void clearCurrentSessionFile() {
        // Delete the current session file if it exists
        try {
            String currentFilename =
                currentLiveSession
                    .getSessionName()
                    .replaceAll("[^a-zA-Z0-9_\\- ]", "_") +
                ".fame";
            File currentFile = new File("FameSessions", currentFilename);
            if (currentFile.exists()) {
                currentFile.delete();
            }
        } catch (Exception e) {
            System.err.println(
                "Error deleting session file: " + e.getMessage()
            );
        }

        // Create a completely new empty session
        currentLiveSession = new FameSession(
            "Live_" + System.currentTimeMillis()
        );

        // Clear the fame gained flag since we're starting fresh
        fameGainedSinceLastSave = false;

        // Optional: Show confirmation message
        JOptionPane.showMessageDialog(
            this,
            "Session data cleared. Starting fresh tracking.",
            "Session Reset",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
}
