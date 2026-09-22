package com.murattahtaci.abaparchitect.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal, bağımlılıksız JSON ayrıştırıcı. Formül:
 * object -> LinkedHashMap, array -> ArrayList, string -> String,
 * tam sayı -> Long/BigInteger, ondalık -> BigDecimal, boolean -> Boolean, null -> null.
 */
public final class JsonParser {

    private static final int MAX_DEPTH = 250;

    private final String source;
    private int pos;
    private int line = 1;
    private int column = 1;
    private int depth;

    private JsonParser(String source) {
        this.source = source;
    }

    public static Object parse(String text) throws JsonParseException {
        if (text == null || text.trim().isEmpty()) {
            throw new JsonParseException("Boş içerik", 1, 1);
        }
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        JsonParser parser = new JsonParser(text);
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw parser.error("Beklenmeyen karakter");
        }
        return value;
    }

    private Object readValue() throws JsonParseException {
        skipWhitespace();
        if (isEnd()) {
            throw error("Beklenmeyen dosya sonu");
        }
        char c = peek();
        if (c == '{') {
            return readObject();
        }
        if (c == '[') {
            return readArray();
        }
        if (c == '"') {
            return readString();
        }
        if (c == 't') {
            return readLiteral("true", Boolean.TRUE);
        }
        if (c == 'f') {
            return readLiteral("false", Boolean.FALSE);
        }
        if (c == 'n') {
            return readLiteral("null", null);
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            return readNumber();
        }
        throw error("Beklenmeyen karakter '" + c + "'");
    }

    private Map<String, Object> readObject() throws JsonParseException {
        enter();
        Map<String, Object> result = new LinkedHashMap<>();
        read();
        skipWhitespace();
        if (peek() == '}') {
            read();
            leave();
            return result;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("Nesne anahtarı için '\"' bekleniyordu");
            }
            String key = readString();
            skipWhitespace();
            if (peek() != ':') {
                throw error("':' bekleniyordu");
            }
            read();
            Object value = readValue();
            result.put(key, value);
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                read();
                continue;
            }
            if (c == '}') {
                read();
                leave();
                return result;
            }
            throw error("Nesne içinde ',' veya '}' bekleniyordu");
        }
    }

    private List<Object> readArray() throws JsonParseException {
        enter();
        List<Object> result = new ArrayList<>();
        read();
        skipWhitespace();
        if (peek() == ']') {
            read();
            leave();
            return result;
        }
        while (true) {
            result.add(readValue());
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                read();
                continue;
            }
            if (c == ']') {
                read();
                leave();
                return result;
            }
            throw error("Dizi içinde ',' veya ']' bekleniyordu");
        }
    }

    private String readString() throws JsonParseException {
        read();
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (isEnd()) {
                throw error("Kapatılmamış metin");
            }
            char c = read();
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (isEnd()) {
                    throw error("Kapatılmamış kaçış dizisi");
                }
                char esc = read();
                switch (esc) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 > source.length()) {
                            throw error("Eksik unicode kaçışı");
                        }
                        String hex = source.substring(pos, pos + 4);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw error("Geçersiz unicode kaçışı '\\u" + hex + "'");
                        }
                        for (int i = 0; i < 4; i++) {
                            read();
                        }
                        break;
                    default:
                        throw error("Geçersiz kaçış dizisi '\\" + esc + "'");
                }
                continue;
            }
            if (c < 0x20) {
                throw error("Metin içinde geçersiz kontrol karakteri");
            }
            sb.append(c);
        }
    }

    private Object readNumber() throws JsonParseException {
        int start = pos;
        if (peek() == '-') {
            read();
        }
        while (!isEnd() && peek() >= '0' && peek() <= '9') {
            read();
        }
        boolean decimal = false;
        if (!isEnd() && peek() == '.') {
            decimal = true;
            read();
            if (isEnd() || peek() < '0' || peek() > '9') {
                throw error("Ondalık kısım bekleniyordu");
            }
            while (!isEnd() && peek() >= '0' && peek() <= '9') {
                read();
            }
        }
        if (!isEnd() && (peek() == 'e' || peek() == 'E')) {
            decimal = true;
            read();
            if (!isEnd() && (peek() == '+' || peek() == '-')) {
                read();
            }
            if (isEnd() || peek() < '0' || peek() > '9') {
                throw error("Üs kısmı bekleniyordu");
            }
            while (!isEnd() && peek() >= '0' && peek() <= '9') {
                read();
            }
        }
        String token = source.substring(start, pos);
        try {
            if (decimal) {
                return new BigDecimal(token);
            }
            return new BigInteger(token);
        } catch (NumberFormatException e) {
            throw error("Geçersiz sayı '" + token + "'");
        }
    }

    private Object readLiteral(String literal, Object value) throws JsonParseException {
        if (source.startsWith(literal, pos)) {
            for (int i = 0; i < literal.length(); i++) {
                read();
            }
            return value;
        }
        throw error("Geçersiz değer");
    }

    private void enter() throws JsonParseException {
        if (++depth > MAX_DEPTH) {
            throw error("Maksimum iç içe geçme derinliği aşıldı");
        }
    }

    private void leave() {
        depth--;
    }

    private void skipWhitespace() {
        while (!isEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t') {
                pos++;
                column++;
            } else if (c == '\n') {
                pos++;
                line++;
                column = 1;
            } else if (c == '\r') {
                pos++;
                if (!isEnd() && peek() == '\n') {
                    pos++;
                }
                line++;
                column = 1;
            } else {
                return;
            }
        }
    }

    private char peek() {
        return source.charAt(pos);
    }

    private char read() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private boolean isEnd() {
        return pos >= source.length();
    }

    private JsonParseException error(String message) {
        return new JsonParseException(message, line, column);
    }
}
