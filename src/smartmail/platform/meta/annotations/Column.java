package smartmail.platform.meta.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.FIELD})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name() default "";

    boolean autoincrement() default false;

    boolean primary() default false;

    String type() default "integer";

    boolean nullable() default false;

    int length() default 0;
}
