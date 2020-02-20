package org.spiderwiz.core;

import org.spiderwiz.zutils.ZBuffer;
import org.spiderwiz.zutils.ZLog;

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

    private class ReseetObject extends Event {
        private final Resetter resetter;

        public ReseetObject(Resetter resetter) {
            this.resetter = resetter;
        }

        @Override
        protected void fire() {
            Main.getInstance().onObjectReset(resetter);
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

    private static EventDispatcher myObject = null;
    private final ZBuffer<Event> buffer;
    private ZLog logger;

    protected EventDispatcher() {
        buffer = new ZBuffer<>(this, true);
        buffer.setTimeout(0);
        buffer.setMaxCapacity(200000);
    }

    static EventDispatcher getInstance() {
        return myObject;
    }
    
    void init() {
        myObject = this;
        logger = Main.getLogger();
        buffer.execute();
    }

    private boolean fireEvent(Event event, DataObject obj) {
        String objCode = obj == null ? null : obj.getObjectCode();
        DataHandler channel = obj == null ? null : obj.getDataChannel();
        if (channel != null && channel.isFileChannel()) {
            event.fire();
        } else if (!buffer.add(event)) {
            logger.logEvent(CoreConsts.EVENT_BUFFER_FULL, objCode);
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
            if (obj.getRename() != null) {
                DataObject newObj = obj.getParent().getChild(obj.getObjectCode(), obj.getRename());
                if (newObj != null)
                    newObj.onRename(obj.getObjectID());
            } else
                obj.onRemoval();
        } else if (!obj.onEvent()) {
            // If synchronous event handling did not complete successfully proceed to handle it asynchronously.
            fireEvent(new ObjectEvent(obj, seq), obj);
            seq = null;
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
