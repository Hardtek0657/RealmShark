package tomato.gui.stats;

import java.util.ArrayList;
import java.util.HashMap;

public class FameTableBridge {

    private static FameTableBridge INSTANCE;
    private FameTablePanel fameTablePanel;
    private final HashMap<Integer, ArrayList<Fame>> fameList;

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

    public static FameTableBridge getInstance() {
        return INSTANCE;
    }
}
