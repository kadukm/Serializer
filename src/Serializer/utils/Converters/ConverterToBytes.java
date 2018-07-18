package Serializer.utils.Converters;


import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;


//TODO: rewrite .allocate to .wrap(arrayToReturn)
public class ConverterToBytes {
    public static byte[] convertInt(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] convertChar(char value) {
        return ByteBuffer.allocate(2).putChar(value).array();
    }

    public static byte[] convertByte(byte value) {
        return new byte[]{value};
    }

    public static byte[] convertShort(short value) {
        return ByteBuffer.allocate(2).putShort(value).array();
    }

    public static byte[] convertLong(long value) {
        return ByteBuffer.allocate(8).putLong(value).array();
    }

    public static byte[] convertBoolean(boolean value) {
        return new byte[]{(byte) (value ? 1 : 0)};
    }

    public static byte[] convertDouble(double value) {
        return ByteBuffer.allocate(8).putDouble(value).array();
    }

    public static byte[] convertFloat(float value) {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }

    public static byte[] convertString(String value) {
        byte[] valueArray = value.getBytes(UTF_8);
        byte[] lenArray = convertInt(value.length());
        byte[] res = new byte[valueArray.length + lenArray.length];
        System.arraycopy(lenArray, 0, res, 0, lenArray.length);
        System.arraycopy(valueArray, 0, res, lenArray.length, valueArray.length);
        return res;
    }
}
