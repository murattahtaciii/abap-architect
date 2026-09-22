package io.github.murattahtaciii.abaparchitect.ui;

import java.util.LinkedHashMap;
import java.util.Map;

final class Messages {

    private static final Map<String, String[]> TEXT = new LinkedHashMap<>();

    static {
        put("viewTitle", "ABAP Architect", "ABAP Architect");
        put("format", "Biçim", "Format");
        put("root", "Root", "Root");
        put("str", "Str", "Str");
        put("tab", "Tab", "Tab");
        put("suffix", "Suffix", "Suffix");
        put("snake", "Snake_Case", "Snake_Case");
        put("attrFlat", "Attr: Düz", "Attr: Flat");
        put("lang", "Dil", "Language");
        put("inputLabel", "Girdi (JSON/XML)", "Input (JSON/XML)");
        put("btnConvert", "Dönüştür", "Convert");
        put("btnFormat", "Formatla", "Format");
        put("btnValidate", "Doğrula", "Validate");
        put("btnCopy", "Kopyala", "Copy");
        put("btnInsert", "Editöre Ekle", "Insert to Editor");
        put("btnSave", "Kaydet", "Save As");
        put("btnSample", "Örnek", "Sample");
        put("tabTypes", "ABAP Types", "ABAP Types");
        put("tabMapping", "Mapping", "Mapping");
        put("tabIxml", "iXML Kodu", "iXML Code");
        put("tabSt", "Basit Dönüşüm", "Simple Trans.");
        put("tabTemplate", "Şablon", "Template");
        put("tabValidation", "Doğrulama", "Validation");
        put("statusReady", "Hazır", "Ready");
        put("statusEmpty", "Editör boş", "Editor empty");
        put("statusValidJson", "JSON geçerli", "JSON valid");
        put("statusInvalidJson", "JSON geçersiz", "JSON invalid");
        put("statusValidXml", "XML geçerli", "XML valid");
        put("statusInvalidXml", "XML geçersiz", "XML invalid");
        put("lines", "satır", "lines");
        put("tipConvert", "Dönüştür (kod üret)", "Convert (generate code)");
        put("tipFormat", "Girdiyi biçimlendir", "Format input");
        put("tipValidate", "Girdiyi doğrula", "Validate input");
        put("tipCopy", "Aktif sekmeyi panoya kopyala", "Copy active tab to clipboard");
        put("tipInsert", "Aktif editörde imleç konumuna ekle", "Insert at cursor in active editor");
        put("tipSave", "Aktif sekmeyi dosyaya kaydet", "Save active tab to file");
        put("tipSample", "Örnek veri yükle", "Load sample data");
        put("msgCopied", "Panoya kopyalandı.", "Copied to clipboard.");
        put("msgCopyFailed", "Panoya kopyalanamadı.", "Could not copy to clipboard.");
        put("msgInserted", "Kod aktif editöre eklendi.", "Code inserted into active editor.");
        put("msgInsertFailed", "Ekleme başarısız", "Insert failed");
        put("msgUnsaved", "Kaydedilecek içerik yok.", "Nothing to save.");
        put("msgSaved", "Dosya kaydedildi", "File saved");
        put("msgSaveFailed", "Dosya kaydedilemedi", "Could not save file");
        put("msgFormatOk", "Biçimlendirildi.", "Formatted.");
        put("msgFormatFailed", "Biçimlendirilemedi", "Could not format");
        put("msgNoContent", "Bu sekme için içerik yok.", "No content for this tab.");
        put("msgEmptyInput", "Önce JSON veya XML girin.", "Enter JSON or XML first.");
        put("validationOk", "Girdi geçerli ve iyi biçimlendirilmiş.", "Input is valid and well-formed.");
        put("validationErrors", "Doğrulama hataları:", "Validation errors:");
        put("infoTypes", "tip", "types");
        put("infoMapping", "mapping", "mapping");
        put("langTr", "TR", "TR");
        put("langEn", "EN", "EN");
        put("suffixDisabled", "→ ABAP", "→ ABAP");
        put("suffixJson", "→ JSON", "→ JSON");
        put("btnDdic", "DDIC Oluştur", "Create DDIC");
        put("tipDdic", "Üretilen tipler için DDIC nesneleri oluştur (structure, table type, tablo, data element, domain)",
                "Create DDIC objects for the generated types (structure, table type, table, data element, domain)");
        put("tabDdic", "DDIC", "DDIC");
        put("ddicPreviewTitle", "DDIC Oluşturma Planı", "DDIC Creation Plan");
        put("ddicPreviewEmpty", "DDIC planı için JSON/XML girip Dönüştür'e basın",
                "Enter JSON/XML and press Convert to build the DDIC plan");
        put("ddicTitle", "DDIC Nesneleri Oluştur", "Create DDIC Objects");
        put("ddicMessage", "Seçilen nesneler bağlı SAP sisteminde oluşturulup aktive edilecek.",
                "The selected objects will be created and activated in the connected SAP system.");
        put("ddicTarget", "Hedef", "Target");
        put("ddicProject", "ADT Projesi", "ADT Project");
        put("ddicPackage", "Paket", "Package");
        put("ddicTransport", "Transport", "Transport");
        put("ddicNaming", "İsimlendirme", "Naming");
        put("ddicPrefix", "Ön ek", "Prefix");
        put("ddicStructSuffix", "Structure soneki", "Structure suffix");
        put("ddicTtSuffix", "Table type soneki", "Table type suffix");
        put("ddicTableSuffix", "Tablo soneki", "Table suffix");
        put("ddicTableName", "Tablo adı", "Table name");
        put("ddicCreateTable", "Transparent tablo da oluştur", "Also create transparent table");
        put("ddicCreateElements", "Data element + domain de oluştur", "Also create data element + domain");
        put("ddicClientField", "Client alanı ekle", "Add client field");
        put("ddicObjects", "Nesneler", "Objects");
        put("ddicColKind", "Tip", "Kind");
        put("ddicColName", "İsim", "Name");
        put("ddicColDesc", "Açıklama", "Description");
        put("ddicColStatus", "Durum", "Status");
        put("ddicResuggest", "Yeniden Öner", "Re-suggest");
        put("ddicCreate", "Oluştur", "Create");
        put("ddicClose", "Kapat", "Close");
        put("ddicLog", "Günlük", "Log");
        put("ddicNoAdt", "ADT (ABAP Development Tools) bulunamadı. DDIC oluşturma devre dışı.",
                "ADT (ABAP Development Tools) not found. DDIC creation is disabled.");
        put("ddicNoProjects", "ADT projesi bulunamadı. Önce bir ABAP projesi oluşturun/bağlayın.",
                "No ADT project found. Create/connect an ABAP project first.");
        put("ddicDone", "Tamamlandı", "Done");
        put("ddicPlanned", "planlanan nesne", "planned objects");
        put("ddicRunning", "Oluşturuluyor...", "Creating...");
        put("ddicStatusOk", "Başarılı", "OK");
        put("ddicStatusError", "Hata", "Error");
        put("ddicStatusPending", "Bekliyor", "Pending");
        put("ddicStatusSkipped", "Atlandı", "Skipped");
        put("limitWarn", "Maksimum karakter sınırına ulaşıldı:", "Maximum characters exceeded:");
        put("ddicListPackages", "Paketleri listele", "List packages");
        put("ddicListTransports", "Transportları listele", "List transports");
        put("ddicPick", "Bir öğe seçin (yazarak filtreleyebilirsiniz):",
                "Select an item (type to filter):");
        put("ddicCatalogBusy", "Liste zaten yükleniyor, lütfen bekleyin...",
                "A list is already loading, please wait...");
        put("ddicNeedPackage", "Paket adı girin.", "Enter a package name.");
        put("ddicNeedTransport", "Paket $TMP değilse transport girin.", "Enter a transport for non-$TMP packages.");
        put("ddicModulePrefix", "Modül ön eki", "Module prefix");
        put("ddicCodeStructure", "Structure kodu", "Structure code");
        put("ddicCodeTableType", "Table type kodu", "Table type code");
        put("ddicCodeTable", "Tablo kodu", "Table code");
        put("ddicCodeElement", "Data element kodu", "Data element code");
        put("ddicCodeDomain", "Domain kodu", "Domain code");
        put("ddicEdit", "Düzenle...", "Edit...");
        put("editTitle", "Düzenle", "Edit");
        put("editMessage", "Nesne tipini, alan adlarını, tiplerini ve açıklamalarını düzenleyin.",
                "Edit the object type, field names, types and descriptions.");
        put("editOk", "Tamam", "OK");
        put("editCancel", "İptal", "Cancel");
        put("editRowType", "Satır tipi", "Row type");
        put("editDomain", "Domain", "Domain");
        put("fldName", "Alan", "Field");
        put("fldType", "ABAP Tipi", "ABAP Type");
        put("fldLength", "Uzunluk", "Length");
        put("fldDecimals", "Ondalık", "Decimals");
        put("fldDescription", "Açıklama", "Description");
        put("fldKey", "Kilit", "Key");
        put("fldAdd", "Alan Ekle", "Add Field");
        put("fldRemove", "Alan Sil", "Remove Field");
        put("editHint", "Düzenlemek için hücreye tıklayın; ABAP Tipi hücresinde dropdown açılır.",
                "Click a cell to edit; the ABAP Type cell opens a dropdown.");
        put("ddicHint", "İsim/tip/açıklama için satırdaki hücreye tıklayın; alanları düzenlemek için satırı seçip Düzenle...'ye basın.",
                "Click a cell to edit name/type/description; select a row and press Edit... to edit fields.");
        put("jsonHint", "İpucu: JSON'da anahtarlar ve metinler çift tırnak içinde olmalı; yorum satırı ve "
                + "sondaki virgül geçersizdir. Girdi UTF-8 olmalıdır.",
                "Hint: In JSON, keys and strings need double quotes; comments and trailing commas are invalid. "
                        + "Input must be UTF-8.");
        put("xmlHint", "İpucu: XML iyi biçimlendirilmiş olmalı; '&' karakteri '&amp;' olarak yazılmalı ve "
                + "tüm etiketler kapatılmalıdır.",
                "Hint: XML must be well-formed; escape '&' as '&amp;' and close all tags.");
    }

    private Messages() {
    }

    private static void put(String key, String tr, String en) {
        TEXT.put(key, new String[] { tr, en });
    }

    static String get(boolean turkish, String key) {
        String[] value = TEXT.get(key);
        if (value == null) {
            return key;
        }
        return turkish ? value[0] : value[1];
    }
}
