package org.spiderwiz.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies the class as a <b>Query Object</b> in the <em>Spiderwiz</em> framework.
 * <p>
 * This annotation is defined for {@link org.spiderwiz.core.QueryObject QueryObject} class and inherited by its subclasses.
 * It may be of benefit to developers of code generators for the Spiderwiz environment.
 */
@Inherited
@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface WizQuery {

}
