package com.murattahtaci.abaparchitect.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class AbapCodeViewer {

    private static final String KEYWORDS = "(?i)\\b(TYPES|BEGIN\\s+OF|END\\s+OF|DATA|TYPE|STANDARD\\s+TABLE\\s+OF"
            + "|WITH\\s+EMPTY\\s+KEY|VALUE|EXPORTING|IMPORTING|CHANGING|REF\\s+TO|LENGTH|DECIMALS"
            + "|CLEAR|APPEND|WHILE|ENDWHILE|IF|ENDIF|IS\\s+NOT\\s+INITIAL|abap_bool)\\b";

    private static final Pattern COMMENT_LINE = Pattern.compile("(?m)^\\*.*$");
    private static final Pattern COMMENT_INLINE = Pattern.compile("(?m)\".*$");
    private static final Pattern STRING = Pattern.compile("'[^']*'");
    private static final Pattern NUMBER = Pattern.compile("(?<![A-Za-z0-9_])\\d+(?:\\.\\d+)?(?![A-Za-z0-9_])");
    private static final Pattern TYPE_NAME = Pattern.compile("\\b[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+\\b");
    private static final Pattern KEYWORD = Pattern.compile(KEYWORDS);

    private final AbapPalette palette;
    private final SourceViewer viewer;
    private final Document document = new Document();

    public AbapCodeViewer(Composite parent, AbapPalette palette) {
        this.palette = palette;
        CompositeRuler ruler = new CompositeRuler();
        LineNumberRulerColumn lineNumbers = new LineNumberRulerColumn();
        lineNumbers.setForeground(palette.lineNumber);
        lineNumbers.setBackground(palette.background);
        ruler.addDecorator(0, lineNumbers);
        viewer = new SourceViewer(parent, ruler, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        viewer.setDocument(document);
        viewer.setEditable(false);
        StyledText text = viewer.getTextWidget();
        text.setFont(palette.monoFont);
        text.setBackground(palette.background);
        text.setForeground(palette.foreground);
        text.setTabs(4);
    }

    public Control getControl() {
        return viewer.getControl();
    }

    public String getText() {
        return document.get();
    }

    public void setText(String content) {
        String value = content == null ? "" : content;
        document.set(value);
        applyHighlighting(value);
    }

    private void applyHighlighting(String content) {
        StyledText text = viewer.getTextWidget();
        if (content.isEmpty()) {
            text.setStyleRanges(new StyleRange[0]);
            return;
        }
        List<StyleRange> ranges = new ArrayList<>();
        List<int[]> claimedRanges = new ArrayList<>();

        addMatches(ranges, claimedRanges, content, COMMENT_LINE, palette.comment);
        addMatches(ranges, claimedRanges, content, COMMENT_INLINE, palette.comment);
        addMatches(ranges, claimedRanges, content, STRING, palette.stringLiteral);
        addMatches(ranges, claimedRanges, content, NUMBER, palette.number);
        addMatches(ranges, claimedRanges, content, TYPE_NAME, palette.typeName);
        addMatches(ranges, claimedRanges, content, KEYWORD, palette.keyword);

        ranges.sort(java.util.Comparator.comparingInt(range -> range.start));
        text.setStyleRanges(ranges.toArray(new StyleRange[0]));
    }

    private static void addMatches(List<StyleRange> ranges, List<int[]> claimedRanges, String content,
            Pattern pattern, org.eclipse.swt.graphics.Color color) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end || overlaps(claimedRanges, start, end)) {
                continue;
            }
            ranges.add(new StyleRange(start, end - start, color, null));
            claimedRanges.add(new int[] { start, end });
        }
    }

    private static boolean overlaps(List<int[]> ranges, int start, int end) {
        for (int[] range : ranges) {
            if (start < range[1] && end > range[0]) {
                return true;
            }
        }
        return false;
    }
}
