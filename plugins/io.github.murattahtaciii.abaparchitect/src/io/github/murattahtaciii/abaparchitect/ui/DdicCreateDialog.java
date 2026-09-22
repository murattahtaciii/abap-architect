package io.github.murattahtaciii.abaparchitect.ui;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import io.github.murattahtaciii.abaparchitect.core.Format;
import io.github.murattahtaciii.abaparchitect.core.JsonParser;
import io.github.murattahtaciii.abaparchitect.core.NameUtil;
import io.github.murattahtaciii.abaparchitect.core.XmlSupport;
import io.github.murattahtaciii.abaparchitect.ddic.DdicKind;
import io.github.murattahtaciii.abaparchitect.ddic.DdicModel;
import io.github.murattahtaciii.abaparchitect.ddic.DdicModelBuilder;
import io.github.murattahtaciii.abaparchitect.ddic.DdicObjectPlan;
import io.github.murattahtaciii.abaparchitect.ddic.DdicPlanOptions;
import io.github.murattahtaciii.abaparchitect.ddic.DdicPlanner;
import io.github.murattahtaciii.abaparchitect.ui.adt.AdtCatalog;
import io.github.murattahtaciii.abaparchitect.ddic.DdicXmlBuilder;
import io.github.murattahtaciii.abaparchitect.ui.adt.AdtException;
import io.github.murattahtaciii.abaparchitect.ui.adt.AdtRestClient;
import io.github.murattahtaciii.abaparchitect.ui.adt.DdicCreator;

public class DdicCreateDialog extends TitleAreaDialog {

    private final String inputText;
    private final Format format;
    private final String rootBase;
    private final boolean turkish;

    private final DdicPlanOptions options = new DdicPlanOptions();
    private List<DdicObjectPlan> plans = new ArrayList<>();
    private IProject[] adtProjects = new IProject[0];

    private Combo projectCombo;
    private Text packageText;
    private Text transportText;
    private Text modulePrefixText;
    private Text structureCodeText;
    private Text tableTypeCodeText;
    private Text tableCodeText;
    private Text elementCodeText;
    private Text domainCodeText;
    private Button tableCheck;
    private Button elementsCheck;
    private Table table;
    private Text logText;
    private Label summaryLabel;
    private Button editButton;
    private final Map<DdicObjectPlan, TableItem> rows = new IdentityHashMap<>();
    private boolean updating;
    private boolean catalogRunning;

    public DdicCreateDialog(Shell parentShell, String inputText, Format format, String rootBase,
            boolean turkish) {
        super(parentShell);
        this.inputText = inputText;
        this.format = format;
        this.rootBase = rootBase == null || rootBase.isEmpty() ? "ROOT" : rootBase.toUpperCase(Locale.ROOT);
        this.turkish = turkish;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    private String t(String key) {
        return Messages.get(turkish, key);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        setTitle(t("ddicTitle"));
        setMessage(t("ddicMessage"));

        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(1, false));

        createLinkRow(container);

        if (!AdtRestClient.isAvailable()) {
            Label warning = new Label(container, SWT.WRAP);
            warning.setText(t("ddicNoAdt") + "\n\n" + AdtRestClient.unavailableReason());
            warning.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
            warning.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            return area;
        }

        createTargetGroup(container);
        createNamingGroup(container);
        createTableGroup(container);
        createLogGroup(container);

        updating = true;
        refreshPlan();
        updating = false;
        return area;
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
        links.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
                openUrl(event.text);
            }
        });
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

    private void createTargetGroup(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        group.setLayout(new GridLayout(4, false));

        new Label(group, SWT.NONE).setText(t("ddicProject"));
        projectCombo = new Combo(group, SWT.READ_ONLY);
        projectCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        new Label(group, SWT.NONE).setText(t("ddicPackage"));
        packageText = new Text(group, SWT.BORDER);
        packageText.setText("$TMP");
        packageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        new Label(group, SWT.NONE).setText(t("ddicTransport"));
        transportText = new Text(group, SWT.BORDER);
        transportText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        transportText.setEnabled(false);

        Button listPackages = new Button(group, SWT.PUSH);
        listPackages.setText(t("ddicListPackages"));
        listPackages.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        listPackages.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
                listPackages();
            }
        });
        Button listTransports = new Button(group, SWT.PUSH);
        listTransports.setText(t("ddicListTransports"));
        listTransports.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        listTransports.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
                listTransports();
            }
        });

        packageText.addModifyListener(event -> {
            String pkg = packageText.getText().trim();
            transportText.setEnabled(!"$TMP".equalsIgnoreCase(pkg));
        });
        loadProjects();
    }

    /** Paket arama (RIS): arka planda çalışır, seçim dialogu ile doldurur. */
    private void listPackages() {
        IProject project = selectedProject();
        if (project == null) {
            MessageDialog.openWarning(getShell(), t("ddicTitle"), t("ddicNoProjects"));
            return;
        }
        String mask = packageText.getText().trim();
        if ("$TMP".equalsIgnoreCase(mask)) {
            mask = "Z*";
        }
        final String queryMask = mask;
        runCatalogTask(t("ddicListPackages"), () -> AdtCatalog.listPackages(project, queryMask),
                entry -> packageText.setText(entry.id));
    }

    /** Kullanıcının değiştirilebilir transportlarını arka planda listeler. */
    private void listTransports() {
        IProject project = selectedProject();
        if (project == null) {
            MessageDialog.openWarning(getShell(), t("ddicTitle"), t("ddicNoProjects"));
            return;
        }
        runCatalogTask(t("ddicListTransports"), () -> AdtCatalog.listTransports(project),
                entry -> transportText.setText(entry.id));
    }

    /**
     * Katalog sorgusunu UI thread'inde (busy cursor) çalıştırır; böylece
     * gerektiğinde ADT Log On diyalogu açılabilir. Sonuç seçim dialoguyla alınır.
     */
    private void runCatalogTask(String title,
            java.util.concurrent.Callable<java.util.List<AdtCatalog.Entry>> work,
            java.util.function.Consumer<AdtCatalog.Entry> onPicked) {
        if (catalogRunning) {
            logText.setText(t("ddicCatalogBusy"));
            return;
        }
        catalogRunning = true;
        try {
            BusyIndicator.showWhile(getShell().getDisplay(), () -> {
                try {
                    java.util.List<AdtCatalog.Entry> entries = work.call();
                    AdtCatalog.Entry selected = choose(title, entries(entries));
                    if (selected != null) {
                        onPicked.accept(selected);
                    }
                } catch (Exception e) {
                    logText.setText(title + " -> " + e.getMessage());
                }
            });
        } finally {
            catalogRunning = false;
        }
    }

    private AdtCatalog.Entry[] entries(java.util.List<AdtCatalog.Entry> list) {
        return list.toArray(new AdtCatalog.Entry[0]);
    }

    private AdtCatalog.Entry choose(String title, AdtCatalog.Entry[] entries) {
        org.eclipse.ui.dialogs.ElementListSelectionDialog dialog =
                new org.eclipse.ui.dialogs.ElementListSelectionDialog(getShell(),
                        new org.eclipse.jface.viewers.LabelProvider() {

                            @Override
                            public String getText(Object element) {
                                return ((AdtCatalog.Entry) element).label();
                            }
                        });
        dialog.setTitle(title);
        dialog.setMessage(t("ddicPick"));
        dialog.setElements(entries);
        dialog.setMultipleSelection(false);
        if (dialog.open() != org.eclipse.jface.window.Window.OK) {
            return null;
        }
        Object result = dialog.getFirstResult();
        return result instanceof AdtCatalog.Entry ? (AdtCatalog.Entry) result : null;
    }

    private void createNamingGroup(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        group.setLayout(new GridLayout(6, false));

        new Label(group, SWT.NONE).setText(t("ddicModulePrefix"));
        modulePrefixText = createSmallText(group, options.modulePrefix);
        new Label(group, SWT.NONE).setText(t("ddicCodeStructure"));
        structureCodeText = createSmallText(group, options.structureCode);
        new Label(group, SWT.NONE).setText(t("ddicCodeTableType"));
        tableTypeCodeText = createSmallText(group, options.tableTypeCode);

        new Label(group, SWT.NONE).setText(t("ddicCodeTable"));
        tableCodeText = createSmallText(group, options.tableCode);
        new Label(group, SWT.NONE).setText(t("ddicCodeElement"));
        elementCodeText = createSmallText(group, options.elementCode);
        new Label(group, SWT.NONE).setText(t("ddicCodeDomain"));
        domainCodeText = createSmallText(group, options.domainCode);

        tableCheck = new Button(group, SWT.CHECK);
        tableCheck.setText(t("ddicCreateTable"));
        tableCheck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        elementsCheck = new Button(group, SWT.CHECK);
        elementsCheck.setText(t("ddicCreateElements"));
        elementsCheck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

        for (Text input : List.of(modulePrefixText, structureCodeText, tableTypeCodeText, tableCodeText,
                elementCodeText, domainCodeText)) {
            input.addModifyListener(event -> {
                if (!updating) {
                    refreshPlan();
                }
            });
        }
        tableCheck.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                if (!updating) {
                    refreshPlan();
                }
            }
        });
        elementsCheck.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                if (!updating) {
                    refreshPlan();
                }
            }
        });
    }

    private Text createSmallText(Composite parent, String value) {
        Text text = new Text(parent, SWT.BORDER);
        text.setText(value);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.widthHint = 70;
        text.setLayoutData(data);
        return text;
    }

    private void createTableGroup(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        group.setLayout(new GridLayout(1, false));

        summaryLabel = new Label(group, SWT.NONE);
        Label hint = new Label(group, SWT.WRAP);
        hint.setText(t("ddicHint"));
        hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        table = new Table(group, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        String[] columns = { t("ddicColKind"), t("ddicColName"), t("ddicColDesc"), t("ddicColStatus") };
        int[] widths = { 130, 240, 220, 280 };
        for (int i = 0; i < columns.length; i++) {
            TableColumn column = new TableColumn(table, SWT.LEFT);
            column.setText(columns[i]);
            column.setWidth(widths[i]);
        }
        table.addListener(SWT.MouseDown, event -> {
            TableItem item = table.getItem(new Point(event.x, event.y));
            if (item == null) {
                return;
            }
            int column = columnAt(event.x);
            if (column >= 0 && column <= 2) {
                openEditor(table.indexOf(item), item, column);
            }
        });
        table.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                if (editButton != null && !editButton.isDisposed()) {
                    editButton.setEnabled(table.getSelectionIndex() >= 0);
                }
            }
        });

        Composite buttons = new Composite(group, SWT.NONE);
        buttons.setLayout(new GridLayout(2, false));
        Button resuggest = new Button(buttons, SWT.PUSH);
        resuggest.setText(t("ddicResuggest"));
        resuggest.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                refreshPlan();
            }
        });
        editButton = new Button(buttons, SWT.PUSH);
        editButton.setText(t("ddicEdit"));
        editButton.setEnabled(false);
        editButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                editSelected();
            }
        });
    }

    private void createLogGroup(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        group.setLayout(new GridLayout(1, false));

        new Label(group, SWT.NONE).setText(t("ddicLog"));
        logText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.heightHint = 90;
        logText.setLayoutData(data);
    }

    private void loadProjects() {
        List<IProject> found = new ArrayList<>();
        for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            if (project.isAccessible() && AdtRestClient.isAdtProject(project)) {
                found.add(project);
            }
        }
        adtProjects = found.toArray(new IProject[0]);
        String[] names = new String[adtProjects.length];
        for (int i = 0; i < adtProjects.length; i++) {
            names[i] = adtProjects[i].getName();
        }
        projectCombo.setItems(names);
        if (names.length > 0) {
            projectCombo.select(0);
            selectActiveProject();
        } else {
            setMessage(t("ddicNoProjects"));
        }
    }

    private void selectActiveProject() {
        try {
            IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                    .getActiveEditor();
            if (editor == null) {
                return;
            }
            IResource resource = editor.getEditorInput().getAdapter(IResource.class);
            if (resource == null) {
                return;
            }
            for (int i = 0; i < adtProjects.length; i++) {
                if (adtProjects[i].equals(resource.getProject())) {
                    projectCombo.select(i);
                    return;
                }
            }
        } catch (RuntimeException ignored) {
            // aktif editör yoksa ilk proje seçili kalır
        }
    }

    private IProject selectedProject() {
        int index = projectCombo.getSelectionIndex();
        return index < 0 || index >= adtProjects.length ? null : adtProjects[index];
    }

    /** Eclipse'in çalıştığı dile göre master language: TR ya da EN. */
    private static String eclipseLanguage() {
        try {
            String nl = org.eclipse.core.runtime.Platform.getNL();
            if (nl != null && nl.toLowerCase(Locale.ROOT).startsWith("tr")) {
                return "TR";
            }
        } catch (Exception ignored) {
            // Platform yoksa sistem yereline bakılır
        }
        return Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT).startsWith("tr") ? "TR" : "EN";
    }

    private DdicPlanOptions readOptions() {        options.modulePrefix = modulePrefixText.getText().trim().toUpperCase(Locale.ROOT);
        options.structureCode = structureCodeText.getText().trim().toUpperCase(Locale.ROOT);
        options.tableTypeCode = tableTypeCodeText.getText().trim().toUpperCase(Locale.ROOT);
        options.tableCode = tableCodeText.getText().trim().toUpperCase(Locale.ROOT);
        options.elementCode = elementCodeText.getText().trim().toUpperCase(Locale.ROOT);
        options.domainCode = domainCodeText.getText().trim().toUpperCase(Locale.ROOT);
        options.packageName = packageText.getText().trim().toUpperCase(Locale.ROOT);
        options.transport = transportText.getText().trim().toUpperCase(Locale.ROOT);
        options.createTable = tableCheck.getSelection();
        options.createDataElements = elementsCheck.getSelection();
        options.addClientField = true;
        options.language = eclipseLanguage();
        options.turkish = turkish;
        return options;
    }

    private void refreshPlan() {
        if (table == null || table.isDisposed()) {
            return;
        }
        DdicPlanOptions current = readOptions();
        try {
            DdicModel.Entity root = buildModel(current);
            plans = root == null ? new ArrayList<>() : DdicPlanner.plan(root, current);
        } catch (Exception e) {
            plans = new ArrayList<>();
            logText.setText("Plan oluşturulamadı: " + e.getMessage());
        }
        rows.clear();
        table.removeAll();
        for (DdicObjectPlan plan : plans) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, plan.kind.name());
            item.setText(1, plan.name);
            item.setText(2, plan.description);
            rows.put(plan, item);
            updateRow(plan);
        }
        long invalid = plans.stream().filter(p -> p.validationError() != null).count();
        summaryLabel.setText(plans.size() + " " + t("ddicPlanned")
                + (invalid > 0 ? "  (" + invalid + " hatalı isim)" : ""));
        updateCreateButton();
        if (editButton != null) {
            if (table.getItemCount() > 0) {
                table.select(0);
                editButton.setEnabled(true);
            } else {
                editButton.setEnabled(false);
            }
        }
    }

    private DdicModel.Entity buildModel(DdicPlanOptions current) throws Exception {
        if (inputText == null || inputText.trim().isEmpty()) {
            return null;
        }
        if (format == Format.XML) {
            return DdicModelBuilder.fromXml(XmlSupport.parse(inputText).getDocumentElement(), rootBase,
                    rootBase, current);
        }
        Object json = JsonParser.parse(inputText);
        if (json instanceof Map && ((Map<?, ?>) json).size() == 1 && "ROOT".equals(rootBase)) {
            Map.Entry<?, ?> entry = ((Map<?, ?>) json).entrySet().iterator().next();
            Object value = entry.getValue();
            if (value instanceof Map) {
                return DdicModelBuilder.fromJson(value, NameUtil.toV(String.valueOf(entry.getKey()), true),
                        String.valueOf(entry.getKey()), current);
            }
        }
        return DdicModelBuilder.fromJson(json, rootBase, rootBase, current);
    }

    private int columnAt(int x) {
        int offset = table.getHorizontalBar() == null ? 0 : table.getHorizontalBar().getSelection();
        int total = 0;
        for (int i = 0; i < table.getColumnCount(); i++) {
            total += table.getColumn(i).getWidth();
            if (x + offset < total) {
                return i;
            }
        }
        return table.getColumnCount() - 1;
    }

    private void openEditor(int index, TableItem item, int column) {
        if (index < 0 || index >= plans.size()) {
            return;
        }
        DdicObjectPlan plan = plans.get(index);
        if (column == 0) {
            editKind(item, plan);
        } else if (column == 1) {
            int max = plan.kind.maxLength;
            editText(item, column, plan.name, value -> {
                String name = value.toUpperCase(Locale.ROOT);
                plan.name = name.length() > max ? name.substring(0, max) : name;
                DdicPlanner.refreshSource(plan, readOptions());
                updateRow(plan);
            }, max);
        } else if (column == 2) {
            editText(item, column, plan.description, value -> {
                plan.description = value;
                DdicPlanner.refreshSource(plan, readOptions());
                updateRow(plan);
            }, 55);
        }
    }

    private void editKind(TableItem item, DdicObjectPlan plan) {
        TableEditor editor = new TableEditor(table);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        editor.minimumWidth = 110;
        Combo combo = new Combo(table, SWT.READ_ONLY);
        for (DdicKind kind : DdicKind.values()) {
            combo.add(kind.name());
        }
        Runnable commit = () -> {
            if (combo.isDisposed()) {
                return;
            }
            int index = combo.getSelectionIndex();
            combo.dispose();
            if (index >= 0) {
                convertKind(plan, DdicKind.values()[index]);
            }
        };
        combo.addFocusListener(FocusListener.focusLostAdapter(event -> commit.run()));
        combo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                commit.run();
            }
        });
        editor.setEditor(combo, item, 0);
        combo.select(plan.kind.ordinal());
        combo.setFocus();
    }

    private void convertKind(DdicObjectPlan plan, DdicKind kind) {
        if (plan.kind == kind) {
            return;
        }
        if (kind == DdicKind.STRUCTURE || kind == DdicKind.TABLE) {
            if (plan.fields == null) {
                plan.fields = new ArrayList<>();
            }
        } else if (kind == DdicKind.TABLE_TYPE) {
            if (plan.rowType == null || plan.rowType.isEmpty()) {
                plan.rowType = firstStructureName();
            }
            plan.builtinRowType = DdicXmlBuilder.BUILTIN_ROW_TYPES
                    .contains(plan.rowType.toUpperCase(Locale.ROOT));
        } else if (kind == DdicKind.DOMAIN) {
            if (plan.domainType == null) {
                plan.domainType = "CHAR";
                plan.length = 255;
                plan.decimals = 0;
            }
        } else if (kind == DdicKind.DATA_ELEMENT) {
            if (plan.domainName == null || plan.domainName.isEmpty()) {
                plan.domainName = firstDomainName();
            }
        }
        plan.kind = kind;
        DdicPlanner.refreshSource(plan, readOptions());
        updateRow(plan);
    }

    private String firstStructureName() {
        for (DdicObjectPlan plan : plans) {
            if (plan.kind == DdicKind.STRUCTURE) {
                return plan.name;
            }
        }
        return "STRING";
    }

    private String firstDomainName() {
        for (DdicObjectPlan plan : plans) {
            if (plan.kind == DdicKind.DOMAIN) {
                return plan.name;
            }
        }
        return "";
    }

    private void editText(TableItem item, int column, String initial,
            java.util.function.Consumer<String> apply, int maxLength) {
        TableEditor editor = new TableEditor(table);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        editor.minimumWidth = 50;
        Text text = new Text(table, SWT.NONE);
        if (maxLength > 0) {
            text.addVerifyListener(event -> {
                String current = text.getText();
                String candidate = current.substring(0, event.start) + event.text
                        + current.substring(event.end);
                if (candidate.length() > maxLength) {
                    event.doit = false;
                    logText.setText(t("limitWarn") + " (" + maxLength + ")");
                    getShell().getDisplay().beep();
                }
            });
        }
        Runnable commit = () -> {
            if (text.isDisposed()) {
                return;
            }
            String value = text.getText();
            text.dispose();
            apply.accept(value);
        };
        text.addFocusListener(FocusListener.focusLostAdapter(event -> commit.run()));
        text.addTraverseListener(event -> {
            if (event.detail == SWT.TRAVERSE_RETURN) {
                commit.run();
            }
        });
        editor.setEditor(text, item, column);
        text.setText(initial == null ? "" : initial);
        text.selectAll();
        text.setFocus();
    }

    private void updateRow(DdicObjectPlan plan) {
        TableItem item = rows.get(plan);
        if (item == null) {
            return;
        }
        item.setText(0, plan.kind.name());
        item.setText(1, plan.name);
        item.setText(2, plan.description);
        String error = plan.validationError();
        item.setText(3, error == null ? t("ddicStatusPending") : error);
        item.setForeground(error == null ? null : table.getDisplay().getSystemColor(SWT.COLOR_RED));
        updateCreateButton();
    }

    /** İsimlendirme kuralı hatalı bir satır varsa Oluştur butonu pasifleşir. */
    private void updateCreateButton() {
        Button button = getButton(2);
        if (button == null || button.isDisposed()) {
            return;
        }
        boolean invalid = plans.stream().anyMatch(p -> p.validationError() != null);
        button.setEnabled(AdtRestClient.isAvailable() && !plans.isEmpty() && !invalid);
    }

    private void editSelected() {
        int index = table.getSelectionIndex();
        if (index < 0 || index >= plans.size()) {
            return;
        }
        DdicObjectPlan plan = plans.get(index);
        DdicFieldsDialog dialog = new DdicFieldsDialog(getShell(), plan, plans, readOptions(), turkish);
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            DdicPlanner.refreshSource(plan, readOptions());
            updateRow(plan);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, 2, t("ddicCreate"), true);
        createButton(parent, 3, t("ddicClose"), false);
        getButton(2).setEnabled(AdtRestClient.isAvailable());
        getButton(3).addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                close();
            }
        });
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == 2) {
            runCreate();
        } else if (buttonId == 3) {
            close();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    private void runCreate() {
        DdicPlanOptions current = readOptions();
        if (current.packageName.isEmpty()) {
            MessageDialog.openWarning(getShell(), t("ddicTitle"), t("ddicNeedPackage"));
            return;
        }
        if (!"$TMP".equalsIgnoreCase(current.packageName) && current.transport.isEmpty()) {
            MessageDialog.openWarning(getShell(), t("ddicTitle"), t("ddicNeedTransport"));
            return;
        }
        IProject project = selectedProject();
        if (project == null) {
            MessageDialog.openWarning(getShell(), t("ddicTitle"), t("ddicNoProjects"));
            return;
        }
        List<DdicObjectPlan> selected = new ArrayList<>();
        for (DdicObjectPlan plan : plans) {
            if (plan.selected) {
                selected.add(plan);
            }
        }
        if (selected.isEmpty()) {
            return;
        }
        List<String> log = new ArrayList<>();
        log.add(t("ddicRunning"));
        getButton(2).setEnabled(false);
        BusyIndicator.showWhile(getShell().getDisplay(), () -> {
            try {
                List<DdicCreator.Result> results = DdicCreator.createAll(project, current, selected,
                        log::add);
                for (DdicCreator.Result result : results) {
                    TableItem item = rows.get(result.plan);
                    if (item != null) {
                        item.setText(3, result.ok ? t("ddicStatusOk") : t("ddicStatusError") + ": " + result.message);
                        item.setForeground(result.ok ? null
                                : table.getDisplay().getSystemColor(SWT.COLOR_RED));
                    }
                    log.add((result.ok ? "OK   " : "HATA ") + result.plan.name + " -> " + result.message);
                }
            } catch (AdtException e) {
                log.add("HATA: " + e.getMessage());
            }
        });
        log.add(t("ddicDone"));
        logText.setText(String.join("\n", log));
        updateCreateButton();
    }
}
