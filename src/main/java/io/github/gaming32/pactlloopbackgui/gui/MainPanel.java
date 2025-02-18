package io.github.gaming32.pactlloopbackgui.gui;

import io.github.gaming32.pactlloopbackgui.Main;
import io.github.gaming32.pactlloopbackgui.pactl.Pactl;
import io.github.gaming32.pactlloopbackgui.pactl.PactlArguments;
import io.github.gaming32.pactlloopbackgui.pactl.PactlModule;
import io.github.gaming32.pactlloopbackgui.pactl.PactlSourceOrSink;
import org.apache.commons.io.function.IOSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MainPanel extends JComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainPanel.class);

    private static final double SCALE = 2;
    private static final Stroke BASE_STROKE = new BasicStroke();
    private static final Stroke LINE_STROKE = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);

    private Map<Integer, PactlSourceOrSink> sources = loadSourcesOrSinks(Pactl::listSources);
    private Map<Integer, PactlSourceOrSink> sinks = loadSourcesOrSinks(Pactl::listSinks);
    private Map<Integer, Map<Integer, PactlModule>> links = loadLinks();

    private Map<Integer, Rectangle> sourcePoints = new HashMap<>();
    private Map<Integer, Rectangle> sinkPoints = new HashMap<>();

    private int start = -1;
    private Point mousePos = new Point();

    public MainPanel() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                start = findPoint(e, sourcePoints);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                final var end = findPoint(e, sinkPoints);
                if (end != -1) {
                    try {
                        Pactl.loadModule("module-loopback", Map.of(
                            "latency_msec", "20",
                            "source", Integer.toString(start),
                            "sink", Integer.toString(end)
                        ));
                        refreshLinks();
                    } catch (IOException ex) {
                        LOGGER.error("Failed to create loopback module", ex);
                        JOptionPane.showMessageDialog(
                            MainPanel.this,
                            "Failed to create loopback module",
                            Main.TITLE,
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
                start = -1;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                final var source = findPoint(e, sourcePoints);
                final var sourceLinks = links.remove(source);
                if (sourceLinks == null) return;

                var failed = new ArrayList<String>();
                for (final var link : sourceLinks.entrySet()) {
                    try {
                        Pactl.unloadModule(link.getValue().index());
                    } catch (IOException ex) {
                        LOGGER.error("Failed to unload module {}", link.getValue(), ex);
                        failed.add(sinks.get(link.getKey()).description());
                    }
                }
                if (!failed.isEmpty()) {
                    JOptionPane.showMessageDialog(
                        MainPanel.this,
                        "Failed to unlink the following sinks: " + String.join(", ", failed),
                        Main.TITLE,
                        JOptionPane.ERROR_MESSAGE
                    );
                }
                if (failed.size() != sourceLinks.size()) {
                    refreshLinks();
                }
            }

            private static int findPoint(MouseEvent event, Map<Integer, Rectangle> points) {
                for (final var point : points.entrySet()) {
                    if (point.getValue().contains(event.getX() / SCALE, event.getY() / SCALE)) {
                        return point.getKey();
                    }
                }
                return -1;
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePos = new Point((int) (e.getX() / SCALE), (int) (e.getY() / SCALE));
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
        });
    }

    public void refresh() {
        final var newSources = loadSourcesOrSinks(Pactl::listSources);
        final var newSinks = loadSourcesOrSinks(Pactl::listSinks);
        final var newLinks = loadLinks();

        if (!newSources.equals(sources) || !newSinks.equals(sinks) || !newLinks.equals(links)) {
            sources = newSources;
            sinks = newSinks;
            links = newLinks;
            repaint();
        }
    }

    private void refreshLinks() {
        final var newLinks = loadLinks();

        if (!newLinks.equals(links)) {
            links = newLinks;
            repaint();
        }
    }

    private static Map<Integer, PactlSourceOrSink> loadSourcesOrSinks(IOSupplier<List<PactlSourceOrSink>> provider) {
        try {
            return provider.get()
                .stream()
                .collect(Collectors.toMap(
                    PactlSourceOrSink::index,
                    Function.identity(),
                    (a, b) -> b,
                    LinkedHashMap::new
                ));
        } catch (IOException e) {
            LOGGER.error("Failed to load sources or sinks", e);
            return new LinkedHashMap<>();
        }
    }

    private static Map<Integer, Map<Integer, PactlModule>> loadLinks() {
        try {
            final var result = Pactl.listModules()
                .stream()
                .filter(module -> module.name().equals("module-loopback"))
                .collect(Collectors.groupingBy(
                    m -> PactlArguments.getIntOrDefault(m.arguments(), "source", -1),
                    Collectors.collectingAndThen(
                        Collectors.toMap(
                            m -> PactlArguments.getIntOrDefault(m.arguments(), "sink", -1),
                            Function.identity(),
                            (a, b) -> b
                        ),
                        m -> {
                            m.remove(-1);
                            return m;
                        }
                    )
                ));
            result.remove(-1);
            return result;
        } catch (IOException e) {
            LOGGER.error("Failed to load links", e);
            return new HashMap<>();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(854, 480);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final var g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.scale(SCALE, SCALE);

        final var boxHeight = g2d.getFontMetrics().getHeight() + 12;
        sourcePoints = drawSources(sources.values(), g2d, 10, false);
        sinkPoints = drawSources(sinks.values(), g2d, g2d.getClipBounds().width - 22, true);

        g2d.setStroke(LINE_STROKE);
        for (final var linkSources : links.entrySet()) {
            final var sourceRect = sourcePoints.get(linkSources.getKey());
            if (sourceRect == null) continue;
            for (final var linkSink : linkSources.getValue().entrySet()) {
                final var sinkRect = sinkPoints.get(linkSink.getKey());
                if (sinkRect == null) continue;
                g2d.setColor(Color.ORANGE);
                g2d.drawLine(
                    (int) (sourceRect.getCenterX() + boxHeight / 8.0),
                    (int) sourceRect.getCenterY(),
                    (int) (sinkRect.getCenterX() - boxHeight / 8.0),
                    (int) sinkRect.getCenterY()
                );
            }
        }

        g2d.setStroke(BASE_STROKE);
        for (final var source : sourcePoints.values()) {
            g2d.setColor(source.contains(mousePos) ? Color.GREEN : Color.BLACK);
            g2d.fillArc(source.x, source.y, source.width, source.height, -90, 180);
        }
        for (final var sink : sinkPoints.values()) {
            g2d.setColor(sink.contains(mousePos) ? Color.GREEN : Color.BLACK);
            g2d.fillArc(sink.x, sink.y, sink.width, sink.height, 90, 180);
        }

        final var sourceRect = sourcePoints.get(start);
        if (sourceRect != null) {
            g2d.setStroke(LINE_STROKE);
            g2d.setColor(Color.ORANGE);
            g2d.drawLine(
                (int) (sourceRect.getCenterX() + boxHeight / 8.0),
                (int) sourceRect.getCenterY(),
                mousePos.x, mousePos.y
            );
        }
    }

    private static Map<Integer, Rectangle> drawSources(Collection<PactlSourceOrSink> sources, Graphics2D g2d, int x, boolean rightAlign) {
        final var points = new HashMap<Integer, Rectangle>();

        final var metrics = g2d.getFontMetrics();
        final var boxHeight = metrics.getHeight() + 12;
        final var centeredY = boxHeight / 2 + (metrics.getAscent() - metrics.getDescent() - metrics.getLeading()) / 2;

        final var widestSource = longestName(sources, g2d.getFontMetrics());
        if (rightAlign) {
            x -= widestSource;
        }
        var y = 10;
        for (final var source : sources) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(x, y, widestSource + 12, boxHeight);

            g2d.setColor(Color.WHITE);
            g2d.fillRect(x + 3, y + 3, widestSource + 6, boxHeight - 6);

            g2d.setColor(Color.BLACK);
            g2d.drawString(source.description(), x + 6, y + centeredY);

            points.put(source.index(), new Rectangle(
                x - boxHeight / 4 + (rightAlign ? 0 : widestSource + 12),
                y + boxHeight / 4,
                boxHeight / 2, boxHeight / 2
            ));

            y += boxHeight + 10;
        }

        return points;
    }

    private static int longestName(Collection<PactlSourceOrSink> sources, FontMetrics font) {
        return sources.stream()
            .mapToInt(s -> font.stringWidth(s.description()))
            .max()
            .orElse(0);
    }
}
