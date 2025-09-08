package tomato.gui.stats.utils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Utility class for creating common UI components used across stats GUI panels.
 * Consolidates panel creation, button styling, and layout patterns.
 */
public class UIComponentUtils {
    
    // Standard dimensions and styling
    private static final Dimension SMALL_BUTTON_SIZE = new Dimension(70, 20);
    private static final Font BUTTON_FONT = new Font("Arial", Font.PLAIN, 10);
    private static final Font DEFAULT_LABEL_FONT = new Font("Arial", Font.PLAIN, 12);
    
    /**
     * Creates a standard scroll pane for vertical scrolling.
     * Note: SmartScroller should be added by the caller as it requires specific imports.
     */
    public static JScrollPane createStandardScrollPane(JComponent component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.getVerticalScrollBar().setUnitIncrement(40);
        return scrollPane;
    }
    
    /**
     * Creates a panel with vertical BoxLayout.
     */
    public static JPanel createVerticalPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
    
    /**
     * Creates a panel with horizontal FlowLayout.
     */
    public static JPanel createHorizontalFlowPanel() {
        return createHorizontalFlowPanel(FlowLayout.CENTER, 5, 5);
    }
    
    /**
     * Creates a panel with horizontal FlowLayout with custom alignment and gaps.
     */
    public static JPanel createHorizontalFlowPanel(int alignment, int hgap, int vgap) {
        return new JPanel(new FlowLayout(alignment, hgap, vgap));
    }
    
    /**
     * Creates a standard button with consistent styling.
     */
    public static JButton createStandardButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setPreferredSize(SMALL_BUTTON_SIZE);
        return button;
    }
    
    /**
     * Creates a button with custom size.
     */
    public static JButton createButton(String text, Dimension size) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        if (size != null) {
            button.setPreferredSize(size);
        }
        return button;
    }
    
    /**
     * Creates a button panel with consistent padding and styling.
     */
    public static JPanel createButtonPanel() {
        JPanel panel = createHorizontalFlowPanel(FlowLayout.LEFT, 5, 5);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return panel;
    }
    
    /**
     * Creates a label with standard font.
     */
    public static JLabel createStandardLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(DEFAULT_LABEL_FONT);
        return label;
    }
    
    /**
     * Creates a label with custom font.
     */
    public static JLabel createLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        if (font != null) {
            label.setFont(font);
        } else {
            label.setFont(DEFAULT_LABEL_FONT);
        }
        return label;
    }
    
    /**
     * Creates a titled border with standard font.
     */
    public static TitledBorder createTitledBorder(String title) {
        return createTitledBorder(title, null);
    }
    
    /**
     * Creates a titled border with custom font.
     */
    public static TitledBorder createTitledBorder(String title, Font font) {
        TitledBorder border = BorderFactory.createTitledBorder(
            null, title, TitledBorder.CENTER, TitledBorder.CENTER, font
        );
        return border;
    }
    
    /**
     * Creates a main container panel with BorderLayout.
     */
    public static JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        return panel;
    }
    
    /**
     * Creates a panel with padding border.
     */
    public static JPanel createPaddedPanel(int padding) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return panel;
    }
    
    /**
     * Creates a panel with compound border (line + empty border for padding).
     */
    public static JPanel createBorderedPanel(Color borderColor, int padding) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor),
            BorderFactory.createEmptyBorder(padding, padding, padding, padding)
        ));
        return panel;
    }
    
    /**
     * Sets font for all components in a container recursively.
     */
    public static void setFontRecursively(Container container, Font font) {
        if (font == null) return;
        
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                setFontRecursively((Container) component, font);
            }
            if (component instanceof JComponent) {
                component.setFont(font);
            }
        }
    }
    
    /**
     * Creates navigation panel similar to DPS GUI style.
     */
    public static JPanel createNavigationPanel() {
        return createHorizontalFlowPanel(FlowLayout.CENTER, 5, 5);
    }
    
    /**
     * Creates a time range button panel for graphs.
     */
    public static JPanel createTimeRangeButtonPanel() {
        JPanel panel = createHorizontalFlowPanel(FlowLayout.CENTER, 5, 5);
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        return panel;
    }
}