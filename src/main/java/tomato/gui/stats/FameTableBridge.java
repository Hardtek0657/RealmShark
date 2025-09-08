package tomato.gui.stats;

import java.util.ArrayList;
import java.util.HashMap;
import tomato.gui.stats.session.FameSession;
import tomato.gui.stats.session.FameSessionManager;

public class FameTableBridge {

    private static FameTableBridge INSTANCE;
    private FameTablePanel fameTablePanel;
    private FameTrackerGUI fameTrackerGUI;
    private final HashMap<Integer, ArrayList<Fame>> fameList;

    // connects both fame views
    public FameTableBridge() {
        INSTANCE = this;
        fameList = new HashMap<>();
    }

    public static void initialize() {
        if (INSTANCE == null) {
            INSTANCE = new FameTableBridge();
        }
    }

    public static void updateFame(
        int charId,
        long fame,
        long time,
        String className
    ) {
        if (INSTANCE != null && INSTANCE.fameTablePanel != null) {
            INSTANCE.fameList
                .computeIfAbsent(charId, e -> new ArrayList<>())
                .add(new Fame(fame, time));

            INSTANCE.fameTablePanel.updateFame(charId, fame, time, className);
        }
    }

    public void setFameTablePanel(FameTablePanel panel) {
        this.fameTablePanel = panel;
    }

    public void setFameTrackerGUI(FameTrackerGUI gui) {
        this.fameTrackerGUI = gui;
    }

    public static FameTableBridge getInstance() {
        return INSTANCE;
    }

    /**
     * Trigger auto-save of the current live session
     */
    public void triggerAutoSave() {
        if (fameTrackerGUI != null) {
            fameTrackerGUI.triggerAutoSave();
        }
    }

    /**
     * Clear the current session file being written
     */
    public void clearCurrentSessionFile() {
        if (fameTrackerGUI != null) {
            fameTrackerGUI.clearCurrentSessionFile();
        }
    }
}
