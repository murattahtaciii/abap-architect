package io.github.murattahtaciii.abaparchitect.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

import io.github.murattahtaciii.abaparchitect.ddic.AbapTypeMapping;
import io.github.murattahtaciii.abaparchitect.ddic.DdicKind;
import io.github.murattahtaciii.abaparchitect.ddic.DdicModel;
import io.github.murattahtaciii.abaparchitect.ddic.DdicObjectPlan;
import io.github.murattahtaciii.abaparchitect.ddic.DdicPlanOptions;
import io.github.murattahtaciii.abaparchitect.ddic.DdicPlanner;
import io.github.murattahtaciii.abaparchitect.ddic.DdicXmlBuilder;

public class DdicFieldsDialog extends TitleAreaDialog {

    private final DdicObjectPlan plan;
    private final List<DdicObjectPlan> allPlans;
    private final DdicPlanOptions options;
    private final boolean turkish;
    private final List<DdicModel.Field> fields = new ArrayList<>();

    private Table table;
    private Combo rowTypeCombo;
    private Combo domainCombo;
    private Combo kindCombo;
    private Text lengthText;
    private Text decimalsText;

    public DdicFieldsDialog(Shell parentShell, DdicObjectPlan plan, List<DdicObjectPlan> allPlans,
            DdicPlanOptions options, boolean turkish) {
        super(parentShell);
        this.plan = plan;
        this.allPlans = allPlans;
        this.options = options;
        this.turkish = turkish;
        if (plan.fields != null) {
            for (DdicModel.Field field : plan.fields) {
                fields.add(copy(field));
            }
        }
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    private String t(String key) {
        return Messages.get(turkish, key);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        setTitle(plan.name + " — " + t("editTitle"));
        setMessage(t("editMessage"));

        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(1, false));

        switch (plan.kind) {
            case STRUCTURE:
            case TABLE:
                createFieldsTable(container);
                break;
            case TABLE_TYPE:
                createRowTypeGroup(container);
                break;
            case DATA_ELEMENT:
                createDomainGroup(container);
                break;
            case DOMAIN:
                createDomainTypeGroup(container);
                break;
            default:
                break;
        }
        return area;
    }

    private void createFieldsTable(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        group.setLayout(new GridLayout(1, false));

        Label hint = new Label(group, SWT.NONE);
        hint.setText(t("editHint"));
        hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        table = new Table(group, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        String[] columns = { t("fldName"), t("fldType"), t("fldLength"), t("fldDecimals"), t("fldDescription"),
                t("fldKey") };
        int[] widths = { 170, 150, 70, 70, 220, 50 };
        for (int i = 0; i < columns.length; i++) {
            TableColumn column = new TableColumn(table, SWT.LEFT);
            column.setText(columns[i]);
            column.setWidth(widths[i]);
        }
        table.addListener(SWT.MouseDown, event -> {
            TableItem item = table.getItem(new org.eclipse.swt.graphics.Point(event.x, event.y));
            if (item == null) {
                return;
            }
            int column = columnAt(event.x);
            if (column == 5) {
                toggleKey(table.indexOf(item), item);
            } else if (column >= 0 && column <= 4) {
                openEditor(table.indexOf(item), item, column);
            }
        });

        Composite buttons = new Composite(group, SWT.NONE);
        buttons.setLayout(new GridLayout(2, false));
        Button add = new Button(buttons, SWT.PUSH);
        add.setText(t("fldAdd"));
        add.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                DdicModel.Field field = new DdicModel.Field();
                field.abapName = "NEW_FIELD";
                field.description = "";
                AbapTypeMapping.TypeInfo info = AbapTypeMapping.apply(AbapTypeMapping.defaultTextChoice(), 255, 0);
                applyType(field, info);
                fields.add(field);
                fillRows();
            }
        });
        Button remove = new Button(buttons, SWT.PUSH);
        remove.setText(t("fldRemove"));
        remove.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                int index = table.getSelectionIndex();
                if (index >= 0 && index < fields.size()) {
                    fields.remove(index);
                    fillRows();
                }
            }
        });
        fillRows();
    }

    private void createRowTypeGroup(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        new Label(group, SWT.NONE).setText(t("editRowType"));
        rowTypeCombo = new Combo(group, SWT.READ_ONLY);
        rowTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (DdicObjectPlan other : allPlans) {
            if (other.kind == DdicKind.STRUCTURE) {
                rowTypeCombo.add(other.name);
            }
        }
        for (String builtin : DdicXmlBuilder.BUILTIN_ROW_TYPES) {
            rowTypeCombo.add(builtin);
        }
        String current = plan.rowType == null ? "" : plan.rowType.toUpperCase(Locale.ROOT);
        int index = -1;
        for (int i = 0; i < rowTypeCombo.getItemCount(); i++) {
            if (rowTypeCombo.getItem(i).equals(current)) {
                index = i;
                break;
            }
        }
        rowTypeCombo.select(index < 0 ? 0 : index);
    }

    private void createDomainGroup(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        new Label(group, SWT.NONE).setText(t("editDomain"));
        domainCombo = new Combo(group, SWT.READ_ONLY);
        domainCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (DdicObjectPlan other : allPlans) {
            if (other.kind == DdicKind.DOMAIN) {
                domainCombo.add(other.name);
            }
        }
        String current = plan.domainName == null ? "" : plan.domainName.toUpperCase(Locale.ROOT);
        int index = -1;
        for (int i = 0; i < domainCombo.getItemCount(); i++) {
            if (domainCombo.getItem(i).equals(current)) {
                index = i;
                break;
            }
        }
        domainCombo.select(index < 0 && domainCombo.getItemCount() > 0 ? 0 : Math.max(index, 0));
    }

    private void createDomainTypeGroup(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(4, false));
        new Label(group, SWT.NONE).setText(t("fldType"));
        kindCombo = new Combo(group, SWT.READ_ONLY);
        List<AbapTypeMapping.Choice> domainChoices = domainChoices();
        for (AbapTypeMapping.Choice choice : domainChoices) {
            kindCombo.add(choice.label);
        }
        kindCombo.select(initialDomainChoiceIndex(domainChoices));
        kindCombo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                applyDomainChoice(domainChoices);
            }
        });

        new Label(group, SWT.NONE).setText(t("fldLength"));
        lengthText = new Text(group, SWT.BORDER);
        lengthText.setText(String.valueOf(plan.length == 0 ? 255 : plan.length));
        lengthText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        digitsOnly(lengthText);

        new Label(group, SWT.NONE).setText(t("fldDecimals"));
        decimalsText = new Text(group, SWT.BORDER);
        decimalsText.setText(String.valueOf(plan.decimals));
        decimalsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        digitsOnly(decimalsText);

        applyDomainChoice(domainChoices);
    }

    private List<AbapTypeMapping.Choice> domainChoices() {
        List<String> domainTypes = List.of("CHAR", "NUMC", "INT4", "INT8", "DEC", "CURR", "QUAN", "DATS",
                "TIMS", "CLNT");
        List<AbapTypeMapping.Choice> result = new ArrayList<>();
        for (AbapTypeMapping.Choice choice : AbapTypeMapping.choices()) {
            if (domainTypes.contains(choice.domainType)) {
                result.add(choice);
            }
        }
        return result;
    }

    private int initialDomainChoiceIndex(List<AbapTypeMapping.Choice> domainChoices) {
        for (int i = 0; i < domainChoices.size(); i++) {
            AbapTypeMapping.Choice choice = domainChoices.get(i);
            if (choice.domainType.equalsIgnoreCase(plan.domainType) && choice.defaultLength == plan.length
                    && choice.defaultDecimals == plan.decimals) {
                return i;
            }
        }
        for (int i = 0; i < domainChoices.size(); i++) {
            if (domainChoices.get(i).domainType.equalsIgnoreCase(plan.domainType)) {
                return i;
            }
        }
        return 0;
    }

    /** Seçilen tipe göre uzunluk/ondalık alanlarını otomatik doldurur ve kilitler. */
    private void applyDomainChoice(List<AbapTypeMapping.Choice> domainChoices) {
        int index = kindCombo.getSelectionIndex();
        if (index < 0 || index >= domainChoices.size()) {
            return;
        }
        AbapTypeMapping.Choice choice = domainChoices.get(index);
        lengthText.setText(String.valueOf(choice.defaultLength));
        decimalsText.setText(String.valueOf(choice.defaultDecimals));
        lengthText.setEnabled(choice.lengthEditable);
        decimalsText.setEnabled(choice.decimalsEditable);
    }

    /** Yalnızca rakam girişine izin verir. */
    private static void digitsOnly(Text text) {
        text.addVerifyListener(event -> {
            if (event.text.isEmpty()) {
                return;
            }
            String current = text.getText();
            String candidate = current.substring(0, event.start) + event.text
                    + current.substring(event.end);
            event.doit = candidate.matches("\\d{0,9}");
        });
    }

    private void fillRows() {
        if (table == null || table.isDisposed()) {
            return;
        }
        table.removeAll();
        for (DdicModel.Field field : fields) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, field.abapName == null ? "" : field.abapName);
            AbapTypeMapping.Choice choice = AbapTypeMapping.choiceFor(field.ddlType, field.length,
                    field.decimals);
            item.setText(1, choice.label);
            item.setText(2, String.valueOf(field.length));
            item.setText(3, String.valueOf(field.decimals));
            item.setText(4, field.description == null ? "" : field.description);
            item.setText(5, field.key ? "X" : "");
        }
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
        if (index < 0 || index >= fields.size()) {
            return;
        }
        DdicModel.Field field = fields.get(index);
        AbapTypeMapping.Choice choice = AbapTypeMapping.choiceFor(field.ddlType, field.length, field.decimals);
        switch (column) {
            case 0:
                editText(item, column, field.abapName, value -> {
                    String name = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
                    int max = plan.kind == DdicKind.TABLE ? 16 : 30;
                    field.abapName = name.length() > max ? name.substring(0, max) : name;
                    fillRows();
                }, maxNameLength());
                break;
            case 1:
                editCombo(item, column, AbapTypeMapping.choices(), field, index);
                break;
            case 2:
                if (!choice.lengthEditable) {
                    return;
                }
                editText(item, column, String.valueOf(field.length), value -> {
                    field.length = parseInt(value, field.length);
                    applyType(field, AbapTypeMapping.apply(
                            AbapTypeMapping.choiceFor(field.ddlType, field.length, field.decimals),
                            field.length, field.decimals));
                    fillRows();
                }, 9);
                break;
            case 3:
                if (!choice.decimalsEditable) {
                    return;
                }
                editText(item, column, String.valueOf(field.decimals), value -> {
                    field.decimals = parseInt(value, field.decimals);
                    applyType(field, AbapTypeMapping.apply(
                            AbapTypeMapping.choiceFor(field.ddlType, field.length, field.decimals),
                            field.length, field.decimals));
                    fillRows();
                }, 9);
                break;
            case 4:
                editText(item, column, field.description, value -> {
                    field.description = value;
                    fillRows();
                }, 55);
                break;
            default:
                break;
        }
    }

    private int maxNameLength() {
        return plan.kind == DdicKind.TABLE ? 16 : 30;
    }

    private void editCombo(TableItem item, int column, List<AbapTypeMapping.Choice> choices,
            DdicModel.Field field, int index) {
        TableEditor editor = new TableEditor(table);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        editor.minimumWidth = 120;
        Combo combo = new Combo(table, SWT.READ_ONLY);
        for (AbapTypeMapping.Choice choice : choices) {
            combo.add(choice.label);
        }
        Runnable commit = () -> {
            if (combo.isDisposed()) {
                return;
            }
            int choiceIndex = combo.getSelectionIndex();
            combo.dispose();
            if (choiceIndex >= 0) {
                AbapTypeMapping.Choice choice = choices.get(choiceIndex);
                applyType(field, AbapTypeMapping.apply(choice, choice.defaultLength, choice.defaultDecimals));
                field.dataElementName = null;
            }
            fillRows();
        };
        combo.addFocusListener(FocusListener.focusLostAdapter(event -> commit.run()));
        combo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                commit.run();
            }
        });
        AbapTypeMapping.Choice current = AbapTypeMapping.choiceFor(field.ddlType, field.length,
                field.decimals);
        int selected = choices.indexOf(current);
        editor.setEditor(combo, item, column);
        combo.select(selected < 0 ? 0 : selected);
        combo.setFocus();
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
                    setMessage(t("limitWarn") + " (" + maxLength + ")", IMessageProvider.WARNING);
                    getShell().getDisplay().beep();
                }
            });
        }
        Runnable commit = () -> {
            if (text.isDisposed()) {
                return;
            }
            String value = text.getText().trim();
            text.dispose();
            setMessage(t("editMessage"));
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

    private void toggleKey(int index, TableItem item) {
        if (index < 0 || index >= fields.size() || plan.kind != DdicKind.TABLE) {
            return;
        }
        DdicModel.Field field = fields.get(index);
        field.key = !field.key;
        item.setText(5, field.key ? "X" : "");
    }

    private static void applyType(DdicModel.Field field, AbapTypeMapping.TypeInfo info) {
        field.ddlType = info.ddlType;
        field.domainType = info.domainType;
        field.length = info.length;
        field.decimals = info.decimals;
        field.typeCode = info.code;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static DdicModel.Field copy(DdicModel.Field source) {
        DdicModel.Field copy = new DdicModel.Field();
        copy.abapName = source.abapName;
        copy.description = source.description;
        copy.ddlType = source.ddlType;
        copy.domainType = source.domainType;
        copy.length = source.length;
        copy.decimals = source.decimals;
        copy.typeCode = source.typeCode;
        copy.key = source.key;
        copy.tableTypeField = source.tableTypeField;
        copy.structEntity = source.structEntity;
        copy.includeStructure = source.includeStructure;
        copy.tableTypeName = source.tableTypeName;
        copy.dataElementName = source.dataElementName;
        copy.note = source.note;
        return copy;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, 0, t("editOk"), true);
        createButton(parent, 1, t("editCancel"), false);
        getButton(1).addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                cancelPressed();
            }
        });
    }

    @Override
    protected void okPressed() {
        if (plan.kind == DdicKind.STRUCTURE || plan.kind == DdicKind.TABLE) {
            plan.fields = new ArrayList<>(fields);
        } else if (plan.kind == DdicKind.TABLE_TYPE && rowTypeCombo != null) {
            int index = rowTypeCombo.getSelectionIndex();
            if (index >= 0) {
                String rowType = rowTypeCombo.getItem(index);
                plan.rowType = rowType;
                plan.builtinRowType = DdicXmlBuilder.BUILTIN_ROW_TYPES.contains(rowType.toUpperCase(Locale.ROOT));
            }
        } else if (plan.kind == DdicKind.DATA_ELEMENT && domainCombo != null) {
            int index = domainCombo.getSelectionIndex();
            if (index >= 0) {
                plan.domainName = domainCombo.getItem(index);
            }
        } else if (plan.kind == DdicKind.DOMAIN && kindCombo != null) {
            int index = kindCombo.getSelectionIndex();
            List<AbapTypeMapping.Choice> domainChoices = domainChoices();
            if (index >= 0 && index < domainChoices.size()) {
                AbapTypeMapping.Choice choice = domainChoices.get(index);
                plan.domainType = choice.domainType;
                plan.length = parseInt(lengthText.getText(), choice.defaultLength);
                plan.decimals = parseInt(decimalsText.getText(), choice.defaultDecimals);
                AbapTypeMapping.TypeInfo info = AbapTypeMapping.apply(choice, plan.length, plan.decimals);
                plan.length = info.length;
                plan.decimals = info.decimals;
            }
        }
        DdicPlanner.refreshSource(plan, options);
        super.okPressed();
    }
}
