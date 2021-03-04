package Solution;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
//The Target annotation specifies which elements of our code can have annotations of the defined type
//Lastly, if we use the RUNTIME identifier,
// not only is the annotation recorded in the .class file, but it is also made available to the runtime by the JVM.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Then {
    String value() default "";
}