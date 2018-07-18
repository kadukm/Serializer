package Serializer.utils;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;


public class ClassDescriptor {
    public final ArrayList<Field> fields;
    public final Class<?> clazz;

    public ClassDescriptor(ArrayList<Field> fields, Class<?> clazz) {
        this.fields = fields;
        this.clazz = clazz;
    }

    public static ClassDescriptor getDescriptorFor(Class<?> clazz) {
        ArrayList<Field> fieldsList = new ArrayList<>();
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            Field[] fields = currentClass.getDeclaredFields();
            fieldsList.addAll(Arrays.asList(fields));
            currentClass = currentClass.getSuperclass();
        }

        fieldsList.removeIf(field -> Modifier.isStatic(field.getModifiers()) ||
                Modifier.isTransient(field.getModifiers()) ||
                field.getName().startsWith("class$"));
        ClassDescriptor classDescriptor = new ClassDescriptor(fieldsList, clazz);
        return classDescriptor;
    }
}
