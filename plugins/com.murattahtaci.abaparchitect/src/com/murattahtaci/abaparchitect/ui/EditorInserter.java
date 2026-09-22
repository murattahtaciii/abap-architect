package com.murattahtaci.abaparchitect.ui;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public final class EditorInserter {

    public static final class Result {

        public final boolean success;
        public final String message;

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private EditorInserter() {
    }

    public static Result insertAtCursor(String text) {
        if (text == null || text.isEmpty()) {
            return new Result(false, "Eklenecek kod yok.");
        }
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null || window.getActivePage() == null) {
            return new Result(false, "Aktif Eclipse penceresi bulunamadı.");
        }
        IWorkbenchPage page = window.getActivePage();
        IEditorPart editor = page.getActiveEditor();
        if (editor == null) {
            return new Result(false, "Aktif editör bulunamadı. Bir ABAP kaynağı açın.");
        }

        ITextEditor textEditor = findTextEditor(editor);
        IDocument document = null;
        if (textEditor != null && textEditor.getDocumentProvider() != null) {
            document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
        }
        if (document == null) {
            document = editor.getAdapter(IDocument.class);
        }
        if (document == null && textEditor != null) {
            document = adapterDocument(textEditor);
        }
        if (document == null) {
            return new Result(false, "Aktif editör metin düzenlemeyi desteklemiyor.");
        }

        int offset = document.getLength();
        int length = 0;
        if (textEditor != null) {
            ISelectionProvider provider = textEditor.getSelectionProvider();
            if (provider != null) {
                ISelection selection = provider.getSelection();
                if (selection instanceof ITextSelection) {
                    offset = ((ITextSelection) selection).getOffset();
                    length = ((ITextSelection) selection).getLength();
                }
            }
        }

        String prefix = offset > 0 && !text.startsWith("\n") ? "\n" : "";
        try {
            document.replace(offset, length, prefix + text);
        } catch (BadLocationException e) {
            return new Result(false, "Kod eklenemedi: " + e.getMessage());
        }
        int insertedLength = prefix.length() + text.length();
        if (textEditor != null) {
            textEditor.selectAndReveal(offset, insertedLength);
        }
        return new Result(true, "Kod aktif editöre eklendi.");
    }

    /**
     * Aktif editörden metin editörünü bulur. ADT ABAP editörü bir MultiPageEditorPart
     * olduğu için ITextEditor'a doğrudan uymaz; ADT'nin IAbapSourcePage adaptörü
     * (ITextEditor'dan türer) üzerinden erişilir. SAP sınıflarına derleme bağımlılığı
     * olmaması için adaptör yansıma ile çağrılır.
     */
    private static ITextEditor findTextEditor(IEditorPart editor) {
        if (editor instanceof ITextEditor) {
            return (ITextEditor) editor;
        }
        ITextEditor adapted = editor.getAdapter(ITextEditor.class);
        if (adapted != null) {
            return adapted;
        }
        try {
            Class<?> adtSourcePage = Class.forName("com.sap.adt.tools.abapsource.ui.sources.editors.IAbapSourcePage");
            Object page = editor.getAdapter(adtSourcePage);
            if (page instanceof ITextEditor) {
                waitForAdtSource(page);
                return (ITextEditor) page;
            }
        } catch (ClassNotFoundException e) {
            // ADT kurulu değil
        }
        return null;
    }

    private static void waitForAdtSource(Object page) {
        try {
            page.getClass().getMethod("waitUntilSourceCodeIsAvailable").invoke(page);
        } catch (Exception ignored) {
            // kaynak henüz hazır değilse ekleme sırasında hata döner
        }
    }

    private static IDocument adapterDocument(ITextEditor textEditor) {
        try {
            Object document = textEditor.getClass().getMethod("getDocument").invoke(textEditor);
            if (document instanceof IDocument) {
                return (IDocument) document;
            }
        } catch (Exception ignored) {
            // standart document provider yolu kullanılır
        }
        return null;
    }
}
