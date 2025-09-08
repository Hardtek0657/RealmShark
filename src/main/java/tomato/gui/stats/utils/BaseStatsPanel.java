package tomato.gui.stats.utils;

import javax.swing.*;
import java.awt.*;

/**
 * Abstract base class for stats GUI panels to reduce redundancy.
 * Provides common functionality for singleton pattern, font management, and update patterns.
 */
public abstract class BaseStatsPanel extends JPanel {
    
    protected static Font mainFont;
    protected boolean updateInProgress = false;
    
    public BaseStatsPanel() {
        initializePanel();
    }
    
    /**
     * Initialize the panel layout and components.
     * Subclasses should override this method to set up their specific UI.
     */
    protected abstract void initializePanel();
    
    /**
     * Update the GUI data and refresh the display.
     * Subclasses should override this method to handle their specific update logic.
     */
    protected abstract void updateGUI();
    
    /**
     * Sets the main font for this panel and all its components.
     */
    public static void setMainFont(Font font) {
        mainFont = font;
    }
    
    /**
     * Gets the current main font.
     */
    public static Font getMainFont() {
        return mainFont;
    }
    
    /**
     * Applies the main font to this panel and all its components recursively.
     */
    protected void applyMainFont() {
        if (mainFont != null) {
            UIComponentUtils.setFontRecursively(this, mainFont);
        }
    }
    
    /**
     * Safely updates the GUI on the Event Dispatch Thread.
     */
    protected void safeUpdateGUI() {
        if (SwingUtilities.isEventDispatchThread()) {
            updateGUI();
        } else {
            SwingUtilities.invokeLater(this::updateGUI);
        }
    }
    
    /**
     * Safely updates the GUI with a custom update action.
     */
    protected void safeUpdateGUI(Runnable updateAction) {
        if (SwingUtilities.isEventDispatchThread()) {
            updateAction.run();
        } else {
            SwingUtilities.invokeLater(updateAction);
        }
    }
    
    /**
     * Refreshes the panel (revalidate and repaint).
     */
    protected void refreshPanel() {
        revalidate();
        repaint();
    }
    
    /**
     * Safely refreshes the panel on the Event Dispatch Thread.
     */
    protected void safeRefreshPanel() {
        safeUpdateGUI(this::refreshPanel);
    }
    
    /**
     * Template method for handling font updates.
     * Applies font and triggers GUI update.
     */
    public void handleFontUpdate(Font font) {
        setMainFont(font);
        applyMainFont();
        safeUpdateGUI();
    }
    
    /**
     * Prevents multiple concurrent updates.
     */
    protected boolean tryStartUpdate() {
        if (updateInProgress) {
            return false;
        }
        updateInProgress = true;
        return true;
    }
    
    /**
     * Marks update as complete.
     */
    protected void finishUpdate() {
        updateInProgress = false;
    }
    
    /**
     * Template method for safe updates with concurrency protection.
     */
    protected void safeUpdate(Runnable updateAction) {
        safeUpdateGUI(() -> {
            if (tryStartUpdate()) {
                try {
                    updateAction.run();
                } finally {
                    finishUpdate();
                }
            }
        });
    }
}