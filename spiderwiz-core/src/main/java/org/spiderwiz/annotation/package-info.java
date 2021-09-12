/**
 * Annotation classes used by the <em>Spiderwiz</em> framework.
 * <p>
 * Spiderwiz uses annotation tags to carry on its work. Some of the tags, namely {@link org.spiderwiz.annotation.WizMain WizMain},
 * {@link org.spiderwiz.annotation.WizObject WizObject} and {@link org.spiderwiz.annotation.WizQuery WizQuery}, decorate the
 * building blocks classes of the framework - {@link org.spiderwiz.core.Main Main}, {@link org.spiderwiz.core.DataObject DataObject}
 * and {@link org.spiderwiz.core.QueryObject QueryObject} respectively. These tags are used internally by the framework and are
 * inherited by subclasses of the annotated classes. The other tags, namely {@link org.spiderwiz.annotation.WizField WizField} and
 * {@link org.spiderwiz.annotation.WizSerializable WizSerializable}, are used by the user to provide serialization hints to the
 * framework.
 */
package org.spiderwiz.annotation;
