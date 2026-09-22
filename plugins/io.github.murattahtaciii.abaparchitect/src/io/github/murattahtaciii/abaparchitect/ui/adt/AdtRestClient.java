package io.github.murattahtaciii.abaparchitect.ui.adt;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;

public class AdtRestClient {

    private static final String SESSION_FACTORY =
            "com.sap.adt.communication.session.AdtSystemSessionFactory";
    private static final String REQUEST_FACTORY =
            "com.sap.adt.communication.message.AdtRestRequestFactory";
    private static final String HEADERS_FACTORY =
            "com.sap.adt.communication.message.HeadersFactory";
    private static final String BYTE_BODY =
            "com.sap.adt.communication.message.ByteArrayMessageBody";
    private static final String I_HEADERS =
            "com.sap.adt.communication.message.IHeaders";
    private static final String I_FIELD =
            "com.sap.adt.communication.message.IHeaders$IField";
    private static final String I_BODY =
            "com.sap.adt.communication.message.IMessageBody";
    private static final String I_REQUEST =
            "com.sap.adt.communication.message.IRequest";
    private static final String METHOD_ENUM =
            "com.sap.adt.communication.message.IRequest$Method";
    private static final String ADT_CORE_PROJECT =
            "com.sap.adt.project.IAdtCoreProject";
    private static final String REGISTRY_FACTORY =
            "com.sap.adt.communication.destinations.AdtDestinationRegistryFactory";
    private static final String LOGON_FACTORY =
            "com.sap.adt.destinations.logon.AdtLogonServiceFactory";
    private static final String LOGON_UI_FACTORY =
            "com.sap.adt.destinations.ui.logon.AdtLogonServiceUIFactory";
    private static final String I_DESTINATION_DATA =
            "com.sap.adt.destinations.model.IDestinationData";
    private static final String I_AUTH_TOKEN =
            "com.sap.adt.destinations.model.IAuthenticationToken";
    private static final String IS_STATEFUL_SESSION =
            "com.sap.adt.communication.session.IStatefulSystemSession";

    private static final Map<String, Object> SESSION_CACHE = new ConcurrentHashMap<>();

    public static final class Response {

        public final int status;
        public final String reason;
        public final String body;

        Response(int status, String reason, String body) {
            this.status = status;
            this.reason = reason;
            this.body = body;
        }

        public boolean ok() {
            return status >= 200 && status < 300;
        }

        public String summary(int maxLength) {
            String text = body == null ? "" : body.trim();
            if (text.length() > maxLength) {
                text = text.substring(0, maxLength) + "...";
            }
            return "HTTP " + status + (reason == null || reason.isEmpty() ? "" : " " + reason)
                    + (text.isEmpty() ? "" : " | " + text.replaceAll("\\s+", " "));
        }
    }

    private final Object session;
    private final String destinationId;

    public static boolean isAvailable() {
        return AdtReflect.exists(SESSION_FACTORY) && AdtReflect.exists(REQUEST_FACTORY)
                && AdtReflect.exists(I_REQUEST);
    }

    public static String unavailableReason() {
        String[] classes = { SESSION_FACTORY, REQUEST_FACTORY, HEADERS_FACTORY, BYTE_BODY, I_REQUEST,
                METHOD_ENUM, ADT_CORE_PROJECT, LOGON_FACTORY, I_DESTINATION_DATA, I_AUTH_TOKEN };
        for (String className : classes) {
            if (!AdtReflect.exists(className)) {
                return "Eksik ADT sınıfı: " + className
                        + "\nADT kurulu değil ya da sürümü uyumsuz olabilir.";
            }
        }
        return "ADT sınıfları bulundu ancak bağlantı kurulamadı.";
    }

    public static String destinationIdOf(IProject project) {
        Object adtProject = adapt(project);
        if (adtProject == null) {
            return null;
        }
        try {
            return AdtReflect.stringResult(adtProject, "getDestinationId");
        } catch (AdtException e) {
            return null;
        }
    }

    public static boolean isAdtProject(IProject project) {
        Object adtProject = adapt(project);
        if (adtProject == null) {
            return false;
        }
        try {
            return AdtReflect.stringResult(adtProject, "getDestinationId") != null;
        } catch (AdtException e) {
            return false;
        }
    }

    public AdtRestClient(IProject project) throws AdtException {
        Object adtProject = adapt(project);
        if (adtProject == null) {
            throw new AdtException("Seçilen proje bir ADT/ABAP projesi değil.");
        }
        Object destinationData = AdtReflect.call(adtProject, "getEffectiveDestinationData", new Class<?>[0]);
        if (destinationData == null) {
            destinationData = AdtReflect.call(adtProject, "getDestinationData", new Class<?>[0]);
        }
        if (destinationData == null) {
            throw new AdtException("Proje için SAP bağlantı bilgisi (destination) bulunamadı.\n"
                    + "ADT projesine sağ tıklayıp 'Log On' yapın ve tekrar deneyin.");
        }
        String destinationId = AdtReflect.stringResult(destinationData, "getId");
        if (destinationId == null || destinationId.isEmpty()) {
            throw new AdtException("SAP destination kimliği alınamadı.");
        }

        ensureRegistered(destinationData, destinationId);
        ensureLoggedOn(destinationData, destinationId);

        try {
            this.session = acquireSession(destinationId);
            this.destinationId = destinationId;
        } catch (AdtException e) {
            throw new AdtException("SAP sistemine bağlantı kurulamadı (" + destinationId + ").\n"
                    + "ADT projesine sağ tıklayıp 'Log On' yapıp tekrar deneyin.\n\n"
                    + e.getMessage(), e);
        }
    }

    private static Object acquireSession(String destinationId) throws AdtException {
        Object cached = SESSION_CACHE.get(destinationId);
        if (cached != null) {
            try {
                Object open = AdtReflect.call(cached, "isOpen", new Class<?>[0]);
                if (Boolean.TRUE.equals(open)) {
                    return cached;
                }
            } catch (AdtException ignored) {
                // kapalı/bozuk oturum; yenisi oluşturulur
            }
            SESSION_CACHE.remove(destinationId, cached);
        }
        Object session = createSession(destinationId);
        SESSION_CACHE.put(destinationId, session);
        return session;
    }

    private static Object createSession(String destinationId) throws AdtException {
        Object factory = AdtReflect.callStatic(SESSION_FACTORY, "createSystemSessionFactory",
                new Class<?>[0]);
        return AdtReflect.call(factory, "createStatefulSession", new Class<?>[] { String.class },
                destinationId);
    }

    private static void ensureRegistered(Object destinationData, String destinationId)
            throws AdtException {
        Object registry = AdtReflect.callStatic(REGISTRY_FACTORY, "getDestinationRegistry",
                new Class<?>[0]);
        Object registered = AdtReflect.call(registry, "isRegistered",
                new Class<?>[] { String.class }, destinationId);
        if (Boolean.TRUE.equals(registered)) {
            return;
        }
        Object status = AdtReflect.call(registry, "register",
                new Class<?>[] { AdtReflect.load(I_DESTINATION_DATA), AdtReflect.load(I_AUTH_TOKEN) },
                destinationData, null);
        if (status != null && !isStatusOk(status)) {
            throw new AdtException(
                    "Destination kaydı yapılamadı (" + destinationId + "): " + statusText(status),
                    statusException(status));
        }
    }

    private static void ensureLoggedOn(Object destinationData, String destinationId)
            throws AdtException {
        Object service = AdtReflect.callStatic(LOGON_FACTORY, "createLogonService", new Class<?>[0]);
        Object loggedOn = AdtReflect.call(service, "isLoggedOn",
                new Class<?>[] { String.class }, destinationId);
        if (Boolean.TRUE.equals(loggedOn)) {
            return;
        }
        // Log On diyalogu yalnızca UI thread'inde açılabilir (arka plan işlerinde SWTException olur)
        if (AdtReflect.exists(LOGON_UI_FACTORY) && isUiThread()) {
            Object status = logOnWithUi(destinationData);
            if (isStatusOk(status)) {
                return;
            }
            throw new AdtException("SAP oturumu açılamadı (" + destinationId + ").\n"
                    + "ADT projesine sağ tıklayıp 'Log On' yapıp tekrar deneyin.\n"
                    + statusText(status), statusException(status));
        }
        Object status = AdtReflect.call(service, "ensureLoggedOn",
                new Class<?>[] { AdtReflect.load(I_DESTINATION_DATA), AdtReflect.load(I_AUTH_TOKEN),
                        org.eclipse.core.runtime.IProgressMonitor.class },
                destinationData, null, null);
        if (status != null && !isStatusOk(status)) {
            throw new AdtException("SAP oturumu açılamadı (" + destinationId + ").\n"
                    + "ADT projesine sağ tıklayıp 'Log On' yapıp tekrar deneyin.\n"
                    + statusText(status), statusException(status));
        }
    }

    /** Geçerli thread SWT UI thread'i mi? (arka plan işlerinde UI çağrıları yasak) */
    private static boolean isUiThread() {
        try {
            return org.eclipse.swt.widgets.Display.getCurrent() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object logOnWithUi(Object destinationData) throws AdtException {
        Object uiService = AdtReflect.callStatic(LOGON_UI_FACTORY, "createLogonServiceUI",
                new Class<?>[0]);
        Object context = null;
        try {
            Object workbench = AdtReflect.callStatic("org.eclipse.ui.PlatformUI", "getWorkbench",
                    new Class<?>[0]);
            context = AdtReflect.call(workbench, "getProgressService", new Class<?>[0]);
        } catch (AdtException ignored) {
            context = null;
        }
        return AdtReflect.call(uiService, "ensureLoggedOn",
                new Class<?>[] { AdtReflect.load(I_DESTINATION_DATA),
                        org.eclipse.jface.operation.IRunnableContext.class },
                destinationData, context);
    }

    private static boolean isStatusOk(Object status) {
        if (status == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(AdtReflect.call(status, "isOK", new Class<?>[0]));
        } catch (AdtException e) {
            return false;
        }
    }

    private static String statusText(Object status) {
        if (status == null) {
            return "durum bilgisi yok";
        }
        return String.valueOf(status);
    }

    private static Throwable statusException(Object status) {
        try {
            Object exception = status == null ? null : AdtReflect.call(status, "getException");
            return exception instanceof Throwable ? (Throwable) exception : null;
        } catch (AdtException e) {
            return null;
        }
    }

    private static Object adapt(IProject project) {
        if (project == null) {
            return null;
        }
        try {
            Class<?> type = Class.forName(ADT_CORE_PROJECT);
            return project.getAdapter(type);
        } catch (Exception e) {
            return null;
        }
    }

    public Response execute(String method, String url, String contentType, String accept, String body,
            java.util.Map<String, String> extraHeaders)
            throws AdtException {
        try {
            Class<?> iHeaders = AdtReflect.load(I_HEADERS);
            Class<?> iField = AdtReflect.load(I_FIELD);
            Class<?> iRequestBody = AdtReflect.load(I_BODY);
            Class<?> methodEnum = AdtReflect.load(METHOD_ENUM);
            Class<?> iRequest = AdtReflect.load(I_REQUEST);

            Object headers = AdtReflect.callStatic(HEADERS_FACTORY, "newHeaders", new Class<?>[0]);
            if (accept != null) {
                addHeader(headers, iField, "Accept", accept);
            }
            if (contentType != null) {
                addHeader(headers, iField, "Content-Type", contentType);
            }
            if (extraHeaders != null) {
                for (java.util.Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                    addHeader(headers, iField, entry.getKey(), entry.getValue());
                }
            }

            String bodyContentType = contentType == null ? "text/plain; charset=utf-8" : contentType;
            byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            Object messageBody = bytes.length == 0 && body == null
                    ? null
                    : AdtReflect.callStatic(BYTE_BODY, "<init>", new Class<?>[] { String.class, byte[].class },
                            bodyContentType, bytes);

            Object requestFactory = AdtReflect.callStatic(REQUEST_FACTORY, "createRestRequestFactory",
                    new Class<?>[0]);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object methodValue = Enum.valueOf((Class<Enum>) (Class) methodEnum, method.toUpperCase());
            Object request = AdtReflect.call(requestFactory, "createInstance",
                    new Class<?>[] { methodEnum, URI.class, iHeaders, iRequestBody },
                    methodValue, URI.create(url), headers, messageBody);

            Class<?> monitorClass = org.eclipse.core.runtime.IProgressMonitor.class;
            Object response;
            // JCo/SAPGUI bağlantıları eşzamanlı çağrıya izin vermez:
            // aynı oturumdaki istekler kilitlenerek sırayla gönderilir.
            synchronized (session) {
                response = AdtReflect.call(session, "sendRequest",
                        new Class<?>[] { monitorClass, iRequest }, null, request);
            }

            int status = ((Number) AdtReflect.call(response, "getStatus", new Class<?>[0])).intValue();
            String reason = AdtReflect.stringResult(response, "getReasonPhrase");
            if (status == 401) {
                invalidateSession();
            }
            return new Response(status, reason, readBody(response));
        } catch (AdtException e) {
            invalidateSession();
            throw e;
        } catch (Exception e) {
            invalidateSession();
            throw new AdtException("ADT isteği başarısız: " + method + " " + url + " -> " + e, e);
        }
    }

    private void invalidateSession() {
        SESSION_CACHE.remove(destinationId, session);
    }

    public Response get(String url, String accept) throws AdtException {
        return execute("GET", url, null, accept, null, null);
    }

    public Response post(String url, String contentType, String accept, String body) throws AdtException {
        return execute("POST", url, contentType, accept, body, null);
    }

    public Response put(String url, String contentType, String accept, String body) throws AdtException {
        return execute("PUT", url, contentType, accept, body, null);
    }

    public Response put(String url, String contentType, String accept, String body,
            String ifMatch) throws AdtException {
        java.util.Map<String, String> extra = ifMatch == null ? null : new java.util.HashMap<>();
        if (extra != null) {
            extra.put("If-Match", ifMatch);
        }
        return execute("PUT", url, contentType, accept, body, extra);
    }

    private static void addHeader(Object headers, Class<?> iField, String name, String value) throws AdtException {
        Object field = AdtReflect.callStatic(HEADERS_FACTORY, "newField",
                new Class<?>[] { String.class, String[].class }, name, new String[] { value });
        AdtReflect.call(headers, "addField", new Class<?>[] { iField }, field);
    }

    private static String readBody(Object response) throws AdtException {
        Object messageBody = AdtReflect.call(response, "getBody", new Class<?>[0]);
        if (messageBody == null) {
            return "";
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AdtReflect.call(messageBody, "writeTo", new Class<?>[] { OutputStream.class }, out);
            return out.toString(StandardCharsets.UTF_8);
        } catch (AdtException e) {
            return "";
        }
    }
}
