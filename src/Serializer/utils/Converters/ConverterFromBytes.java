package Serializer.utils.Converters;

import javafx.util.Pair;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;


public class ConverterFromBytes {
    public static Pair<Integer, Integer> getInt(byte[] data, int offset) {
        byte[] dataToConvert = Arrays.copyOfRange(data, offset, offset + 4);
        return new Pair<>(ByteBuffer.wrap(dataToConvert).getInt(), 4);
    }

    public static Pair<Character, Integer> getChar(byte[] data, int offset) {
        byte[] dataToConvert = Arrays.copyOfRange(data, offset, offset + 2);
        return new Pair<>(ByteBuffer.wrap(dataToConvert).getChar(), 2);
    }

    public static Pair<Byte, Integer> getByte(byte[] data, int offset) {
        return new Pair<>(data[offset], 1);
    }

    public static Pair<Short, Integer> getShort(byte[] data, int offset) {
        byte[] dataToConvert = Arrays.copyOfRange(data, offset, offset + 2);
        return new Pair<>(ByteBuffer.wrap(dataToConvert).getShort(), 2);
    }

    public static Pair<Long, Integer> getLong(byte[] data, int offset) {
        byte[] dataToConvert = Arrays.copyOfRange(data, offset, offset + 8);
        return new Pair<>(ByteBuffer.wrap(dataToConvert).getLong(), 8);
    }

    public static Pair<Boolean, Integer> getBoolean(byte[] data, int offset) {
        return new Pair<>(data[offset] == 1, 1);
    }

    public static Pair<Double, Integer> getDouble(byte[] data, int offset) {
        byte[] dataToConvert = Arrays.copyOfRange(data, offset, offset + 8);
        return new Pair<>(ByteBuffer.wrap(dataToConvert).getDouble(), 8);
    }

    public static Pair<Float, Integer> getFloat(byte[] data, int offset) {
        byte[] dataToConvert = Arrays.copyOfRange(data, offset, offset + 4);
        return new Pair<>(ByteBuffer.wrap(dataToConvert).getFloat(), 4);
    }

    public static Pair<String, Integer> getString(byte[] data, int offset) {
        Pair<Integer, Integer> pairForStrLen = getInt(data, offset);
        int strLen = pairForStrLen.getKey();
        int resOffset = pairForStrLen.getValue() + strLen;
        return new Pair<>(new String(data, offset + 4, strLen, UTF_8), resOffset);
    }
}
