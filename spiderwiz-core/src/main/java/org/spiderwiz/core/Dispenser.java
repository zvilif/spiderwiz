package org.spiderwiz.core;

import org.spiderwiz.zutils.ZDispenser;

/**
 * Implement ZDispenser with an error notification.
 */
abstract class Dispenser<T> implements ZDispenser<T>{
    @Override
    public void handleException(Exception ex) {
        Main.getInstance().sendExceptionMail(ex, CoreConsts.EXCEPTION_IN_ZBUFFER, null, false);
    }

}
