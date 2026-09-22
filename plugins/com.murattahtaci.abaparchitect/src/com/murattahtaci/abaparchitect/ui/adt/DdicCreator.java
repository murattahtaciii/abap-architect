package com.murattahtaci.abaparchitect.ui.adt;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;

import com.murattahtaci.abaparchitect.core.NameUtil;
import com.murattahtaci.abaparchitect.ddic.DdicKind;
import com.murattahtaci.abaparchitect.ddic.DdicModel;
import com.murattahtaci.abaparchitect.ddic.DdicObjectPlan;
import com.murattahtaci.abaparchitect.ddic.DdicPlanOptions;
import com.murattahtaci.abaparchitect.ddic.DdicXmlBuilder;

public final class DdicCreator {

    private static final String ACCEPT_LOCK = "application/vnd.sap.as+xml;charset=UTF-8;"
            + "dataname=com.sap.adt.lock.result;q=0.8, application/vnd.sap.as+xml;charset=UTF-8;"
            + "dataname=com.sap.adt.lock.result2;q=0.9";
    private static final String CT_ACTIVATION = "application/vnd.sap.adt.activation+xml";
    private static final Pattern LOCK_HANDLE = Pattern.compile("<LOCK_HANDLE>([^<]+)</LOCK_HANDLE>");

    public interface Logger {

        void log(String line);
    }

    public static final class Result {

        public final DdicObjectPlan plan;
        public boolean ok;
        public String message = "";

        Result(DdicObjectPlan plan) {
            this.plan = plan;
        }
    }

    private DdicCreator() {
    }

    /** options.turkish'e gore cift dilli sabit metin. */
    private static String L(DdicPlanOptions options, String tr, String en) {
        return options != null && options.turkish ? tr : en;
    }

    public static List<Result> createAll(IProject project, DdicPlanOptions options,
            List<DdicObjectPlan> plans, Logger logger) throws AdtException {
        List<Result> results = new ArrayList<>();
        AdtRestClient client = new AdtRestClient(project);
        List<DdicObjectPlan> ordered = dependencyOrder(plans);
        List<Result> failed = new ArrayList<>();
        for (DdicObjectPlan plan : ordered) {
            Result result = runPlan(client, options, plan, logger);
            results.add(result);
            if (!result.ok) {
                failed.add(result);
            }
        }
        if (!failed.isEmpty()) {
            logger.log(L(options, "--- Tekrar deneme ---", "--- Retrying failed objects ---"));
            for (Result result : failed) {
                Result retry = runPlan(client, options, result.plan, logger);
                result.ok = retry.ok;
                result.message = retry.message;
            }
        }
        return results;
    }

    private static Result runPlan(AdtRestClient client, DdicPlanOptions options, DdicObjectPlan plan,
            Logger logger) {
        Result result = new Result(plan);
        String error = plan.validationError();
        if (error != null) {
            result.message = error;
            logger.log(L(options, "ATLANDI", "SKIPPED") + " " + plan.name + ": " + error);
            return result;
        }
        try {
            result.message = createOne(client, options, plan, logger);
            result.ok = true;
        } catch (AdtException e) {
            result.message = e.getMessage();
            logger.log(L(options, "HATA", "ERROR") + " " + plan.name + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * Bağımlılıklara göre sıralar: domain -> data element, include edilen
     * structure -> onu içeren structure/table, satır tipi -> table type.
     */
    static List<DdicObjectPlan> dependencyOrder(List<DdicObjectPlan> plans) {
        Map<String, DdicObjectPlan> byName = new LinkedHashMap<>();
        for (DdicObjectPlan plan : plans) {
            byName.putIfAbsent(plan.name.toUpperCase(Locale.ROOT), plan);
        }
        Map<DdicObjectPlan, List<DdicObjectPlan>> deps = new LinkedHashMap<>();
        for (DdicObjectPlan plan : plans) {
            List<DdicObjectPlan> list = new ArrayList<>();
            collectDependencies(plan, byName, list);
            deps.put(plan, list);
        }
        List<DdicObjectPlan> ordered = new ArrayList<>();
        boolean[] emitted = new boolean[plans.size()];
        for (int guard = 0; guard < plans.size(); guard++) {
            boolean progress = false;
            for (int i = 0; i < plans.size(); i++) {
                if (emitted[i]) {
                    continue;
                }
                DdicObjectPlan plan = plans.get(i);
                boolean ready = true;
                for (DdicObjectPlan dep : deps.get(plan)) {
                    if (!containsEmitted(plans, emitted, dep)) {
                        ready = false;
                        break;
                    }
                }
                if (ready) {
                    emitted[i] = true;
                    ordered.add(plan);
                    progress = true;
                }
            }
            if (!progress) {
                // döngüsel bağımlılık: kalanlar sırayla eklenir
                for (int i = 0; i < plans.size(); i++) {
                    if (!emitted[i]) {
                        ordered.add(plans.get(i));
                    }
                }
                break;
            }
        }
        return ordered;
    }

    private static boolean containsEmitted(List<DdicObjectPlan> plans, boolean[] emitted,
            DdicObjectPlan target) {
        for (int i = 0; i < plans.size(); i++) {
            if (emitted[i] && plans.get(i) == target) {
                return true;
            }
        }
        return false;
    }

    private static void collectDependencies(DdicObjectPlan plan, Map<String, DdicObjectPlan> byName,
            List<DdicObjectPlan> out) {
        addDependency(plan.domainName, byName, out);
        if (plan.kind == DdicKind.TABLE_TYPE && plan.rowType != null && !plan.builtinRowType) {
            addDependency(plan.rowType, byName, out);
        }
        if (plan.fields != null) {
            for (DdicModel.Field field : plan.fields) {
                if (field.includeStructure != null) {
                    addDependency(field.includeStructure, byName, out);
                }
                if (field.dataElementName != null) {
                    addDependency(field.dataElementName, byName, out);
                }
                if (field.tableTypeField && field.tableTypeName != null) {
                    addDependency(field.tableTypeName, byName, out);
                }
            }
        }
    }

    private static void addDependency(String name, Map<String, DdicObjectPlan> byName,
            List<DdicObjectPlan> out) {
        if (name == null || name.isEmpty()) {
            return;
        }
        DdicObjectPlan dep = byName.get(name.toUpperCase(Locale.ROOT));
        if (dep != null && !out.contains(dep)) {
            out.add(dep);
        }
    }

    private static String createOne(AdtRestClient client, DdicPlanOptions options, DdicObjectPlan plan,
            Logger logger) throws AdtException {
        DdicKind kind = plan.kind;
        String base = "/sap/bc/adt/ddic/" + kind.uriSegment;
        if (kind == DdicKind.TABLE && plan.fields != null) {
            for (DdicModel.Field field : plan.fields) {
                if (field.tableTypeField && field.tableTypeName != null) {
                    logger.log(L(options, "Bilgi: " + plan.name + " içinde '" + field.abapName
                            + "' tablo tipi (" + field.tableTypeName
                            + ") DDIC tablosunda bileşen olamaz; atlandı (ayrı TT nesnesi olarak oluşturuldu).",
                            "Info: '" + field.abapName + "' in " + plan.name + " is a table type ("
                                    + field.tableTypeName
                                    + ") and cannot be a database table component; skipped (created as separate TT object)."));
                }
            }
        }
        String createUrl = base + (options.transport == null || options.transport.isEmpty()
                ? ""
                : "?corrNr=" + encode(options.transport));
        String createBody = switch (kind) {
            case STRUCTURE -> DdicXmlBuilder.structureCreate(plan.name, plan.description, options.packageName,
                    options.language);
            case TABLE -> DdicXmlBuilder.tableCreate(plan.name, plan.description, options.packageName,
                    options.language);
            case TABLE_TYPE -> DdicXmlBuilder.tableTypeCreate(plan.name, plan.description, options.packageName,
                    options.language);
            case DOMAIN -> DdicXmlBuilder.domainCreate(plan.name, plan.description, options.packageName,
                    options.language);
            case DATA_ELEMENT -> DdicXmlBuilder.dataElementCreate(plan.name, plan.description, options.packageName,
                    options.language);
        };

        AdtRestClient.Response response = client.post(createUrl, kind.contentType, kind.contentType, createBody);
        boolean existed = false;
        if (!response.ok()) {
            if (isAlreadyExists(response.body)) {
                existed = true;
                logger.log(L(options, "Bilgi:", "Info:") + " " + plan.name + " "
                        + L(options, "zaten var, güncelleme denenecek.",
                                "already exists, update will be attempted."));
            } else {
                throw new AdtException(L(options, "Oluşturma başarısız", "Creation failed") + " (" + plan.name + "): " + response.summary(4000));
            }
        }

        String lower = plan.name.toLowerCase(Locale.ROOT);
        String lockHandle = lock(client, base, lower);
        if (lockHandle == null) {
            throw new AdtException(L(options, "Kilit alınamadı", "Lock failed") + " (" + plan.name + "). "
                    + L(options, "Nesne başka bir kullanıcıda kilitli olabilir.",
                            "The object may be locked by another user."));
        }
        try {
            String corrSuffix = options.transport == null || options.transport.isEmpty()
                    ? ""
                    : "&corrNr=" + encode(options.transport);
            if (kind.ddlSource) {
                checkSource(client, options, kind, plan, lower, logger);
                String urlBase = base + "/" + encode(lower) + "/source/main?lockHandle=" + encode(lockHandle);
                AdtRestClient.Response put = putWithTransportFallback(client, urlBase,
                        "text/plain; charset=utf-8", "text/plain", plan.ddlSource, corrSuffix, options,
                        logger, plan.name);
                if (!put.ok()) {
                    checkSource(client, options, kind, plan, lower, logger);
                    throw new AdtException(L(options, "Kaynak yüklenemedi", "Source upload failed") + " (" + plan.name + "): " + put.summary(4000));
                }
            } else {
                String urlBase = base + "/" + encode(lower) + "?lockHandle=" + encode(lockHandle);
                AdtRestClient.Response put = putWithTransportFallback(client, urlBase,
                        kind.contentType + "; charset=utf-8", kind.contentType, plan.xmlSource, corrSuffix,
                        options, logger, plan.name);
                if (!put.ok()) {
                    throw new AdtException(L(options, "Tanım kaydedilemedi", "Definition save failed") + " (" + plan.name + "): " + put.summary(4000));
                }
            }
        } finally {
            unlock(client, base, lower, lockHandle);
        }

        String message = activate(client, options, kind, plan);
        return (existed ? L(options, "Güncellendi ve aktive edildi.", "Updated and activated.") : L(options, "Oluşturuldu ve aktive edildi.", "Created and activated.")) + " " + message;
    }

    /**
     * PUT sırasında 409 (CTS kilit çakışması) oluşursa nesneyi mevcut transport
     * kaydıyla kaydetmeyi dener: önce corrNr olmadan, olmazsa hata mesajındaki
     * transport numarasıyla. Böylece daha önce başka bir talebe kaydedilmiş
     * nesneler güncellenebilir.
     */
    private static AdtRestClient.Response putWithTransportFallback(AdtRestClient client, String urlBase,
            String contentType, String accept, String body, String corrSuffix, DdicPlanOptions options,
            Logger logger, String planName) throws AdtException {
        AdtRestClient.Response response = client.put(urlBase + corrSuffix, contentType, accept, body);
        if (response.status != 409) {
            return response;
        }
        logger.log(L(options, "Bilgi:", "Info:") + " " + planName
                + L(options, " başka bir transportta kayıtlı; mevcut kayıtla denenecek.",
                        " is recorded in another transport; retrying with existing record."));
        response = client.put(urlBase, contentType, accept, body);
        if (response.status != 409) {
            return response;
        }
        String existing = existingTransport(response.body);
        if (existing != null && !existing.isEmpty()) {
            response = client.put(urlBase + "&corrNr=" + encode(existing), contentType, accept, body);
        }
        return response;
    }

    /** 409 yanıtındaki T100KEY-V3 alanından çakışan transport numarasını alır. */
    private static String existingTransport(String body) {
        if (body == null) {
            return null;
        }
        Matcher m = Pattern.compile("<entry key=\"T100KEY-V3\">([^<]+)</entry>").matcher(body);
        return m.find() ? m.group(1).trim() : null;
    }
    private static void checkSource(AdtRestClient client, DdicPlanOptions options, DdicKind kind,
            DdicObjectPlan plan, String lower, Logger logger) {
        try {
            String uri = DdicXmlBuilder.objectUri(kind, plan.name);
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    .append("<chkrun:checkObjectList xmlns:chkrun=\"http://www.sap.com/adt/checkrun\"")
                    .append(" xmlns:adtcore=\"http://www.sap.com/adt/core\">")
                    .append("<chkrun:checkObject adtcore:uri=\"").append(uriEscape(uri)).append("\">")
                    .append("<chkrun:artifacts>")
                    .append("<chkrun:artifact chkrun:uri=\"").append(uriEscape(uri + "/source/main")).append("\"")
                    .append(" chkrun:contentType=\"text/plain; charset=utf-8\">")
                    .append("<chkrun:content>")
                    .append(java.util.Base64.getEncoder().encodeToString(
                            plan.ddlSource == null ? new byte[0] : plan.ddlSource.getBytes(StandardCharsets.UTF_8)))
                    .append("</chkrun:content>")
                    .append("</chkrun:artifact>")
                    .append("</chkrun:artifacts>")
                    .append("</chkrun:checkObject>")
                    .append("</chkrun:checkObjectList>");
            AdtRestClient.Response resp = client.post("/sap/bc/adt/checkruns",
                    "application/vnd.sap.adt.checkobjects+xml", "application/vnd.sap.adt.checkmessages+xml",
                    sb.toString());
            logger.log("CHECK " + plan.name + " [" + kind.adtType + "]: HTTP " + resp.status
                    + " | " + summarizeCheck(resp.body));
        } catch (Exception e) {
            logger.log(L(options, "CHECK " + plan.name + ": çağrılamadı -> ", "CHECK " + plan.name + ": failed -> ") + e);
        }
    }

    private static String uriEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String lock(AdtRestClient client, String base, String lower) throws AdtException {
        String url = base + "/" + encode(lower) + "?_action=LOCK&accessMode=MODIFY";
        AdtRestClient.Response response = client.post(url, null, ACCEPT_LOCK, null);
        if (!response.ok()) {
            return null;
        }
        Matcher matcher = LOCK_HANDLE.matcher(response.body == null ? "" : response.body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static void unlock(AdtRestClient client, String base, String lower, String lockHandle) {
        try {
            String url = base + "/" + encode(lower) + "?_action=UNLOCK&lockHandle=" + encode(lockHandle);
            client.post(url, null, null, null);
        } catch (AdtException ignored) {
            // kilit zaten düşmüş olabilir
        }
    }

    private static String activate(AdtRestClient client, DdicPlanOptions options, DdicKind kind,
            DdicObjectPlan plan) throws AdtException {
        String uri = DdicXmlBuilder.objectUri(kind, plan.name);
        AdtRestClient.Response response = client.post("/sap/bc/adt/activation?method=activate&preauditRequested=true",
                CT_ACTIVATION, "application/xml", DdicXmlBuilder.activation(uri, plan.name));
        if (!response.ok()) {
            throw new AdtException(L(options, "Aktivasyon başarısız", "Activation failed") + " (" + plan.name + "): " + response.summary(4000));
        }
        String body = response.body == null ? "" : response.body;
        String errors = activationErrors(body);
        if (errors != null) {
            throw new AdtException(L(options, "Aktivasyon hatalı", "Activation has errors") + " ("
                    + plan.name + "): " + errors);
        }
        return "";
    }

    /**
     * Aktivasyon yanıtındaki hata (severity E) mesajlarını çıkarır; hata yoksa null.
     * E seviyeli sonuç aktivasyonun başarısız olduğu anlamına gelir.
     */
    private static String activationErrors(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        boolean hasError = body.contains("severity=\"E\"") || body.contains("severity='E'")
                || body.contains("SEVERITY=\"E\"") || body.contains("<SEVERITY>E")
                || body.contains("type=\"E\"") || body.contains(">E<");
        if (!hasError) {
            return null;
        }
        return summarizeCheck(body);
    }

    private static boolean isAlreadyExists(String body) {
        if (body == null) {
            return false;
        }
        String lower = body.toLowerCase(Locale.ROOT);
        return lower.contains("already exist") || lower.contains("zaten var")
                || lower.contains("adt_object_already_exists");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** checkmessages yanıtındaki mesajları kısa metin olarak özetler. */
    private static String summarizeCheck(String body) {
        if (body == null || body.isEmpty()) {
            return "yanıt boş";
        }
        Matcher msg = Pattern.compile("<chkrun:checkMessage\\b[^>]*/?>", Pattern.DOTALL).matcher(body);
        StringBuilder out = new StringBuilder();
        while (msg.find()) {
            String chunk = msg.group();
            String type = attr(chunk, "chkrun:type");
            String code = attr(chunk, "chkrun:code");
            String text = attr(chunk, "chkrun:shortText", "chkrun:description");
            if (text.isEmpty()) {
                text = firstElement(chunk, "chkrun:shortText", "chkrun:description");
            }
            String loc = attr(chunk, "chkrun:uri", "adtcore:uri");
            out.append("[").append(type.isEmpty() ? "?" : type).append("]");
            if (!code.isEmpty()) {
                out.append("(").append(code).append(")");
            }
            if (!loc.isEmpty()) {
                out.append(" @").append(loc);
            }
            out.append(" ").append(text);
            if (text.isEmpty()) {
                out.append(chunk.replaceAll("\\s+", " ").replaceAll("<[^>]+>", " ").trim());
            }
            out.append(" || ");
        }
        if (out.length() == 0) {
            return body.length() > 1200 ? body.substring(0, 1200) : body;
        }
        return out.toString();
    }

    private static String attr(String xml, String... names) {
        for (String name : names) {
            Matcher m = Pattern.compile("(?:\\b" + name + ")=\"([^\"]*)\"").matcher(xml);
            if (m.find()) {
                return m.group(1);
            }
        }
        return "";
    }

    private static String firstElement(String xml, String... names) {
        for (String name : names) {
            Matcher m = Pattern.compile("<" + name + "\\b[^>]*>(.*?)</" + name + ">", Pattern.DOTALL)
                    .matcher(xml);
            if (m.find()) {
                return m.group(1);
            }
        }
        return "";
    }

    public static String safeName(String name) {
        return NameUtil.stripTrailingUnderscores(name);
    }
}
