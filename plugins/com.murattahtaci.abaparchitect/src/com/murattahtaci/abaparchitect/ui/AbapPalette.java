package com.murattahtaci.abaparchitect.ui;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class AbapPalette {

    public final Color background;
    public final Color foreground;
    public final Color comment;
    public final Color keyword;
    public final Color typeName;
    public final Color stringLiteral;
    public final Color number;
    public final Color lineNumber;
    public final Font monoFont;
    public final boolean dark;

    public AbapPalette(Display display) {
        background = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        foreground = display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        dark = luminance(background.getRGB()) < 128;
        if (dark) {
            comment = new Color(display, new RGB(0x7a, 0x8f, 0xa8));
            keyword = new Color(display, new RGB(0xc0, 0x84, 0xfc));
            typeName = new Color(display, new RGB(0x7d, 0xd3, 0xfc));
            stringLiteral = new Color(display, new RGB(0x4a, 0xde, 0x80));
            number = new Color(display, new RGB(0xfb, 0x92, 0x3c));
            lineNumber = new Color(display, new RGB(0x5a, 0x7a, 0x9f));
        } else {
            comment = new Color(display, new RGB(0x6b, 0x72, 0x80));
            keyword = new Color(display, new RGB(0x5b, 0x21, 0xb6));
            typeName = new Color(display, new RGB(0x1e, 0x40, 0xaf));
            stringLiteral = new Color(display, new RGB(0x16, 0x65, 0x34));
            number = new Color(display, new RGB(0x9a, 0x34, 0x12));
            lineNumber = new Color(display, new RGB(0x94, 0xa3, 0xb8));
        }
        monoFont = JFaceResources.getFont(JFaceResources.TEXT_FONT);
    }

    public void dispose() {
        comment.dispose();
        keyword.dispose();
        typeName.dispose();
        stringLiteral.dispose();
        number.dispose();
        lineNumber.dispose();
    }

    private static int luminance(RGB rgb) {
        return (rgb.red * 299 + rgb.green * 587 + rgb.blue * 114) / 1000;
    }
}
