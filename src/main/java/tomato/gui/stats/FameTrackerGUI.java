package tomato.gui.stats;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import tomato.gui.stats.data.MapFameData;
import tomato.gui.stats.session.FameSession;
import tomato.gui.stats.session.FameSessionManager;
import tomato.gui.stats.utils.BaseStatsPanel;
import tomato.gui.stats.utils.UIComponentUtils;

public class FameTrackerGUI extends BaseStatsPanel {

    private static FameTrackerGUI INSTANCE;

    private final ArrayList<Fame> scores;
    private final HashMap<Integer, ArrayList<Fame>> fameList;
    private final GraphPanel graphPanel;

    // Session management

    private FameSession currentLiveSession;

    private boolean fameGainedSinceLastSave = false;

    public FameTrackerGUI() {
        INSTANCE = this;
        initializePanel();
    }

    @Override
    protected void initializePanel() {
        setLayout(new BorderLayout());

        scores = new ArrayList<>();
        fameList = new HashMap<>();

        graphPanel = new GraphPanel(scores);

        // Create a new live session
        currentLiveSession = new FameSession(
            "Live_" + System.currentTimeMillis()
        );

        // Create navigation panel (similar to DPS GUI)
        JPanel navigationPanel = createNavigationPanel();

        // Create container panel with navigation above graph
        JPanel containerPanel = UIComponentUtils.createMainPanel();
        containerPanel.add(navigationPanel, BorderLayout.NORTH);
        containerPanel.add(graphPanel, BorderLayout.CENTER);

        add(containerPanel);
    }

    @Override
    protected void updateGUI() {
        refreshPanel();
    }

    private JPanel createNavigationPanel() {
        JPanel navPanel = UIComponentUtils.createNavigationPanel();

        // Add time range buttons from graph panel
        navPanel.add(graphPanel.createTimeRangeButtons());

        return navPanel;
    }

    public static void updateFame(int charId, long fame, long time) {
        INSTANCE.update(charId, fame, time);
    }

    private void update(int charId, long fame, long time) {
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
            if (!fameList.isEmpty()) {
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
        autoSaveLiveSession();
    }

    /**
     * Manually save the current live session with a custom name
     */
    public void saveCurrentSession(String sessionName) {
        if (sessionName != null && !sessionName.trim().isEmpty()) {
            currentLiveSession.setSessionName(sessionName.trim());
            if (FameSessionManager.saveSession(currentLiveSession)) {
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
