package org.spiderwiz.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a class as serializable by the <em>Spiderwiz</em> framework.
 * <p>
 * Use this annotation to mark a class (outer, inner or nested) as serializable by the framework.  A serializable class should contain
 * at least one field that is annotated as {@link org.spiderwiz.annotation.WizField WizField}.
 * <p>
 * Note that <em>Data Objects</em> (classes extending {@link org.spiderwiz.core.DataObject DataObject}) and classes annotated by
 * {@link javax.xml.bind.annotation.XmlType XmlType} annotation are implicitly serializable and therefore don't explicit
 * <em>WizSerializable</em> annotation.
 */
@Inherited
@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface WizSerializable {
}
