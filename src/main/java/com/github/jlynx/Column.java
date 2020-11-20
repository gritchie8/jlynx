package com.github.jlynx;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    String value() default "";

    boolean include() default true;

    boolean pk() default false;

}
