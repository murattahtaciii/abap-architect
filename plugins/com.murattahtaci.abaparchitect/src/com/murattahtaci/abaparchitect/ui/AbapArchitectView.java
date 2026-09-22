package com.murattahtaci.abaparchitect.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import com.murattahtaci.abaparchitect.core.Converter;
import com.murattahtaci.abaparchitect.core.Format;
import com.murattahtaci.abaparchitect.core.GenerateOptions;
import com.murattahtaci.abaparchitect.core.GenerateResult;
import com.murattahtaci.abaparchitect.core.JsonParseException;
import com.murattahtaci.abaparchitect.core.JsonParser;
import com.murattahtaci.abaparchitect.core.XmlSupport;
import com.murattahtaci.abaparchitect.ddic.DdicModel;
import com.murattahtaci.abaparchitect.ddic.DdicModelBuilder;
import com.murattahtaci.abaparchitect.ddic.DdicObjectPlan;
import com.murattahtaci.abaparchitect.ddic.DdicPlanOptions;
import com.murattahtaci.abaparchitect.ddic.DdicPlanner;
import com.murattahtaci.abaparchitect.ui.adt.AdtRestClient;

public class AbapArchitectView extends ViewPart {

    public static final String VIEW_ID = "com.murattahtaci.abaparchitect.view";

    private static final String[] TAB_KEYS = { "types", "mapping", "ixml", "st", "template", "ddic", "validation" };
    private static final String[] TAB_LABEL_KEYS = { "tabTypes", "tabMapping", "tabIxml", "tabSt", "tabTemplate", "tabDdic", "tabValidation" };

    private static final String SAMPLE_JSON = "{\n"
            + "  \"salesOrder\": {\n"
            + "    \"orderId\": \"4500001234\",\n"
            + "    \"orderDate\": \"2024-01-15\",\n"
            + "    \"customerNumber\": \"0000012345\",\n"
            + "    \"salesOrganization\": \"1000\",\n"
            + "    \"distributionChannel\": \"10\",\n"
            + "    \"isPriority\": true,\n"
            + "    \"totalAmount\": 14850.50,\n"
            + "    \"items\": [{\n"
            + "      \"itemNumber\": \"000010\",\n"
            + "      \"materialNumber\": \"MAT-001-A\",\n"
            + "      \"description\": \"HDPE Pipe 50mm\",\n"
            + "      \"quantity\": 150,\n"
            + "      \"unitOfMeasure\": \"EA\",\n"
            + "      \"netPrice\": 99.0,\n"
            + "      \"currency\": \"USD\"\n"
            + "    }],\n"
            + "    \"header\": {\n"
            + "      \"purchaseOrderNumber\": \"PO-2024-001\",\n"
            + "      \"requestedDeliveryDate\": \"2024-02-15\",\n"
            + "      \"shippingCondition\": \"01\",\n"
            + "      \"paymentTerms\": \"NT30\"\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String SAMPLE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<salesOrder>\n"
            + "  <orderId>4500001234</orderId>\n"
            + "  <orderDate>2024-01-15</orderDate>\n"
            + "  <customerNumber>0000012345</customerNumber>\n"
            + "  <salesOrganization>1000</salesOrganization>\n"
            + "  <distributionChannel>10</distributionChannel>\n"
            + "  <isPriority>true</isPriority>\n"
            + "  <totalAmount>14850.50</totalAmount>\n"
            + "  <items>\n"
            + "    <item>\n"
            + "      <itemNumber>000010</itemNumber>\n"
            + "      <materialNumber>MAT-001-A</materialNumber>\n"
            + "      <description>HDPE Pipe 50mm</description>\n"
            + "      <quantity>150</quantity>\n"
            + "      <unitOfMeasure>EA</unitOfMeasure>\n"
            + "      <netPrice>99.0</netPrice>\n"
            + "      <currency>USD</currency>\n"
            + "    </item>\n"
            + "    <item>\n"
            + "      <itemNumber>000020</itemNumber>\n"
            + "      <materialNumber>MAT-002-B</materialNumber>\n"
            + "      <description>PVC Fitting 25mm</description>\n"
            + "      <quantity>50</quantity>\n"
            + "      <unitOfMeasure>EA</unitOfMeasure>\n"
            + "      <netPrice>45.0</netPrice>\n"
            + "      <currency>USD</currency>\n"
            + "    </item>\n"
            + "  </items>\n"
            + "  <header purchaseOrderNumber=\"PO-2024-001\" shippingCondition=\"01\">\n"
            + "    <requestedDeliveryDate>2024-02-15</requestedDeliveryDate>\n"
            + "    <paymentTerms>NT30</paymentTerms>\n"
            + "  </header>\n"
            + "</salesOrder>";

    private AbapPalette palette;
    private StyledText input;
    private Combo formatCombo;
    private Text rootField;
    private Text structPrefixField;
    private Text tablePrefixField;
    private Text suffixField;
    private Combo suffixTargetCombo;
    private Button snakeCheck;
    private Button attrFlatCheck;
    private Combo languageCombo;
    private TabFolder outputFolder;
    private final Map<String, TabItem> tabItems = new LinkedHashMap<>();
    private final Map<String, AbapCodeViewer> viewers = new LinkedHashMap<>();
    private StyledText validationText;
    private Label statusLabel;
    private Label linesLabel;
    private Label generationLabel;
    private Runnable pendingConvert;
    private int lastTypeCount;
    private int lastMappingCount;

    private final Map<String, Control> translatable = new LinkedHashMap<>();
    private final Map<String, Button> tooltipButtons = new LinkedHashMap<>();

    @Override
    public void createPartControl(Composite parent) {
        palette = new AbapPalette(parent.getDisplay());
        Composite root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));

        createLinkRow(root);
        createSettingsRow(root);
        createActionRow(root);
        createWorkspace(root);
        createStatusRow(root);

        setPartName("ABAP Architect");
        applyLanguage();
        updateLinesInfo();
        showValidationMessage("", false);
    }

    @Override
    public void setFocus() {
        if (input != null && !input.isDisposed()) {
            input.setFocus();
        }
    }

    @Override
    public void dispose() {
        if (palette != null) {
            palette.dispose();
            palette = null;
        }
        super.dispose();
    }

    /** Sağ üste LinkedIn / GitHub bağlantıları. */
    private void createLinkRow(Composite parent) {
        Composite row = new Composite(parent, SWT.NONE);
        row.setLayout(new GridLayout(1, false));
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        org.eclipse.swt.widgets.Link links = new org.eclipse.swt.widgets.Link(row, SWT.RIGHT);
        links.setText("<a href=\"https://www.linkedin.com/in/murattahtacii/\">LinkedIn</a>"
                + "        <a href=\"https://github.com/murattahtaciii\">GitHub</a>");
        links.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
        links.addSelectionListener(onSelect(event -> openUrl(event.text)));
    }

    /** Bağlantıyı sistemin varsayılan tarayıcısında açar. */
    private static void openUrl(String url) {
        try {
            org.eclipse.ui.PlatformUI.getWorkbench().getBrowserSupport()
                    .getExternalBrowser().openURL(new java.net.URL(url));
        } catch (Exception e) {
            org.eclipse.swt.program.Program.launch(url);
        }
    }

    private void createSettingsRow(Composite parent) {
        Composite row = createRow(parent);

        addSettingLabel(row, "format");
        formatCombo = new Combo(row, SWT.READ_ONLY);
        formatCombo.setItems(new String[] { "JSON", "XML" });
        formatCombo.select(0);
        formatCombo.addSelectionListener(onSelect(event -> scheduleConvert()));

        addSettingLabel(row, "root");
        rootField = createText(row, "ROOT", 80, true);

        addSettingLabel(row, "str");
        structPrefixField = createText(row, "TS_", 56, true);

        addSettingLabel(row, "tab");
        tablePrefixField = createText(row, "TT_", 56, true);

        addSettingLabel(row, "suffix");
        suffixField = createText(row, "", 64, false);
        suffixField.addModifyListener(event -> {
            suffixTargetCombo.setEnabled(!suffixField.getText().trim().isEmpty());
            scheduleConvert();
        });
        suffixTargetCombo = new Combo(row, SWT.READ_ONLY);
        suffixTargetCombo.setItems(new String[] { "→ ABAP", "→ JSON" });
        suffixTargetCombo.select(0);
        suffixTargetCombo.setEnabled(false);
        suffixTargetCombo.addSelectionListener(onSelect(event -> scheduleConvert()));

        snakeCheck = new Button(row, SWT.CHECK);
        snakeCheck.setSelection(true);
        snakeCheck.addSelectionListener(onSelect(event -> scheduleConvert()));
        register("snake", snakeCheck);

        attrFlatCheck = new Button(row, SWT.CHECK);
        attrFlatCheck.setSelection(true);
        attrFlatCheck.addSelectionListener(onSelect(event -> scheduleConvert()));
        register("attrFlat", attrFlatCheck);

        languageCombo = new Combo(row, SWT.READ_ONLY);
        languageCombo.setItems(new String[] { "TR", "EN" });
        languageCombo.select(isTurkishLocale() ? 0 : 1);
        languageCombo.setToolTipText("TR / EN");
        languageCombo.addSelectionListener(onSelect(event -> {
            applyLanguage();
            scheduleConvert();
        }));
    }

    private void createActionRow(Composite parent) {
        Composite row = createRow(parent);

        createButton(row, "btnConvert", "tipConvert", event -> convert());
        createButton(row, "btnFormat", "tipFormat", event -> formatInput());
        createButton(row, "btnValidate", "tipValidate", event -> validate());
        createButton(row, "btnDdic", "tipDdic", event -> openDdicDialog());
        createButton(row, "btnCopy", "tipCopy", event -> copyActiveTab());
        createButton(row, "btnInsert", "tipInsert", event -> insertActiveTab());
        createButton(row, "btnSave", "tipSave", event -> saveActiveTab());
        createButton(row, "btnSample", "tipSample", event -> loadSample());
    }

    private void createWorkspace(Composite parent) {
        SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite left = new Composite(sash, SWT.NONE);
        left.setLayout(new GridLayout(1, false));
        Label inputLabel = new Label(left, SWT.NONE);
        inputLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        register("inputLabel", inputLabel);

        input = new StyledText(left, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        input.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        input.setFont(palette.monoFont);
        input.setBackground(palette.background);
        input.setForeground(palette.foreground);
        input.setTabs(2);
        input.addModifyListener(event -> {
            updateLinesInfo();
            scheduleConvert();
        });

        Composite right = new Composite(sash, SWT.NONE);
        right.setLayout(new GridLayout(1, false));
        outputFolder = new TabFolder(right, SWT.NONE);
        outputFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        for (int i = 0; i < TAB_KEYS.length; i++) {
            if ("validation".equals(TAB_KEYS[i])) {
                createValidationTab();
            } else {
                createCodeTab(TAB_KEYS[i]);
            }
        }
        sash.setWeights(new int[] { 45, 55 });
    }

    private void createStatusRow(Composite parent) {
        Composite row = new Composite(parent, SWT.NONE);
        row.setLayout(new GridLayout(3, false));
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        statusLabel = new Label(row, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        linesLabel = new Label(row, SWT.NONE);
        linesLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        generationLabel = new Label(row, SWT.RIGHT);
        generationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    }

    private void createCodeTab(String key) {
        TabItem item = new TabItem(outputFolder, SWT.NONE);
        AbapCodeViewer viewer = new AbapCodeViewer(outputFolder, palette);
        item.setControl(viewer.getControl());
        tabItems.put(key, item);
        viewers.put(key, viewer);
    }

    private void createValidationTab() {
        TabItem item = new TabItem(outputFolder, SWT.NONE);
        validationText = new StyledText(outputFolder, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        validationText.setEditable(false);
        validationText.setFont(palette.monoFont);
        validationText.setBackground(palette.background);
        validationText.setForeground(palette.foreground);
        validationText.setTabs(2);
        item.setControl(validationText);
        tabItems.put("validation", item);
    }

    private void convert() {
        if (input == null || input.isDisposed()) {
            return;
        }
        String text = input.getText();
        if (text.trim().isEmpty()) {
            clearGenerated();
            setStatus(t("statusEmpty"), false);
            showValidationMessage(t("msgEmptyInput"), false);
            return;
        }
        Format format = Format.detect(text);
        selectFormatCombo(format);
        GenerateOptions options = readOptions();
        try {
            GenerateResult result = Converter.generate(text, format, options);
            setViewerText("types", result.types);
            setViewerText("mapping", result.mapping);
            setViewerText("ixml", result.ixml);
            setViewerText("st", result.simpleTransformation);
            setViewerText("template", result.template);
            setViewerText("ddic", buildDdicPreview(text, format));
            lastTypeCount = result.typeCount;
            lastMappingCount = result.mappingCount;
            refreshTabLabels();
            showValidationMessage((format == Format.XML ? "XML" : "JSON") + " ✓ " + t("validationOk"), true);
            setStatus((format == Format.XML ? t("statusValidXml") : t("statusValidJson")) + " ✓", false);
            generationLabel.setText(result.typeCount + " " + t("infoTypes") + ", "
                    + result.mappingCount + " " + t("infoMapping") + " • " + result.elapsedMillis + " ms");
        } catch (JsonParseException e) {
            clearGenerated();
            showValidationMessage("JSON — " + e.getMessage() + "\n\n" + t("jsonHint"), false);
            setStatus(t("statusInvalidJson"), true);
            selectTab("validation");
        } catch (Exception e) {
            clearGenerated();
            String label = format == Format.XML ? "XML" : "JSON";
            showValidationMessage(label + " — " + describeError(e) + "\n\n"
                    + t(format == Format.XML ? "xmlHint" : "jsonHint"), false);
            setStatus(format == Format.XML ? t("statusInvalidXml") : t("statusInvalidJson"), true);
            selectTab("validation");
        }
    }

    private void formatInput() {
        String text = input.getText();
        if (text.trim().isEmpty()) {
            setStatus(t("msgEmptyInput"), false);
            return;
        }
        try {
            input.setText(Converter.prettify(text, Format.detect(text)));
            setStatus(t("msgFormatOk"), false);
        } catch (Exception e) {
            Format detected = Format.detect(text);
            setStatus((detected == Format.XML ? "XML" : "JSON") + " " + t("msgFormatFailed") + ": "
                    + describeError(e), true);
        }
    }

    private void validate() {
        String text = input.getText();
        if (text.trim().isEmpty()) {
            setStatus(t("msgEmptyInput"), false);
            return;
        }
        Format format = Format.detect(text);
        List<String> errors = Converter.parseErrorLines(text, format);
        selectTab("validation");
        if (errors.isEmpty()) {
            showValidationMessage("✓ " + t("validationOk"), true);
            setStatus((format == Format.XML ? t("statusValidXml") : t("statusValidJson")) + " ✓", false);
        } else {
            String label = format == Format.XML ? "XML" : "JSON";
            showValidationMessage(label + " — " + String.join("\n", errors) + "\n\n"
                    + t(format == Format.XML ? "xmlHint" : "jsonHint"), false);
            setStatus(format == Format.XML ? t("statusInvalidXml") : t("statusInvalidJson"), true);
        }
    }

    private void copyActiveTab() {
        String content = activeTabContent();
        if (content.trim().isEmpty()) {
            setStatus(t("msgNoContent"), false);
            return;
        }
        Clipboard clipboard = new Clipboard(getSite().getShell().getDisplay());
        try {
            clipboard.setContents(new Object[] { content }, new Transfer[] { TextTransfer.getInstance() });
            setStatus(t("msgCopied"), false);
        } catch (RuntimeException e) {
            setStatus(t("msgCopyFailed"), true);
        } finally {
            clipboard.dispose();
        }
    }

    private void insertActiveTab() {
        String content = activeTabContent();
        if (content.trim().isEmpty()) {
            setStatus(t("msgNoContent"), false);
            return;
        }
        EditorInserter.Result result = EditorInserter.insertAtCursor(content);
        if (result.success) {
            setStatus(t("msgInserted"), false);
        } else {
            setStatus(t("msgInsertFailed") + ": " + result.message, true);
            MessageDialog.openWarning(getSite().getShell(), t("btnInsert"), result.message);
        }
    }

    private void saveActiveTab() {
        String content = activeTabContent();
        if (content.trim().isEmpty()) {
            setStatus(t("msgUnsaved"), false);
            return;
        }
        FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { "*.abap", "*.txt", "*.*" });
        dialog.setFileName(activeTabKey() + ".abap");
        String path = dialog.open();
        if (path == null) {
            return;
        }
        try {
            Files.writeString(Path.of(path), content, StandardCharsets.UTF_8);
            setStatus(t("msgSaved") + ": " + path, false);
        } catch (IOException e) {
            setStatus(t("msgSaveFailed") + ": " + e.getMessage(), true);
        }
    }

    private void loadSample() {
        boolean xml = formatCombo.getSelectionIndex() == 1;
        selectFormatCombo(xml ? Format.XML : Format.JSON);
        input.setText(xml ? SAMPLE_XML : SAMPLE_JSON);
    }

    private void openDdicDialog() {
        if (input == null || input.getText().trim().isEmpty()) {
            setStatus(t("msgEmptyInput"), false);
            return;
        }
        if (!AdtRestClient.isAvailable()) {
            MessageDialog.openWarning(getSite().getShell(), t("ddicTitle"),
                    t("ddicNoAdt") + "\n\n" + AdtRestClient.unavailableReason());
            return;
        }
        String text = input.getText();
        Format format = Format.detect(text);
        DdicCreateDialog dialog = new DdicCreateDialog(getSite().getShell(), text, format,
                ddicRootBase(text, format), isTurkish());
        dialog.open();
    }

    private String ddicRootBase(String text, Format format) {
        String root = rootField.getText().trim().toUpperCase(Locale.ROOT);
        if (!root.isEmpty()) {
            return root;
        }
        try {
            if (format == Format.XML) {
                return XmlSupport.parse(text).getDocumentElement().getTagName().toUpperCase(Locale.ROOT);
            }
            Object json = JsonParser.parse(text);
            if (json instanceof Map && ((Map<?, ?>) json).size() == 1) {
                return String.valueOf(((Map<?, ?>) json).keySet().iterator().next())
                        .toUpperCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
            // önizleme yoksa ROOT kullanılır
        }
        return "ROOT";
    }

    private String buildDdicPreview(String text, Format format) {
        try {
            DdicPlanOptions options = new DdicPlanOptions();
            options.snakeCase = snakeCheck.getSelection();
            options.turkish = isTurkish();
            options.packageName = "$TMP";
            String base = ddicRootBase(text, format);
            DdicModel.Entity root;
            if (format == Format.XML) {
                root = DdicModelBuilder.fromXml(XmlSupport.parse(text).getDocumentElement(), base, base, options);
            } else {
                Object json = JsonParser.parse(text);
                Object node = json;
                if (json instanceof Map && ((Map<?, ?>) json).size() == 1) {
                    Object value = ((Map<?, ?>) json).values().iterator().next();
                    if (value instanceof Map) {
                        node = value;
                    }
                }
                root = DdicModelBuilder.fromJson(node, base, base, options);
            }
            List<DdicObjectPlan> plans = DdicPlanner.plan(root, options);
            return DdicPlanner.preview(plans);
        } catch (Exception e) {
            return t("ddicPreviewEmpty");
        }
    }

    private GenerateOptions readOptions() {
        GenerateOptions options = new GenerateOptions();
        options.root = rootField.getText().trim().toUpperCase(Locale.ROOT);
        options.structPrefix = valueOrDefault(structPrefixField.getText(), "TS_");
        options.tablePrefix = valueOrDefault(tablePrefixField.getText(), "TT_");
        options.snakeCase = snakeCheck.getSelection();
        options.suffix = suffixField.getText().trim();
        options.suffixToAbap = suffixTargetCombo.getSelectionIndex() == 0;
        options.attrFlat = attrFlatCheck.getSelection();
        options.turkish = languageCombo.getSelectionIndex() == 0;
        return options;
    }

    private void scheduleConvert() {
        if (pendingConvert == null) {
            pendingConvert = this::convert;
        }
        if (input == null || input.isDisposed()) {
            return;
        }
        if (getSite() == null || getSite().getShell() == null || getSite().getShell().isDisposed()) {
            return;
        }
        getSite().getShell().getDisplay().timerExec(-1, pendingConvert);
        getSite().getShell().getDisplay().timerExec(300, pendingConvert);
    }

    private void clearGenerated() {
        for (String key : viewers.keySet()) {
            viewers.get(key).setText("");
        }
        lastTypeCount = 0;
        lastMappingCount = 0;
        refreshTabLabels();
        generationLabel.setText("");
    }

    private void setViewerText(String key, String content) {
        AbapCodeViewer viewer = viewers.get(key);
        if (viewer != null) {
            viewer.setText(content == null ? "" : content);
        }
    }

    private void showValidationMessage(String message, boolean ok) {
        if (validationText == null || validationText.isDisposed()) {
            return;
        }
        validationText.setText(message == null ? "" : message);
        validationText.setForeground(ok ? palette.stringLiteral : palette.foreground);
    }

    private void selectTab(String key) {
        TabItem item = tabItems.get(key);
        if (item != null && !item.isDisposed()) {
            outputFolder.setSelection(item);
        }
    }

    private String activeTabKey() {
        int index = outputFolder.getSelectionIndex();
        return index < 0 || index >= TAB_KEYS.length ? "types" : TAB_KEYS[index];
    }

    private String activeTabContent() {
        String key = activeTabKey();
        if ("validation".equals(key)) {
            return validationText == null ? "" : validationText.getText();
        }
        AbapCodeViewer viewer = viewers.get(key);
        return viewer == null ? "" : viewer.getText();
    }

    private void selectFormatCombo(Format format) {
        int index = format == Format.XML ? 1 : 0;
        if (formatCombo.getSelectionIndex() != index) {
            formatCombo.select(index);
        }
    }

    private void updateLinesInfo() {
        if (input == null || input.isDisposed() || linesLabel == null) {
            return;
        }
        linesLabel.setText(input.getLineCount() + " " + t("lines"));
    }

    private void setStatus(String message, boolean error) {
        if (statusLabel != null && !statusLabel.isDisposed()) {
            statusLabel.setText(message);
            statusLabel.setForeground(error ? palette.number : palette.foreground);
        }
    }

    private void applyLanguage() {
        boolean tr = isTurkish();
        for (Map.Entry<String, Control> entry : translatable.entrySet()) {
            String text = Messages.get(tr, entry.getKey());
            Control control = entry.getValue();
            if (control instanceof Label) {
                ((Label) control).setText(text);
            } else if (control instanceof Button) {
                ((Button) control).setText(text);
            }
        }
        for (Map.Entry<String, Button> entry : tooltipButtons.entrySet()) {
            entry.getValue().setToolTipText(Messages.get(tr, entry.getKey()));
        }
        suffixTargetCombo.setItem(0, Messages.get(tr, "suffixDisabled"));
        suffixTargetCombo.setItem(1, Messages.get(tr, "suffixJson"));
        refreshTabLabels();
        updateLinesInfo();
    }

    private void refreshTabLabels() {
        for (int i = 0; i < TAB_KEYS.length; i++) {
            TabItem item = tabItems.get(TAB_KEYS[i]);
            if (item == null || item.isDisposed()) {
                continue;
            }
            String label = t(TAB_LABEL_KEYS[i]);
            if ("types".equals(TAB_KEYS[i]) && lastTypeCount > 0) {
                label += " (" + lastTypeCount + ")";
            } else if ("mapping".equals(TAB_KEYS[i]) && lastMappingCount > 0) {
                label += " (" + lastMappingCount + ")";
            }
            item.setText(label);
        }
    }

    private Composite createRow(Composite parent) {
        Composite row = new Composite(parent, SWT.NONE);
        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.spacing = 6;
        layout.marginTop = 2;
        layout.marginBottom = 2;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        row.setLayout(layout);
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return row;
    }

    private void addSettingLabel(Composite row, String key) {
        Label label = new Label(row, SWT.NONE);
        register(key, label);
    }

    private Text createText(Composite row, String value, int width, boolean convertOnChange) {
        Text field = new Text(row, SWT.BORDER);
        field.setText(value);
        RowData data = new RowData();
        data.width = width;
        field.setLayoutData(data);
        if (convertOnChange) {
            field.addModifyListener(event -> scheduleConvert());
        }
        return field;
    }

    private Button createButton(Composite row, String labelKey, String tooltipKey,
            java.util.function.Consumer<SelectionEvent> listener) {
        Button button = new Button(row, SWT.PUSH);
        register(labelKey, button);
        tooltipButtons.put(tooltipKey, button);
        button.addSelectionListener(onSelect(listener));
        return button;
    }

    private void register(String key, Control control) {
        translatable.put(key, control);
    }

    private String t(String key) {
        return Messages.get(isTurkish(), key);
    }

    private boolean isTurkish() {
        return languageCombo != null && languageCombo.getSelectionIndex() == 0;
    }

    private static boolean isTurkishLocale() {
        return "tr".equalsIgnoreCase(Locale.getDefault().getLanguage());
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    private static String describeError(Exception error) {
        if (error instanceof org.xml.sax.SAXParseException) {
            org.xml.sax.SAXParseException sax = (org.xml.sax.SAXParseException) error;
            return sax.getMessage() + " (satır " + sax.getLineNumber() + ", sütun " + sax.getColumnNumber() + ")";
        }
        return String.valueOf(error.getMessage());
    }

    private static SelectionListener onSelect(java.util.function.Consumer<org.eclipse.swt.events.SelectionEvent> action) {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                action.accept(event);
            }
        };
    }
}
