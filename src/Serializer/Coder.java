package Serializer;


import Serializer.utils.ClassDescriptor;

import java.util.HashMap;


abstract class Coder {
    protected static final byte[] header = new byte[]{22, 82};
    protected static final byte clsDescriptorByte = 0;
    protected static final byte arrayFlagByte = 1;
    protected static final byte isNullFlag = 0;
    protected static final byte isLinkFlag = 1;
    protected static final HashMap<Class<?>, Byte> flagsForPrimitives = new HashMap<>();
    protected static final int reservedBytesCount = 3 + 9;
    protected HashMap<Class<?>, Byte> clsFlags;
    protected HashMap<Byte, ClassDescriptor> clsDescriptors;
    
    static {
        byte curFlag = 1;
        flagsForPrimitives.put(Integer.TYPE, ++curFlag);
        flagsForPrimitives.put(Character.TYPE, ++curFlag);
        flagsForPrimitives.put(Byte.TYPE, ++curFlag);
        flagsForPrimitives.put(Short.TYPE, ++curFlag);
        flagsForPrimitives.put(Long.TYPE, ++curFlag);
        flagsForPrimitives.put(Boolean.TYPE, ++curFlag);
        flagsForPrimitives.put(Double.TYPE, ++curFlag);
        flagsForPrimitives.put(Float.TYPE, ++curFlag);
        flagsForPrimitives.put(String.class, ++curFlag);
    }

    protected Coder() {
        clsFlags = new HashMap<>(flagsForPrimitives);
        clsDescriptors = new HashMap<>();
    }
}
