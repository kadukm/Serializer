package Serializer;


import Serializer.utils.Errors.DeserializationError;
import Serializer.utils.Errors.SerializationError;

import java.util.*;


public class Main {
    public static void main(String[] args) {
        testSerialization();
    }

    private static void testSerialization() {
        Test test1 = new Test(null, "one");
        Test test2 = new Test(new byte[] {1,2,3}, "two");
        test1.bro = test2;
        test2.bro = test1;
        try {
            byte[] data = Serializer.dumps(test1);
            Object res = Serializer.loads(data);
            System.out.println(test1.equals(res));
        } catch (SerializationError | DeserializationError error) {
            error.printStackTrace();
        }
    }

    private static class Test {
        public String sTest;
        public byte[] bsTest;
        public Test bro;

        private Test() {  }

        public Test(byte[] bs, String s) {
            sTest = s;
            bsTest = bs;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Test)) return false;
            Test testObj = (Test)obj;
            return (sTest.equals(testObj.sTest) &&
                    Arrays.equals(bsTest, testObj.bsTest)
            );
        }
    }
}

