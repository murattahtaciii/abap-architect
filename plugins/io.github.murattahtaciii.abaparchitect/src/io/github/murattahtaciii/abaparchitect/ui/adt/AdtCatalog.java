package io.github.murattahtaciii.abaparchitect.ui.adt;

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

/**
 * ADT üzerinden kullanıcı transportlarının ve paketlerin listelenmesi.
 * Transport: GET /sap/bc/adt/cts/transportrequests?user=<USER>&targets=true
 *            (tm:modifiable bölümlerindeki tm:request girdileri)
 * Paket:     GET /sap/bc/adt/repository/informationsystem/search
 *            ?operation=quickSearch&query=<MASK>*&maxResults=100&objectType=DEVC
 *            (adtcore:objectReference girdileri)
 */
public final class AdtCatalog {

    public static final class Entry {

        public final String id;
        public final String description;

        public Entry(String id, String description) {
            this.id = id;
            this.description = description == null ? "" : description.trim();
        }

        public String label() {
            return description.isEmpty() ? id : id + "  —  " + description;
        }
    }

    private static final String TRANSPORT_REQUESTS = "/sap/bc/adt/cts/transportrequests";
    private static final String TRANSPORT_CONFIGS =
            "/sap/bc/adt/cts/transportrequests/searchconfiguration/configurations";
    private static final String PACKAGE_SEARCH =
            "/sap/bc/adt/repository/informationsystem/search";

    private AdtCatalog() {
    }

    /**
     * Kullanıcının değiştirilebilir transportlarını listeler. Sırayla denenir:
     * 0) resmi ADT servisi IAdtTransportService.findTransports(user, "K")
     * 1) /cts/transportrequests?user=X&status=D
     * 2) arama konfigürasyonu (configUri) akışı
     * 3) /cts/transportrequests?user=X&targets=true
     * Her denemenin sonucu teşhis metnine eklenir.
     */
    public static List<Entry> listTransports(IProject project) throws AdtException {
        AdtRestClient client = new AdtRestClient(project);
        String user = destinationUserOf(project);
        StringBuilder diag = new StringBuilder();

        List<Entry> result = tryTransportService(project, user, diag);
        if (!result.isEmpty()) {
            return result;
        }
        result = trySearch(client,
                TRANSPORT_REQUESTS + "?user=" + encode(user) + "&status=D", diag);
        if (!result.isEmpty()) {
            return result;
        }
        result = tryConfigSearch(client, user, diag);
        if (!result.isEmpty()) {
            return result;
        }
        result = trySearch(client,
                TRANSPORT_REQUESTS + "?user=" + encode(user) + "&targets=true", diag);
        if (!result.isEmpty()) {
            return result;
        }
        throw new AdtException("Transport listesi alınamadı (kullanıcı: "
                + (user == null ? "?" : user) + ").\n" + diag);
    }

    /**
     * Resmi ADT transport servisi: AdtTransportServiceFactory.createTransportService(destinationId)
     * üzerinden IAdtTransportService.findTransports(user, "K") çağrılır.
     */
    private static List<Entry> tryTransportService(IProject project, String user, StringBuilder diag) {
        List<Entry> result = new ArrayList<>();
        String factoryClass = "com.sap.adt.transport.AdtTransportServiceFactory";
        if (!AdtReflect.exists(factoryClass) || user == null || user.isEmpty()) {
            diag.append("resmi transport servisi: atlandı\n");
            return result;
        }
        try {
            String destinationId = destinationIdOf(project);
            Object service = AdtReflect.callStatic(factoryClass, "createTransportService",
                    new Class<?>[] { String.class }, destinationId);
            Object list = AdtReflect.call(service, "findTransports",
                    new Class<?>[] { String.class, String.class }, user, "K");
            if (list instanceof java.util.Collection) {
                for (Object item : (java.util.Collection<?>) list) {
                    String number = AdtReflect.stringResult(item, "getRequestNumber");
                    if (number != null && !number.isEmpty()) {
                        result.add(new Entry(number, AdtReflect.stringResult(item, "getDescription")));
                    }
                }
            }
            diag.append("resmi transport servisi -> ").append(result.size()).append(" kayıt\n");
        } catch (AdtException | RuntimeException e) {
            diag.append("resmi transport servisi -> ").append(e.getMessage()).append('\n');
        }
        return result;
    }

    private static String destinationIdOf(IProject project) {
        try {
            Class<?> type = Class.forName("com.sap.adt.project.IAdtCoreProject");
            Object adtProject = project.getAdapter(type);
            return adtProject == null ? null
                    : AdtReflect.stringResult(adtProject, "getDestinationId");
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Entry> trySearch(AdtRestClient client, String url, StringBuilder diag) {
        try {
            AdtRestClient.Response response = client.get(url, "*/*");
            diag.append(url).append(" -> HTTP ").append(response.status);
            if (!response.ok()) {
                diag.append(" | ").append(snippet(response.body, 300)).append('\n');
                return new ArrayList<>();
            }
            List<Entry> result = new ArrayList<>();
            for (String section : sections(response.body, "modifiable")) {
                result.addAll(parseRequests(section, false));
            }
            if (result.isEmpty()) {
                result.addAll(parseRequests(response.body, true));
            }
            diag.append(" | ").append(result.size()).append(" kayıt");
            if (result.isEmpty()) {
                diag.append(" | yanıt: ").append(snippet(response.body, 400));
            }
            diag.append('\n');
            return result;
        } catch (AdtException e) {
            diag.append(url).append(" -> ").append(e.getMessage()).append('\n');
            return new ArrayList<>();
        }
    }

    /** Arama konfigürasyonu (configUri) akışı. */
    private static List<Entry> tryConfigSearch(AdtRestClient client, String user, StringBuilder diag) {
        try {
            List<String[]> configs = new ArrayList<>();
            AdtRestClient.Response list = client.get(TRANSPORT_CONFIGS,
                    "application/vnd.sap.adt.configurations.v1+xml");
            diag.append("configurations -> HTTP ").append(list.status);
            if (list.ok()) {
                for (String block : sections(list.body, "configuration:configuration")) {
                    String link = attr(block, "href");
                    String etag = attr(block, "etag");
                    if (link != null && !link.isEmpty()) {
                        configs.add(new String[] { link, etag == null ? "" : etag });
                    }
                }
            } else {
                diag.append(" | ").append(snippet(list.body, 200));
            }
            diag.append(" | ").append(configs.size()).append(" konfigürasyon\n");

            if (configs.isEmpty()) {
                client.post(TRANSPORT_CONFIGS, null, "application/vnd.sap.adt.configuration.v1+xml",
                        null);
                list = client.get(TRANSPORT_CONFIGS, "application/vnd.sap.adt.configurations.v1+xml");
                for (String block : sections(list.body, "configuration:configuration")) {
                    String link = attr(block, "href");
                    String etag = attr(block, "etag");
                    if (link != null && !link.isEmpty()) {
                        configs.add(new String[] { link, etag == null ? "" : etag });
                    }
                }
                diag.append("konfigürasyon oluşturma sonrası: ").append(configs.size()).append('\n');
            }
            if (configs.isEmpty()) {
                return new ArrayList<>();
            }

            String[] first = configs.get(0);
            String link = first[0];
            AdtRestClient.Response cfg = client.get(link,
                    "application/vnd.sap.adt.configuration.v1+xml");
            Map<String, String> props = configProperties(cfg.body);
            String cfgEtag = attr(cfg.body, "etag");
            if (cfgEtag == null || cfgEtag.isEmpty()) {
                cfgEtag = first[1];
            }
            diag.append("config oku -> HTTP ").append(cfg.status)
                    .append(" | User=").append(props.get("User"))
                    .append(" WB=").append(props.get("WorkbenchRequests"))
                    .append(" Mod=").append(props.get("Modifiable"))
                    .append('\n');
            if (!configMatches(props, user)) {
                AdtRestClient.Response put = updateConfig(client, link, cfgEtag, props, user);
                diag.append("config güncelle -> HTTP ").append(put.status).append('\n');
            }
            String url = TRANSPORT_REQUESTS + "?configUri=" + encode(link) + "&targets=true";
            return trySearch(client, url, diag);
        } catch (AdtException e) {
            diag.append("config akışı -> ").append(e.getMessage()).append('\n');
            return new ArrayList<>();
        }
    }

    /** <configuration:property key="..">değer</...> girdilerini okur. */
    private static Map<String, String> configProperties(String xml) {
        Map<String, String> result = new LinkedHashMap<>();
        if (xml == null) {
            return result;
        }
        Matcher m = Pattern.compile(
                "<(?:[\\w-]+:)?property\\b([^>]*?)>(.*?)</(?:[\\w-]+:)?property\\s*>",
                Pattern.DOTALL).matcher(xml);
        while (m.find()) {
            String key = attr("<property " + m.group(1) + ">", "key");
            String value = m.group(2);
            if (key != null && !key.isEmpty()) {
                result.put(key, value == null ? "" : value.trim());
            }
        }
        return result;
    }

    private static boolean configMatches(Map<String, String> props, String user) {
        if (user != null && !user.isEmpty()) {
            String configUser = props.get("User");
            if (configUser == null || !configUser.trim().equalsIgnoreCase(user.trim())) {
                return false;
            }
        }
        return truthy(props.get("WorkbenchRequests")) && truthy(props.get("Modifiable"))
                && !truthy(props.get("Released"));
    }

    private static boolean truthy(String value) {
        return value != null && !value.trim().isEmpty() && !value.trim().equalsIgnoreCase("false");
    }

    /** Konfigürasyonu workbench+modifiable aramasına göre günceller (PUT, If-Match). */
    private static AdtRestClient.Response updateConfig(AdtRestClient client, String link, String etag,
            Map<String, String> props, String user) throws AdtException {
        boolean xStyle = "X".equalsIgnoreCase(props.get("WorkbenchRequests"));
        String on = xStyle ? "X" : "true";
        String off = xStyle ? "" : "false";
        String dateFilter = props.containsKey("DateFilter") && !props.get("DateFilter").isEmpty()
                ? props.get("DateFilter")
                : "1";
        StringBuilder sb = new StringBuilder();
        sb.append("<configuration:configuration xmlns:configuration=\"http://www.sap.com/adt/configuration\">")
                .append("<configuration:properties>")
                .append(prop("WorkbenchRequests", on))
                .append(prop("CustomizingRequests", off))
                .append(prop("TransportOfCopies", off))
                .append(prop("DateFilter", dateFilter))
                .append(prop("Modifiable", on))
                .append(prop("Released", off))
                .append(prop("User", user == null ? "" : user))
                .append("</configuration:properties>")
                .append("</configuration:configuration>");
        return client.put(link, "application/vnd.sap.adt.configuration.v1+xml",
                "application/vnd.sap.adt.configuration.v1+xml", sb.toString(),
                etag == null || etag.isEmpty() ? null : etag);
    }

    /** Teşhis için ham yanıtı kısaltır. */
    private static String snippet(String body, int max) {
        if (body == null) {
            return "(boş)";
        }
        String text = body.replaceAll("\\s+", " ").trim();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...";
    }

    private static String prop(String key, String value) {
        return "<configuration:property key=\"" + key + "\">" + escapeXml(value) + "</configuration:property>";
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static List<Entry> parseRequests(String xml, boolean onlyModifiableStatus) {
        List<Entry> result = new ArrayList<>();
        if (xml == null) {
            return result;
        }
        // Doğrusal tarama: büyük yanıtlarda geri izleme (backtracking) riski yok
        Matcher m = Pattern.compile("<(?:[\\w-]+:)?request\\b[^>]*>", Pattern.CASE_INSENSITIVE)
                .matcher(xml);
        while (m.find()) {
            String openTag = m.group();
            String number = attr(openTag, "number");
            if (number == null || number.isEmpty()) {
                continue;
            }
            String status = attr(openTag, "status");
            if (onlyModifiableStatus && status != null && !status.isEmpty()
                    && !"D".equalsIgnoreCase(status)) {
                continue;
            }
            String desc = attr(openTag, "desc", "description");
            if (desc == null || desc.isEmpty()) {
                int from = m.end();
                int to = Math.min(xml.length(), from + 2000);
                desc = elementText(xml.substring(from, to), "desc");
            }
            result.add(new Entry(number, unescape(desc)));
        }
        return result;
    }

    /** RIS quicksearch ile paket arar (DEVC). */
    public static List<Entry> listPackages(IProject project, String mask) throws AdtException {
        AdtRestClient client = new AdtRestClient(project);
        String maskText = mask == null || mask.isBlank() ? "Z*" : mask.trim();
        if (!maskText.endsWith("*")) {
            maskText = maskText + "*";
        }
        String url = PACKAGE_SEARCH + "?operation=quickSearch&query=" + encode(maskText)
                + "&maxResults=100&objectType=DEVC";
        AdtRestClient.Response response = client.get(url, "application/*");
        if (!response.ok()) {
            throw new AdtException("Paket listesi alınamadı: " + response.summary(600));
        }
        List<Entry> result = new ArrayList<>();
        for (String tag : tags(response.body, "objectReference")) {
            String name = attr(tag, "name");
            if (name == null || name.isEmpty()) {
                continue;
            }
            String description = attr(tag, "description");
            // eski sistemler "ZREPORT (PROGRAM)" biçiminde ad döndürebilir
            Matcher nm = Pattern.compile("([^\\s(]+)\\s*\\((.*)\\)\\s*$").matcher(name);
            if (nm.find()) {
                name = nm.group(1);
                if (description == null || description.isEmpty()) {
                    description = nm.group(2);
                }
            }
            String type = attr(tag, "type");
            if (type != null && !type.toUpperCase(Locale.ROOT).startsWith("DEVC")) {
                continue;
            }
            result.add(new Entry(name, unescape(description)));
        }
        if (result.isEmpty()) {
            throw new AdtException("Paket bulunamadı (" + maskText + "). Ham yanıt: "
                    + response.summary(600));
        }
        return result;
    }

    private static String destinationUserOf(IProject project) {
        try {
            Class<?> type = Class.forName("com.sap.adt.project.IAdtCoreProject");
            Object adtProject = project.getAdapter(type);
            if (adtProject == null) {
                return null;
            }
            Object data = AdtReflect.call(adtProject, "getDestinationData", new Class<?>[0]);
            if (data == null) {
                data = AdtReflect.call(adtProject, "getEffectiveDestinationData", new Class<?>[0]);
            }
            if (data == null) {
                return null;
            }
            return AdtReflect.stringResult(data, "getUser");
        } catch (Exception e) {
            return null;
        }
    }

    /** <...:section> ... </...:section> bölümlerini doğrusal tarar (backtracking yok). */
    private static List<String> sections(String xml, String name) {
        List<String> result = new ArrayList<>();
        if (xml == null) {
            return result;
        }
        Matcher m = Pattern.compile("<(?:[\\w-]+:)?" + name + "\\b[^>]*>",
                Pattern.CASE_INSENSITIVE).matcher(xml);
        while (m.find()) {
            String open = m.group();
            String close = "</" + tagName(open);
            int from = m.end();
            int end = indexOfIgnoreCase(xml, close, from);
            if (end < 0) {
                end = Math.min(xml.length(), from + 500_000);
            }
            result.add(xml.substring(m.start(), end));
        }
        return result;
    }

    private static String tagName(String openTag) {
        Matcher m = Pattern.compile("<([\\w:-]+)").matcher(openTag);
        return m.find() ? m.group(1) : "";
    }

    private static int indexOfIgnoreCase(String haystack, String needle, int from) {
        int max = haystack.length() - needle.length();
        outer:
        for (int i = Math.max(0, from); i <= max; i++) {
            for (int j = 0; j < needle.length(); j++) {
                char a = Character.toLowerCase(haystack.charAt(i + j));
                char b = Character.toLowerCase(needle.charAt(j));
                if (a != b) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /** Açılış etiketlerini yakalar (objectReference vb., kendi kapatan dahil). */
    private static List<String> tags(String xml, String name) {
        List<String> result = new ArrayList<>();
        if (xml == null) {
            return result;
        }
        Matcher m = Pattern.compile("<(?:[\\w-]+:)?" + name + "\\b[^>]*>",
                Pattern.CASE_INSENSITIVE).matcher(xml);
        while (m.find()) {
            result.add(m.group());
        }
        return result;
    }

    private static String attr(String xml, String... names) {
        for (String name : names) {
            Matcher m = Pattern.compile("(?:\\w+:)?" + name + "=(?:\"([^\"]*)\"|'([^']*)')")
                    .matcher(xml);
            if (m.find()) {
                return m.group(1) != null ? m.group(1) : m.group(2);
            }
        }
        return null;
    }

    private static String elementText(String xml, String element) {
        Matcher m = Pattern.compile("<(?:[\\w-]+:)?" + element + "\\b[^>]*>(.*?)</(?:[\\w-]+:)?"
                + element + "\\s*>", Pattern.DOTALL).matcher(xml);
        return m.find() ? unescape(m.group(1)) : null;
    }

    private static String unescape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&apos;", "'");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
