package smartmail.platform.utils;

import org.apache.commons.lang.ArrayUtils;
import smartmail.platform.exceptions.SystemException;
import smartmail.platform.logging.Logger;
import smartmail.platform.meta.annotations.Column;

import java.lang.reflect.Field;

public class Inspector {
    public static String[] classFields(Object object) {
        String[] fields = new String[0];
        if (object != null)
            for (Field field : object.getClass().getDeclaredFields())
                fields = (String[])ArrayUtils.add((Object[])fields, field.getName());
        return fields;
    }

    public static Column columnMeta(Object object, String columnName) {
        if (object != null)
            try {
                Field field = object.getClass().getDeclaredField(columnName);
                Column[] annotations = field.<Column>getAnnotationsByType(Column.class);
                if (annotations.length > 0)
                    return annotations[0];
            } catch (Exception e) {
                Logger.error((Exception)new SystemException(e), Inspector.class);
            }
        return null;
    }
}
