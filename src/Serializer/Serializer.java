package Serializer;


import Serializer.utils.ClassDescriptor;
import Serializer.utils.Converters.ConverterFromBytes;
import Serializer.utils.Converters.ConverterToBytes;
import Serializer.utils.Errors.DeserializationError;
import Serializer.utils.Errors.SerializationError;
import Serializer.utils.IdentityKeyWrapper;


import javafx.util.Pair;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * It can work only with 255 types at one moment (11 of them is reserved)
 * </br>
 * It can serialize array of arrays but can't deserialize it
 * </br>
 * It can work only with 254 non-primitive objects at one moment
 */
public class Serializer {
    public static byte[] dumps(Object obj) throws SerializationError {
        Encoder encoder = new Encoder();
        return encoder.encode(obj);
    }

    public static Object loads(byte[] bytes) throws DeserializationError {
        Decoder decoder = new Decoder();
        return decoder.decode(bytes);
    }

    private static class Encoder extends Coder {
        private byte objIDLen;  //TODO: Assign the var and work with different lengths
        private byte maxObjID;
        private ArrayList<ByteArrayOutputStream> clsDescriptorStreams;
        private ByteArrayOutputStream bodyStream;
        private byte maxClsFlag;
        private HashMap<IdentityKeyWrapper, Byte> objectToID;
        private static HashMap<Byte, IConverter> converters;
        private interface IConverter {
            byte[] getValue(Object obj);
        }

        static {
            converters = new HashMap<>();
            converters.put(flagsForPrimitives.get(Integer.TYPE),
                    (obj) -> ConverterToBytes.convertInt((int)obj));
            converters.put(flagsForPrimitives.get(Character.TYPE),
                    (obj) -> ConverterToBytes.convertChar((char)obj));
            converters.put(flagsForPrimitives.get(Byte.TYPE),
                    (obj) -> ConverterToBytes.convertByte((byte)obj));
            converters.put(flagsForPrimitives.get(Short.TYPE),
                    (obj) -> ConverterToBytes.convertShort((short)obj));
            converters.put(flagsForPrimitives.get(Long.TYPE),
                    (obj) -> ConverterToBytes.convertLong((long)obj));
            converters.put(flagsForPrimitives.get(Boolean.TYPE),
                    (obj) -> ConverterToBytes.convertBoolean((boolean)obj));
            converters.put(flagsForPrimitives.get(Double.TYPE),
                    (obj) -> ConverterToBytes.convertDouble((double)obj));
            converters.put(flagsForPrimitives.get(Float.TYPE),
                    (obj) -> ConverterToBytes.convertFloat((float)obj));
            converters.put(flagsForPrimitives.get(String.class),
                    (obj) -> ConverterToBytes.convertString((String)obj));
        }

        private Encoder() {
            super();
            maxObjID = 1;
            objectToID = new HashMap<>();
            clsDescriptorStreams = new ArrayList<>();
            bodyStream = new ByteArrayOutputStream();
            maxClsFlag = reservedBytesCount - 1;
        }

        private byte[] encode(Object obj) throws SerializationError {
            if (obj == null)
                throw new SerializationError("Can't serialize just null object");

            Class<?> clazz = obj.getClass();
            Byte clsFlag = clsFlags.get(clazz);
            if (clsFlag == null) {
                encode(obj, clazz, true);
            }
            else {
                bodyStream.write(clsFlag);
                IConverter converter = converters.get(clsFlag);
                byte[] data = converter.getValue(obj);
                bodyStream.write(data, 0, data.length);
            }

            return getResult();
        }

        private void encode(Object obj, Class<?> clazz, boolean withClsFlag) throws SerializationError {
            if (clazz.isArray())
                encodeArray(obj, withClsFlag);
            else
                encodeObject(obj, clazz, withClsFlag);
        }

        //TODO: maybe I have to add descriptors once in encode(Object) method
        private Byte addClsDescriptor(Class<?> clazz) {
            byte resFlag = ++maxClsFlag;
            clsFlags.put(clazz, resFlag);
            ByteArrayOutputStream curClsDescriptorStream = new ByteArrayOutputStream();
            clsDescriptorStreams.add(curClsDescriptorStream);
            curClsDescriptorStream.write(clsDescriptorByte);
            curClsDescriptorStream.write(resFlag);
            byte[] clsNameDescription = ConverterToBytes.convertString(clazz.getName());
            curClsDescriptorStream.write(clsNameDescription, 0, clsNameDescription.length);

            ClassDescriptor clazzDescriptor = ClassDescriptor.getDescriptorFor(clazz);
            clsDescriptors.put(resFlag, clazzDescriptor);
            byte[] fieldsCountData = ConverterToBytes.convertInt(clazzDescriptor.fields.size());
            curClsDescriptorStream.write(fieldsCountData, 0, fieldsCountData.length);
            for (Field field : clazzDescriptor.fields) {
                Class<?> fieldType = field.getType();
                String strFieldName = field.getName();
                Byte clsFlagForField = clsFlags.get(fieldType);
                if (clsFlagForField == null)
                    //TODO: Add clsFlag if it's an array (i.e. arrayFlagByte + clsFlagByte)
                    clsFlagForField = (fieldType.isArray()) ? arrayFlagByte : addClsDescriptor(fieldType);
                byte[] fieldDescription = ConverterToBytes.convertString(strFieldName);
                curClsDescriptorStream.write(fieldDescription, 0, fieldDescription.length);
                curClsDescriptorStream.write(clsFlagForField);
            }
            return resFlag;
        }

        private void encodeObject(Object obj, Class<?> clazz, boolean withClsFlag) throws SerializationError {
            Byte clsFlag = clsFlags.get(clazz);
            if (clsFlag == null)
                clsFlag = addClsDescriptor(clazz);
            if (withClsFlag)
                bodyStream.write(clsFlag);

            if (obj == null) {
                bodyStream.write(isNullFlag);
                return;
            }
            IdentityKeyWrapper objWrapper = new IdentityKeyWrapper(obj);
            Byte objID = objectToID.get(objWrapper);
            if (objID != null) {
                bodyStream.write(isLinkFlag);
                bodyStream.write(objID);
                return;
            }

            objID = ++maxObjID;
            bodyStream.write(objID);
            objectToID.put(objWrapper, objID);

            ClassDescriptor clsDescriptor = clsDescriptors.get(clsFlag);
            for (Field field : clsDescriptor.fields) {
                Class<?> fieldType = field.getType();
                try {
                    field.setAccessible(true);
                } catch (SecurityException e) {
                    throw new SerializationError(
                            String.format("Can't get access to the field \"%s\"", field.getName()),
                            e.getCause()
                    );
                }
                Object fieldValue;
                try {
                    fieldValue = field.get(obj);
                } catch (IllegalAccessException e) {
                    throw new SerializationError(
                            String.format("Can't get value of the field \"%s\"", field.getName()),
                            e.getCause()
                    );
                }
                Byte clsFlagForPrimitive = flagsForPrimitives.get(fieldType);
                if (clsFlagForPrimitive == null) {
                    encode(fieldValue, fieldType,false);
                }
                else {
                    IConverter converter = converters.get(clsFlagForPrimitive);
                    byte[] fieldData = converter.getValue(fieldValue);
                    bodyStream.write(fieldData, 0, fieldData.length);
                }
            }
        }

        private void encodeArray(Object obj, boolean withArrayFlag) throws SerializationError {
            if (withArrayFlag)
                bodyStream.write(arrayFlagByte);

            if (obj == null) {
                bodyStream.write(isNullFlag);
                return;
            }
            IdentityKeyWrapper objWrapper = new IdentityKeyWrapper(obj);
            Byte objID = objectToID.get(objWrapper);
            if (objID != null) {
                bodyStream.write(isLinkFlag);
                bodyStream.write(objID);
                return;
            }

            objID = ++maxObjID;
            bodyStream.write(objID);
            objectToID.put(objWrapper, objID);

            Class<?> clazz = obj.getClass();
            Class<?> fieldType = clazz.getComponentType();

            Byte fieldTypeFlag = clsFlags.get(fieldType);
            if (fieldTypeFlag == null)
                fieldTypeFlag = (fieldType.isArray()) ? arrayFlagByte : addClsDescriptor(fieldType);
            bodyStream.write(fieldTypeFlag);

            int arrayLen = Array.getLength(obj);
            byte[] arrayLenBytes = ConverterToBytes.convertInt(arrayLen);
            bodyStream.write(arrayLenBytes, 0, arrayLenBytes.length);
            IConverter converter = converters.get(fieldTypeFlag);

            for (int i = 0; i < arrayLen; i++) {
                Object value = Array.get(obj, i);
                if (converter == null) {
                    encode(value, fieldType, false);
                }
                else {
                    byte[] data = converter.getValue(value);
                    bodyStream.write(data, 0, data.length);
                }
            }
        }

        private byte[] getResult() {
            int resBytesCount = header.length + (objIDLen & 0xff);
            for (ByteArrayOutputStream clsDescriptorStream : clsDescriptorStreams)
                resBytesCount += clsDescriptorStream.size();
            resBytesCount += bodyStream.size();
            byte[] result = new byte[resBytesCount];
            int curPointer = 0;
            System.arraycopy(header, 0, result, curPointer, header.length);
            curPointer += header.length;
//            result[curPointer] = objIDLen;
//            curPointer++;
            for (ByteArrayOutputStream clsDescriptor : clsDescriptorStreams) {
                System.arraycopy(clsDescriptor.toByteArray(), 0, result, curPointer, clsDescriptor.size());
                curPointer += clsDescriptor.size();
            }
            System.arraycopy(bodyStream.toByteArray(), 0, result, curPointer, bodyStream.size());
            return result;
        }
    }

    private static class Decoder extends Coder {
        private byte[] data;
        private int idx;
        private HashMap<Byte, Class<?>> classesForFlags;
        private HashMap<Byte, Object> idToObject;
        private static HashMap<Byte, IReader> readers;
        private interface IReader {
            Pair<?, Integer> getValue(byte[] data, int off);
        }

        static {
            readers = new HashMap<>();
            readers.put(flagsForPrimitives.get(Integer.TYPE), ConverterFromBytes::getInt);
            readers.put(flagsForPrimitives.get(Character.TYPE), ConverterFromBytes::getChar);
            readers.put(flagsForPrimitives.get(Byte.TYPE), ConverterFromBytes::getByte);
            readers.put(flagsForPrimitives.get(Short.TYPE), ConverterFromBytes::getShort);
            readers.put(flagsForPrimitives.get(Long.TYPE), ConverterFromBytes::getLong);
            readers.put(flagsForPrimitives.get(Boolean.TYPE), ConverterFromBytes::getBoolean);
            readers.put(flagsForPrimitives.get(Double.TYPE), ConverterFromBytes::getDouble);
            readers.put(flagsForPrimitives.get(Float.TYPE), ConverterFromBytes::getFloat);
            readers.put(flagsForPrimitives.get(String.class), ConverterFromBytes::getString);
        }

        private Decoder() {
            super();
            idToObject = new HashMap<>();
            classesForFlags = new HashMap<>();
            classesForFlags.put(flagsForPrimitives.get(Integer.TYPE), Integer.TYPE);
            classesForFlags.put(flagsForPrimitives.get(Character.TYPE), Character.TYPE);
            classesForFlags.put(flagsForPrimitives.get(Byte.TYPE), Byte.TYPE);
            classesForFlags.put(flagsForPrimitives.get(Short.TYPE), Short.TYPE);
            classesForFlags.put(flagsForPrimitives.get(Long.TYPE), Long.TYPE);
            classesForFlags.put(flagsForPrimitives.get(Boolean.TYPE), Boolean.TYPE);
            classesForFlags.put(flagsForPrimitives.get(Double.TYPE), Double.TYPE);
            classesForFlags.put(flagsForPrimitives.get(Float.TYPE), Float.TYPE);
            classesForFlags.put(flagsForPrimitives.get(String.class), String.class);
        }

        private byte getCurByte() {
            return data[idx];
        }

        private Object decode(byte[] data) throws DeserializationError {
            this.data = data;
            this.idx = 0;
            readHeader();
            while (getCurByte() == 0) {
                idx++;
                readClsDescriptor();
            }
            byte mainObjClsFlag = getCurByte();
            idx++;
            return decode(mainObjClsFlag);
        }

        private Object decode(Byte clsFlag) throws DeserializationError {
            IReader reader = readers.get(clsFlag);
            if (reader != null) {
                Pair<?, Integer> pairForMainObj = reader.getValue(data, idx);
                Object result = pairForMainObj.getKey();
                idx += pairForMainObj.getValue();
                return result;
            }
            else {
                byte objDescriptionByte = getCurByte();
                idx++;
                if (objDescriptionByte == isNullFlag)
                    return null;
                if (objDescriptionByte == isLinkFlag) {
                    byte linkObjID = getCurByte();
                    idx++;
                    return idToObject.get(linkObjID);
                }

                ClassDescriptor classDescriptor = clsDescriptors.get(clsFlag);
                if (classDescriptor == null) {
                    Byte arrayTypeFlag = getCurByte();
                    idx++;
                    return decodeArray(arrayTypeFlag, objDescriptionByte);
                }
                else {
                    return decodeObject(classDescriptor, objDescriptionByte);
                }
            }
        }

        private void readClsDescriptor() throws DeserializationError {
            byte clsFlag = getCurByte();
            idx++;
            Pair<String, Integer> pairForClsName = ConverterFromBytes.getString(data, idx);
            String clsName = pairForClsName.getKey();
            idx += pairForClsName.getValue();
            Class<?> clazz;
            try {
                clazz = Class.forName(clsName);
            } catch (ClassNotFoundException e) {
                throw new DeserializationError(
                        String.format("Can't find the class \"%s\"", clsName), e.getCause());
            }
            clsFlags.put(clazz, clsFlag);
            classesForFlags.put(clsFlag, clazz);
            Pair<Integer, Integer> pairForFieldsCount = ConverterFromBytes.getInt(data, idx);
            int fieldsCount = pairForFieldsCount.getKey();
            idx += pairForFieldsCount.getValue();
            ArrayList<Field> fields = new ArrayList<>();
            for (int i = 0; i < fieldsCount; i++) {
                Pair<String, Integer> pairForFieldName = ConverterFromBytes.getString(data, idx);
                String fieldName = pairForFieldName.getKey();
                idx += pairForFieldName.getValue();
                Field field;
                try {
                    field = clazz.getField(fieldName);
                } catch (NoSuchFieldException e) {
                    throw new DeserializationError(
                            String.format("The class \"%s\" doesn't have field \"%s\"", clsName, fieldName),
                            e.getCause()
                    );
                }
                byte fieldTypeFlag = getCurByte();  //TODO: save the flag and check type of deserializable objects
                idx++;
                fields.add(field);
            }
            ClassDescriptor classDescriptor = new ClassDescriptor(fields, clazz);
            clsDescriptors.put(clsFlag, classDescriptor);
        }

        private Object decodeArray(byte arrayTypeFlag, byte objID) throws DeserializationError {
            Pair<Integer, Integer> pairForArrayLen = ConverterFromBytes.getInt(data, idx);
            int arrayLen = pairForArrayLen.getKey();
            idx += pairForArrayLen.getValue();
            //TODO: add opportunity to work with array of arrays
            Class<?> arrayType = classesForFlags.get(arrayTypeFlag);
            Object result = Array.newInstance(arrayType, arrayLen);
            idToObject.put(objID, result);
            for (int i = 0; i < arrayLen; i++) {
                Array.set(result, i, decode(arrayTypeFlag));
            }
            return result;
        }

        private Object decodeObject(ClassDescriptor classDescriptor, byte objID) throws DeserializationError {
            Object result;
            try {
                Constructor<?> constructor = classDescriptor.clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                result = constructor.newInstance();
                idToObject.put(objID, result);
                for (Field field : classDescriptor.fields) {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    Byte fieldValueClsFlag = clsFlags.get(fieldType);
                    Object fieldValue = decode(fieldValueClsFlag);
                    field.set(result, fieldValue);
                }
            } catch (IllegalAccessException |
                    InstantiationException |
                    InvocationTargetException |
                    NoSuchMethodException e) {
                throw new DeserializationError(
                        String.format("\"%s\" is not serializable", classDescriptor.clazz.getName()),
                        e.getCause()
                );
            }
            return result;
        }

        private void readHeader() throws DeserializationError {
            if (data[0] != header[0] || data[1] != header[1])
                throw new DeserializationError("Can't read the marker");
            idx += 2;
            //TODO: read objIDLen
        }
    }
}
