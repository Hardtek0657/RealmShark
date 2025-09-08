package tomato.gui.stats;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GraphPanel extends JPanel implements MouseMotionListener {

    private static final int POINT_SIZE = 4;
    private static final int NUMBER_Y_DIVISIONS = 10;

    private final int padding = 25;
    private final int labelPadding = 25;
    private final Color lineColor = new Color(44, 102, 230, 180);
    private final Color pointColor = new Color(100, 100, 100, 180);
    private final Color gridColor = new Color(200, 200, 200, 200);
    private static final Stroke GRAPH_STROKE = new BasicStroke(2f);
    private ArrayList<Fame> scores;
    private int screenX;
    private int screenXdragLeft;
    private int screenXdragRight;
    private Fame dragLeft;
    private Fame dragRight;
    private double hoverFame = -1;
    private boolean pressed;
    private int rightSelectionValue;
    private int leftSelectionValue;
    private ArrayList<Fame> originalScores;
    private JPanel buttonPanel;
    private JButton btn1Min;
    private JButton btn5Min;
    private JButton btn10Min;
    private JButton btn20Min;
    private JButton btn30Min;
    private JButton btnAll;

    // Add fields to track button-based selection
    private Fame buttonDragLeft = null;
    private Fame buttonDragRight = null;
    private boolean buttonSelectionActive = false;

    // Track current time filter to reapply when new data arrives
    private long currentTimeFilter = -1; // -1 means no filter (All view)

    public GraphPanel(ArrayList<Fame> scores) {
        addMouseMotionListener(this);
        this.scores = scores;
        this.originalScores = new ArrayList<>(scores);
        // Buttons will be added by parent container, not here
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        MinMax minMax = getMinMaxScore();

        double xScale =
            ((double) getWidth() - (2 * padding) - labelPadding) /
            (minMax.maxScoreX - minMax.minScoreX);
        double yScale =
            ((double) getHeight() - 2 * padding - labelPadding) /
            (minMax.maxScoreY - minMax.minScoreY);

        List<Point> graphPoints = new ArrayList<>();
        int size = scores.size();
        for (Fame score : scores) {
            double v = score.time - minMax.minScoreX;
            int x1 = (int) (v * xScale + padding + labelPadding);
            double v1 = minMax.maxScoreY - score.fame;
            int y1 = (int) (v1 * yScale + padding);
            Point e = new Point(x1, y1);
            graphPoints.add(e);
        }

        // draw white background
        //        g2.setColor(Color.WHITE);
        //        g2.fillRect(padding + labelPadding, padding, getWidth() - (2 * padding) - labelPadding, getHeight() - 2 * padding - labelPadding);
        g2.setColor(Color.GRAY);

        // draw selection box
        int xWidth = getWidth() - labelPadding;
        int x3 = padding * 2 + labelPadding - 12;
        int y3 = padding + labelPadding + 5;
        if (pressed && dragLeft != null && dragRight != null) {
            g2.setColor(new Color(160, 180, 240));
            double dfame = Math.abs(dragLeft.fame - dragRight.fame);
            long dtime = Math.abs(dragLeft.time - dragRight.time);
            double f = (dfame / (dtime / 60000f));
            String s1 = String.format("Selected fame: %.0f", dfame);
            String s2 = String.format(
                "Selected time: %.2f min ( %.1f h )",
                (dtime / 60000f),
                (dtime / 3600000f)
            );
            String s3 = String.format(
                "Fame / Min: %.3f ( %.1f f/h )",
                f,
                f * 60
            );
            int leftSelection = graphPoints.get(leftSelectionValue).x;
            int rightSelection = graphPoints.get(rightSelectionValue).x;
            int selectionWidth = leftSelection - rightSelection;
            g2.fillRect(
                leftSelection,
                padding,
                Math.abs(selectionWidth),
                getHeight() - 2 * padding - labelPadding
            );
            g2.setColor(Color.GRAY);

            g2.drawString(s1, x3, y3);
            g2.drawString(s2, x3, y3 + 14);
            g2.drawString(s3, x3, y3 + 28);
            g2.drawLine(
                screenX,
                padding,
                screenX,
                getHeight() - padding - labelPadding
            );
        } else if (hoverFame >= 0) {
            g2.drawLine(
                screenX,
                padding,
                screenX,
                getHeight() - padding - labelPadding
            );
            String s = String.format("Fame: %.0f", hoverFame);
            g2.drawString(s, x3, y3);

            // Always display overall fame/min
            double overallFpm = calculateOverallFamePerMin();
            String s3 = String.format(
                "Fame / Min: %.3f ( %.1f f/h )",
                overallFpm,
                overallFpm * 60
            );
            g2.drawString(s3, x3, y3 + 14);
        } else if (
            buttonSelectionActive &&
            buttonDragLeft != null &&
            buttonDragRight != null
        ) {
            g2.setColor(new Color(160, 180, 240));
            double dfame = Math.abs(buttonDragLeft.fame - buttonDragRight.fame);
            long dtime = Math.abs(buttonDragLeft.time - buttonDragRight.time);
            double f = (dfame / (dtime / 60000f));
            String s1 = String.format("Selected fame: %.0f", dfame);
            String s2 = String.format(
                "Selected time: %.2f min ( %.1f h )",
                (dtime / 60000f),
                (dtime / 3600000f)
            );
            String s3 = String.format(
                "Fame / Min: %.3f ( %.1f f/h )",
                f,
                f * 60
            );

            // Calculate selection rectangle based on time range
            // Reuse existing minMax and xScale variables from method scope

            int leftX = (int) ((buttonDragLeft.time - minMax.minScoreX) *
                    xScale +
                padding +
                labelPadding);
            int rightX = (int) ((buttonDragRight.time - minMax.minScoreX) *
                    xScale +
                padding +
                labelPadding);
            int selectionWidth = Math.abs(leftX - rightX);

            g2.fillRect(
                Math.min(leftX, rightX),
                padding,
                selectionWidth,
                getHeight() - 2 * padding - labelPadding
            );
            g2.setColor(Color.GRAY);

            g2.drawString(s1, x3, y3);
            g2.drawString(s2, x3, y3 + 14);
            g2.drawString(s3, x3, y3 + 28);
        } else {
            // Always display overall fame/min when no selection or hover
            double overallFpm = calculateOverallFamePerMin();
            String s3 = String.format(
                "Fame / Min: %.3f ( %.1f f/h )",
                overallFpm,
                overallFpm * 60
            );
            g2.drawString(s3, x3, y3 + 14);
        }

        // create hatch marks and grid lines for y axis.
        for (int i = 0; i < NUMBER_Y_DIVISIONS + 1; i++) {
            int x0 = padding + labelPadding;
            int x1 = POINT_SIZE + padding + labelPadding;
            int y0 =
                getHeight() -
                ((i * (getHeight() - padding * 2 - labelPadding)) /
                        NUMBER_Y_DIVISIONS +
                    padding +
                    labelPadding);
            if (size > 0) {
                g2.setColor(gridColor);
                g2.drawLine(
                    padding + labelPadding + 1 + POINT_SIZE,
                    y0,
                    getWidth() - padding,
                    y0
                );
                g2.setColor(Color.GRAY);
                String yLabel = String.valueOf(
                    ((int) ((minMax.minScoreY +
                                (minMax.maxScoreY - minMax.minScoreY) *
                                ((i * 1.0) / NUMBER_Y_DIVISIONS)) *
                            100)) /
                    100
                );
                FontMetrics metrics = g2.getFontMetrics();
                int labelWidth = metrics.stringWidth(yLabel);
                g2.drawString(
                    yLabel,
                    x0 - labelWidth - 5,
                    y0 + (metrics.getHeight() / 2) - 3
                );
            }
            g2.drawLine(x0, y0, x1, y0);
        }

        int seconds = (int) (minMax.maxScoreX - minMax.minScoreX) / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;

        int span;
        int display;
        String timeString;
        if (minutes < 10) {
            span = 60;
            display = 1;
            timeString = "(min)";
        } else if (minutes < 60) {
            span = 600;
            display = 10;
            timeString = "(min)";
        } else if (minutes < 120) {
            span = 1200;
            display = 30;
            timeString = "(min)";
        } else if (hours < 20) {
            span = 3600;
            display = 1;
            timeString = "(hour)";
        } else if (hours < 60) {
            display = 3;
            span = 3600 * display;
            timeString = "(hour)";
        } else {
            display = 9;
            span = 3600 * display;
            timeString = "(hour)";
        }
        g2.drawString(
            timeString,
            xWidth - padding - 9,
            getHeight() - padding - labelPadding - 5
        );

        float fraction = (float) span / seconds;

        // and for x axis
        for (int i = 0; i < 100; i++) {
            if (size > 1) {
                int x0 = (int) (((i *
                            (getWidth() - padding * 2 - labelPadding)) *
                        fraction) +
                    padding +
                    labelPadding);
                if (x0 >= xWidth) break;
                int y0 = getHeight() - padding - labelPadding;
                int y1 = y0 - POINT_SIZE;
                //                if ((i % ((int) ((size / 5.0)) + 1)) == 0) {
                if (scores.size() > 0) {
                    g2.setColor(gridColor);
                    g2.drawLine(
                        x0,
                        getHeight() - padding - labelPadding - 1 - POINT_SIZE,
                        x0,
                        padding
                    );
                    g2.setColor(Color.GRAY);
                    int sec = display * i;
                    FontMetrics metrics = g2.getFontMetrics();
                    String xLabel = Integer.toString(sec);
                    int labelWidth = metrics.stringWidth(xLabel);
                    g2.drawString(
                        xLabel,
                        x0 - labelWidth / 2,
                        y0 + metrics.getHeight() + 3
                    );
                }
                g2.drawLine(x0, y0, x0, y1);
            }
        }

        // create x and y axes
        g2.drawLine(
            padding + labelPadding,
            getHeight() - padding - labelPadding,
            padding + labelPadding,
            padding
        );
        g2.drawLine(
            padding + labelPadding,
            getHeight() - padding - labelPadding,
            getWidth() - padding,
            getHeight() - padding - labelPadding
        );

        Stroke oldStroke = g2.getStroke();
        g2.setColor(lineColor);
        g2.setStroke(GRAPH_STROKE);
        for (int i = 0; i < graphPoints.size() - 1; i++) {
            int x1 = graphPoints.get(i).x;
            int y1 = graphPoints.get(i).y;
            int x2 = graphPoints.get(i + 1).x;
            int y2 = graphPoints.get(i + 1).y;
            g2.drawLine(x1, y1, x2, y1);
            g2.drawLine(x2, y1, x2, y2);
        }

        g2.setStroke(oldStroke);
        g2.setColor(pointColor);
        for (Point graphPoint : graphPoints) {
            int x = graphPoint.x - POINT_SIZE / 2;
            int y = graphPoint.y - POINT_SIZE / 2;
            g2.fillOval(x, y, POINT_SIZE, POINT_SIZE);
        }
    }

    private MinMax getMinMaxScore() {
        MinMax minMax = new MinMax();
        for (Fame score : scores) {
            minMax.minScoreX = Math.min(minMax.minScoreX, score.time);
            minMax.minScoreY = Math.min(minMax.minScoreY, score.fame);
            minMax.maxScoreX = Math.max(minMax.maxScoreX, score.time);
            minMax.maxScoreY = Math.max(minMax.maxScoreY, score.fame);
        }
        return minMax;
    }

    private double calculateOverallFamePerMin() {
        if (scores == null || scores.size() < 2) {
            return 0.0;
        }

        double totalFame =
            scores.get(scores.size() - 1).fame - scores.get(0).fame;
        long totalTime =
            scores.get(scores.size() - 1).time - scores.get(0).time;

        if (totalTime <= 0) {
            return 0.0;
        }

        return totalFame / (totalTime / 60000.0);
    }

    public void setScores(ArrayList<Fame> scores) {
        this.scores = scores;
        this.originalScores = new ArrayList<>(scores);

        // Reapply current time filter if one is active
        if (currentTimeFilter > 0) {
            filterData(currentTimeFilter);
        } else {
            invalidate();
            this.repaint();
        }
    }

    private static void createAndShowGui() {
        ArrayList<Fame> scores = new ArrayList<>();
        Random random = new Random();
        int maxDataPoints = 10;
        int maxScore = 300000000;
        int sumy = 0;
        int sumx = 0;
        for (int i = 0; i < maxDataPoints; i++) {
            sumy += random.nextDouble() * maxScore;
            sumx += random.nextDouble() * maxScore * 3;
            scores.add(new Fame(sumx, sumy));
        }
        GraphPanel mainPanel = new GraphPanel(scores);
        mainPanel.setPreferredSize(new Dimension(800, 600));
        JFrame frame = new JFrame("DrawGraph");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GraphPanel::createAndShowGui);
    }

    public JPanel createTimeRangeButtons() {
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));

        btn1Min = new JButton("1 min");
        btn5Min = new JButton("5 min");
        btn10Min = new JButton("10 min");
        btn20Min = new JButton("20 min");
        btn30Min = new JButton("30 min");
        btnAll = new JButton("All");

        // Style buttons
        Font buttonFont = new Font("Arial", Font.PLAIN, 10);
        Dimension buttonSize = new Dimension(70, 20);

        btn1Min.setFont(buttonFont);
        btn1Min.setPreferredSize(buttonSize);
        btn5Min.setFont(buttonFont);
        btn5Min.setPreferredSize(buttonSize);
        btn10Min.setFont(buttonFont);
        btn10Min.setPreferredSize(buttonSize);
        btn20Min.setFont(buttonFont);
        btn20Min.setPreferredSize(buttonSize);
        btn30Min.setFont(buttonFont);
        btn30Min.setPreferredSize(buttonSize);
        btnAll.setFont(buttonFont);
        btnAll.setPreferredSize(buttonSize);

        // Add action listeners
        btn1Min.addActionListener(e -> filterData(1 * 60 * 1000L));
        btn5Min.addActionListener(e -> filterData(5 * 60 * 1000L));
        btn10Min.addActionListener(e -> filterData(10 * 60 * 1000L));
        btn20Min.addActionListener(e -> filterData(20 * 60 * 1000L));
        btn30Min.addActionListener(e -> filterData(30 * 60 * 1000L));
        btnAll.addActionListener(e -> showAllData());

        buttonPanel.add(btn1Min);
        buttonPanel.add(btn5Min);
        buttonPanel.add(btn10Min);
        buttonPanel.add(btn20Min);
        buttonPanel.add(btn30Min);
        buttonPanel.add(btnAll);

        return buttonPanel;
    }

    private void filterData(long timeRangeMs) {
        if (originalScores == null || originalScores.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - timeRangeMs;

        ArrayList<Fame> filteredScores = new ArrayList<>();
        for (Fame fame : originalScores) {
            if (fame.time >= cutoffTime) {
                filteredScores.add(fame);
            }
        }

        this.scores = filteredScores;
        this.currentTimeFilter = timeRangeMs; // Store the current filter

        // Set up selection info for the filtered time range
        if (!filteredScores.isEmpty()) {
            buttonDragLeft = filteredScores.get(0);
            buttonDragRight = filteredScores.get(filteredScores.size() - 1);
            buttonSelectionActive = true;
        } else {
            buttonSelectionActive = false;
        }

        repaint();
    }

    private void showAllData() {
        if (originalScores != null) {
            this.scores = new ArrayList<>(originalScores);
            this.currentTimeFilter = -1; // Reset filter to All view
            buttonSelectionActive = false; // Hide selection info when showing all data
            repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!pressed) {
            screenXdragLeft = e.getX();
        }
        if (dragLeft != null) pressed = true;
        screenXdragRight = e.getX();
        if (screenXdragLeft < screenXdragRight) {
            dragLeft = getRange(screenXdragLeft, false);
            dragRight = getRange(screenXdragRight, true);
        } else {
            dragLeft = getRange(screenXdragLeft, true);
            dragRight = getRange(screenXdragRight, false);
        }
        screenX = e.getX();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        pressed = false;
        screenX = e.getX();
        Fame f = getRange(screenX, false);
        hoverFame = -1;
        if (f != null) {
            hoverFame = f.fame;
        }
        repaint();
    }

    public Fame getRange(int x, boolean b) {
        if (scores.size() == 0) return null;
        float dx =
            (float) (x - padding - labelPadding) /
            (getWidth() - labelPadding * 3);
        if (dx < 0) {
            if (b) {
                rightSelectionValue = 0;
            }
            return scores.get(0);
        } else if (dx > 1) {
            if (b) {
                rightSelectionValue = scores.size() - 1;
            }
            return scores.get(scores.size() - 1);
        }
        MinMax m = getMinMaxScore();
        float ddx = (float) (m.maxScoreX - m.minScoreX) * dx;

        int f = 0;
        for (int i = 0; i < scores.size(); i++) {
            Fame fame = scores.get(i);
            if ((fame.time - m.minScoreX) > ddx) {
                break;
            }
            f = i;
        }

        if (b) {
            f++;
            rightSelectionValue = f;
        } else {
            leftSelectionValue = f;
        }

        if (f < scores.size()) {
            return scores.get(f);
        } else if (f == scores.size()) {
            rightSelectionValue--;
            return scores.get(scores.size() - 1);
        } else {
            return null;
        }
    }

    private static class MinMax {

        double minScoreX = Double.MAX_VALUE;
        double minScoreY = Double.MAX_VALUE;
        double maxScoreX = Double.MIN_VALUE;
        double maxScoreY = Double.MIN_VALUE;
    }
}
