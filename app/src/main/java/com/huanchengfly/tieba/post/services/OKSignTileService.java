package com.huanchengfly.tieba.post.services;

import android.annotation.TargetApi;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import com.huanchengfly.tieba.post.utils.TiebaUtil;

@TargetApi(24)
public class OKSignTileService extends TileService {
    private static final String TAG = "OKSignTileService";

    @Override
    public void onStartListening() {
        super.onStartListening();
        if (getQsTile() == null) {
            return;
        }
        getQsTile().setState(Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        if (getQsTile() == null) {
            return;
        }
        getQsTile().setState(Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            TiebaUtil.startSign(this);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start sign from tile", t);
        }
    }
}
