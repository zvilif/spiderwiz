package org.spiderwiz.core;

import org.spiderwiz.zutils.ZBuffer;

/**
 * Manage all sort of asynchronous events
 * @author Zvi 
 */
class EventDispatcher extends Dispenser<EventDispatcher.Event> {
    abstract class Event {
        protected abstract void fire();
    }
    
    private class ObjectEvent extends Event {
        private final DataObject obj;
        private Integer seq;

        public ObjectEvent(DataObject object, Integer seq) {
            this.obj = object;
            this.seq = seq;
        }

        @Override
        protected void fire() {
            try {
                // If onAsyncEvent returns false don't acknowledge lossless objects
                if (!obj.isObsolete() && !obj.onAsyncEvent())
                    seq = null;
            } finally {
                if (seq != null)
                    obj.getDataChannel().sendAck(obj.getObjectCode(), Main.getInstance().getAppUUID(), obj.getOriginUUID(), seq);
            }
        }
    }

    /**
     * Handle delete or rename object events
     */
    private class ObsoleteObjectEvent extends Event {
        private final DataObject obj;
        private final Integer seq;

        public ObsoleteObjectEvent(DataObject object, Integer seq) {
            this.obj = object;
            this.seq = seq;
        }

        @Override
        protected void fire() {
            try {
                if (obj.getRename() != null) {
                    DataObject newObj = obj.getParent().getChild(obj.getObjectCode(), obj.getRename());
                    if (newObj != null)
                        newObj.onRename(obj.getObjectID());
                } else {
                    if (!obj.onRemoval())
                        obj.undelete();
                }
            } finally {
                if (seq != null)
                    obj.getDataChannel().sendAck(obj.getObjectCode(), Main.getInstance().getAppUUID(), obj.getOriginUUID(), seq);
            }
        }
    }

    private class ReseetObject extends Event {
        private final Resetter resetter;

        public ReseetObject(Resetter resetter) {
            this.resetter = resetter;
        }

        @Override
        protected void fire() {
            // Try first manual reset. If it returns false do automatic reset.
            if (!Main.getInstance().onObjectReset(resetter))
                DataManager.getInstance().getRootObject().resetObject(resetter);
        }
    }

    private class ResetCompleted extends Event {
        private final Resetter resetter;

        public ResetCompleted(Resetter resetter) {
            this.resetter = resetter;
        }

        @Override
        protected void fire() {
            Main.getInstance().onResetCompleted(resetter);
        }
    }

    private class QueryReply extends Event {

        private final QueryObject query;

        public QueryReply(QueryObject query) {
            this.query = query;
        }
        
        /**
         * Called from fire() and also from queryReply(). If called from the latter then there is no need to call releaseBusy
         */
        void reply() {
            try {
                switch(query.getQueryState()) {
                case REPLIED:
                    query.onReply();
                    break;
                case NEXT:
                    query.onReplyNext();
                    break;
                case END:
                    query.onReplyEnd();
                    break;
                }
            } finally {
                query.releaseWaitForReply();
            }
        }

        @Override
        protected void fire() {
            try {
                reply();
            } finally {
                query.releaseBusy();
            }
        }
    }

    private class QueryEnquire extends Event {
        private final QueryObject query;

        public QueryEnquire(QueryObject query) {
            this.query = query;
        }

        @Override
        protected void fire() {
            try {
                if (query.inquire())
                    query.reply(false);
            } catch (Exception ex) {
                Main.getInstance().sendExceptionMail(ex,
                    String.format(CoreConsts.AlertMail.WHEN_HANDLING_A_QUERY, query.getObjectCode()), null, false);
            }
        }
    }

    private final ZBuffer<Event> buffer;
    private boolean initialized = false;

    protected EventDispatcher(int threads) {
        if (threads == 0)
            buffer = null;
        else {
            buffer = new ZBuffer<>(this, threads);
            buffer.setTimeout(0);
            buffer.setMaxCapacity(200000);
        }
    }

    /**
     * Initialize the object of not done yet.
     */
    private synchronized void init() {
        if (!initialized) {
            initialized = true;
            if (buffer != null)
                buffer.execute();
        }
    }

    private boolean fireEvent(Event event, DataObject obj) {
        init();
        String objCode = obj == null ? null : obj.getObjectCode();
        DataHandler channel = obj == null ? null : obj.getDataChannel();
        if (buffer == null || channel != null && channel.isFileChannel()) {
            event.fire();
        } else if (!buffer.add(event)) {
            Main.getLogger().logEvent(CoreConsts.EVENT_BUFFER_FULL, objCode);
            return false;
        }
        return true;
    }

    /**
     * Fire an event on a newly received data object
     * @param obj       the object
     * @param seq       sequential number of the command carrying the serialized object (for acknowledging a lossless object)
     */
    void objectEvent(DataObject obj, Integer seq) {
        if (obj.isObsolete()) {
            fireEvent(new ObsoleteObjectEvent(obj, seq), obj);
            return;
        } else if (!obj.onEvent()) {
            // If synchronous event handling did not complete successfully proceed to handle it asynchronously.
            fireEvent(new ObjectEvent(obj, seq), obj);
            return;
        }
        if (seq != null)
            obj.getDataChannel().sendAck(obj.getObjectCode(), Main.getInstance().getAppUUID(), obj.getOriginUUID(), seq);
    }
    
    void objectReset(Resetter resetter) {
        fireEvent(new ReseetObject(resetter), null);
    }
    
    void resetCompleted(Resetter resetter) {
        fireEvent(new ResetCompleted(resetter), null);
    }
    
    boolean queryReply(QueryObject query, boolean doNow) {
        QueryReply qr = new QueryReply(query);
        if (doNow) {
            qr.reply();
            return true;
        }
        return fireEvent(qr, query);
    }
    
    void queryEnquire(QueryObject query) {
        fireEvent(new QueryEnquire(query), query);
    }

    void cleanup() {
        if (buffer != null)
            buffer.cleanup(false);
    }

    @Override
    public void dispense(Event task, boolean flush) {
        try {
            if (task != null) {
                task.fire();
            }
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex,null, null, false);
        }
    }
}
