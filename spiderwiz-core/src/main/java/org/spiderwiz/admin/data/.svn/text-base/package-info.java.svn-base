/**
 * Classes used by <a href="http://spideradmin.com">SpiderAdmin</a>.
 * <p>
 * The <em><b>SpiderAdmin</b></em> service uses an agent (the {@code spider-admin} dependency) to exchange data with the applications
 * that it governs. This package contains the classes that are used in this exchange. The basic SpiderAdmin function does not require
 * any user coding (except including the dependency in the project) and will use these classes transparently. However if you want
 * to customize the application's SpiderAdmin page you would find here what you need.
 * <p>
 * SpiderAdmin customization is done by overriding related methods of
 * <a href=../../core/Main.html#customspideradmin>class Main</a>.
 * If you do that then the method that you will most frequently override
 * {@link org.spiderwiz.core.Main#getPageInfo(java.lang.String) Main.getPageInfo()}
 * and {@link org.spiderwiz.core.Main#customAdminService(java.lang.String, java.lang.String) Main.customAdminService()},
 * both of them use classes defined in this package. The first returns an object of type {@link PageInfo} that
 * specifies the layout the application page. By hooking into this object you can use
 * {@link PageInfo#addButton(java.lang.String, java.lang.String, boolean) addButton()} to add custom operation buttons at the top of
 * the page, and {@link PageInfo#addTable(org.spiderwiz.admin.data.PageInfo.TableInfo) addTable()} to add custom tables.
 * <p>
 * Having added custom buttons and tables, you would need override
 * {@link org.spiderwiz.core.Main#customAdminService(java.lang.String, java.lang.String) Main.customAdminService()}
 * in order to handle button operations and provide data for the new tables. You would return an object of type
 * {@link OpResults} if you do the first and {@link TableData} for the second.
 * <p>
 * For details see the class descriptions in this package.
 */
package org.spiderwiz.admin.data;
