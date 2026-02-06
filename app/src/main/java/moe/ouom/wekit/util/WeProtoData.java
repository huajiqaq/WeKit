package moe.ouom.wekit.util;

import static cn.hutool.core.convert.Convert.hexToBytes;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import moe.ouom.wekit.util.log.WeLogger;

public class WeProtoData {

    /** A single field occurrence in the original stream, in exact order. */
    private static final class Field {
        final int fieldNumber;
        final int wireType;

        // For wireType 0: Long (varint)
        // For wireType 1: Long (fixed64)
        // For wireType 5: Integer (fixed32)
        // For wireType 2: LenValue
        Object value;

        Field(int fieldNumber, int wireType, Object value) {
            this.fieldNumber = fieldNumber;
            this.wireType = wireType;
            this.value = value;
        }
    }

    /**
     * wireType=2 payload holder.
     * We preserve raw bytes to keep exact HEX if unchanged.
     * Optionally we keep decoded UTF-8 string view and/or parsed submessage view (lazy-ish).
     */
    private static final class LenValue {
        byte[] raw;                 // authoritative bytes to write if unchanged
        String utf8;                // if raw is valid UTF-8 roundtrip
        WeProtoData subMessage;    // if raw can be parsed as protobuf-like structure

        LenValue(byte[] raw) {
            this.raw = raw;
            this.utf8 = tryDecodeUtf8Roundtrip(raw);
            this.subMessage = tryParseSubMessage(raw);
        }

        static String tryDecodeUtf8Roundtrip(byte[] b) {
            try {
                String s = new String(b, StandardCharsets.UTF_8);
                byte[] re = s.getBytes(StandardCharsets.UTF_8);
                if (Arrays.equals(b, re)) return s;
            } catch (Exception ignored) { }
            return null;
        }

        static WeProtoData tryParseSubMessage(byte[] b) {
            try {
                WeProtoData sub = new WeProtoData();
                sub.fromBytes(b);
                // Heuristic: if parsed produced at least 1 field, treat as submessage view
                // (Still keep raw as authoritative unless modified)
                if (!sub.fields.isEmpty()) return sub;
            } catch (Exception ignored) { }
            return null;
        }
    }

    /** Preserve exact occurrence order. */
    private final List<Field> fields = new ArrayList<>();

    public static byte[] getUnpPackage(byte[] b) {
        if (b == null) return null;
        if (b.length < 4) return b;
        if ((b[0] & 0xFF) == 0) {
            return Arrays.copyOfRange(b, 4, b.length);
        } else {
            return b;
        }
    }

    /** Clears current fields. */
    public void clear() {
        fields.clear();
    }

    /** Parse from protobuf-like bytes, preserving exact field order and raw bytes for len-delimited. */
    public void fromBytes(byte[] b) throws IOException {
        clear();
        if (b == null) return;

        CodedInputStream in = CodedInputStream.newInstance(b);
        while (!in.isAtEnd()) {
            final int tag;
            try {
                tag = in.readTag();
            } catch (InvalidProtocolBufferException e) {
                throw new InvalidProtocolBufferException(e);
            }

            if (tag == 0) break;

            int fieldNumber = tag >>> 3;
            int wireType = tag & 7;

            if (wireType == 4 || wireType == 3 || wireType > 5) {
                throw new IOException("Unexpected wireType: " + wireType);
            }

            switch (wireType) {
                case 0: { // varint
                    long v = in.readInt64(); // preserves negative correctly
                    fields.add(new Field(fieldNumber, wireType, v));
                    break;
                }
                case 1: { // fixed64  (FIX: your old code readRawVarint64() which is WRONG)
                    long v = in.readFixed64();
                    fields.add(new Field(fieldNumber, wireType, v));
                    break;
                }
                case 2: { // len-delimited
                    byte[] subBytes = in.readByteArray();
                    fields.add(new Field(fieldNumber, wireType, new LenValue(subBytes)));
                    break;
                }
                case 5: { // fixed32
                    int v = in.readFixed32();
                    fields.add(new Field(fieldNumber, wireType, v));
                    break;
                }
                default:
                    // unreachable due to check
                    break;
            }
        }
    }

    /**
     * Convert to JSON for debugging/view.
     * IMPORTANT: JSON is only a VIEW. Do NOT rebuild bytes from JSON if you care about identical HEX.
     */
    public JSONObject toJSON() throws Exception {
        JSONObject obj = new JSONObject();

        // group by field number but keep occurrence order inside arrays
        // JSON object key order is not guaranteed, but that's fine for viewing.
        for (Field f : fields) {
            String k = String.valueOf(f.fieldNumber);
            Object jsonVal = fieldValueToJsonValue(f);

            if (!obj.has(k)) {
                obj.put(k, jsonVal);
            } else {
                Object existing = obj.get(k);
                JSONArray arr;
                if (existing instanceof JSONArray) {
                    arr = (JSONArray) existing;
                } else {
                    arr = new JSONArray();
                    arr.put(existing);
                    obj.put(k, arr);
                }
                arr.put(jsonVal);
            }
        }
        return obj;
    }

    private Object fieldValueToJsonValue(Field f) throws Exception {
        if (f.wireType == 2) {
            LenValue lv = (LenValue) f.value;
            // Prefer submessage view if available, else utf8 string if roundtrip valid, else hex marker
            if (lv.subMessage != null) return lv.subMessage.toJSON();
            if (lv.utf8 != null) return lv.utf8;
            return "hex->" + bytesToHex(lv.raw);
        }
        return f.value;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }

    /**
     * Serialize back.
     * If nothing has been modified, this will match the original bytes exactly because:
     * - fields are emitted in original order
     * - wire types preserved
     * - wireType=2 emits preserved raw bytes
     */
    public byte[] toBytes() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(bos);
        try {
            for (Field f : fields) {
                switch (f.wireType) {
                    case 0: { // varint
                        long v = (Long) f.value;
                        if (v >= 0) out.writeUInt64(f.fieldNumber, v);
                        else out.writeInt64(f.fieldNumber, v);
                        break;
                    }
                    case 1: { // fixed64
                        long v = (Long) f.value;
                        out.writeFixed64(f.fieldNumber, v);
                        break;
                    }
                    case 2: { // len-delimited
                        LenValue lv = (LenValue) f.value;
                        // If subMessage exists and has been modified via our APIs, its raw might be stale.
                        // We treat lv.raw as authoritative unless we explicitly refresh it.
                        out.writeByteArray(f.fieldNumber, lv.raw);
                        break;
                    }
                    case 5: { // fixed32
                        int v = (Integer) f.value;
                        out.writeFixed32(f.fieldNumber, v);
                        break;
                    }
                    default:
                        // unreachable
                        break;
                }
            }
            out.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            WeLogger.e("FunProtoData - toBytes", e);
            return new byte[0];
        }
    }

    /**
     * Replace any UTF-8 string field (wireType=2) whose decoded string contains "needle".
     * This preserves exact HEX for all other fields.
     *
     * @return number of replacements performed
     */
    public int replaceUtf8Contains(String needle, String replacement) {
        if (needle == null || needle.isEmpty()) return 0;

        int count = 0;
        for (Field f : fields) {
            if (f.wireType != 2) continue;
            LenValue lv = (LenValue) f.value;

            // If it is a submessage, recurse into it too.
            if (lv.subMessage != null) {
                count += lv.subMessage.replaceUtf8Contains(needle, replacement);
                // If submessage changed, refresh raw to reflect changes.
                // (Only then will bytes differ, as intended)
                if (count > 0) {
                    lv.raw = lv.subMessage.toBytes();
                    lv.utf8 = LenValue.tryDecodeUtf8Roundtrip(lv.raw);
                    // keep subMessage as is
                }
            }

            // If it is a UTF8 string, replace
            if (lv.utf8 != null && lv.utf8.contains(needle)) {
                lv.utf8 = lv.utf8.replace(needle, replacement);
                lv.raw = lv.utf8.getBytes(StandardCharsets.UTF_8);
                // After turning into plain string bytes, subMessage view no longer trustworthy
                lv.subMessage = null;
                count++;
            }
        }
        return count;
    }

    /**
     * Compatibility method (NOT hex-stable).
     * JSON cannot preserve:
     * - order of occurrences
     * - wire types
     * - raw bytes of len-delimited
     * So rebuilding from JSON can never guarantee identical HEX.
     *
     * Keep this only if you REALLY need it.
     */
    public void fromJSON(JSONObject json) {
        try {
            clear();
            Iterator<String> keyIt = json.keys();
            while (keyIt.hasNext()) {
                String key = keyIt.next();
                int fieldNumber = Integer.parseInt(key);
                Object value = json.get(key);

                if (value instanceof JSONObject) {
                    WeProtoData sub = new WeProtoData();
                    sub.fromJSON((JSONObject) value);
                    // wireType=2 because JSON can't represent other wire types for embedded
                    LenValue lv = new LenValue(sub.toBytes());
                    fields.add(new Field(fieldNumber, 2, lv));
                } else if (value instanceof JSONArray arr) {
                    for (int i = 0; i < arr.length(); i++) {
                        Object v = arr.get(i);
                        addJsonValueAsField(fieldNumber, v);
                    }
                } else {
                    addJsonValueAsField(fieldNumber, value);
                }
            }
        } catch (Exception ignored) { }
    }

    private void addJsonValueAsField(int fieldNumber, Object value) {
        try {
            if (value instanceof JSONObject) {
                WeProtoData sub = new WeProtoData();
                sub.fromJSON((JSONObject) value);
                LenValue lv = new LenValue(sub.toBytes());
                fields.add(new Field(fieldNumber, 2, lv));
            } else if (value instanceof Number) {
                long v = ((Number) value).longValue();
                fields.add(new Field(fieldNumber, 0, v)); // best effort: varint
            } else if (value instanceof String s) {
                if (s.startsWith("hex->")) {
                    byte[] raw = hexToBytes(s.substring(5));
                    fields.add(new Field(fieldNumber, 2, new LenValue(raw)));
                } else {
                    byte[] raw = s.getBytes(StandardCharsets.UTF_8);
                    fields.add(new Field(fieldNumber, 2, new LenValue(raw)));
                }
            } else if (value == null) {
                // ignore
            } else {
                WeLogger.w("FunProtoData.fromJSON Unknown type: " + value.getClass().getName());
            }
        } catch (Exception ignored) { }
    }
}
