package org.spiderwiz.core;

import org.spiderwiz.zutils.ZModerator;

/**
 * Moderates action rate according to value in settings.dat. Usually used for moderation of transmission over the network
 * @author @author  zvil
 */
class TransmitModerator extends ZModerator{
    private int explicitRate = -1;

    public void setExplicitRate(int explicitRate) {
        this.explicitRate = explicitRate;
    }
    
    @Override
    protected int getRate() {
        return explicitRate < 0 ? Main.getMyConfig().getRefreshTranmsitRate() : explicitRate;
    }
}
