package com.hxh.robusttest;

import android.util.Log;

import com.meituan.robust.Patch;
import com.meituan.robust.RobustCallBack;

import java.util.List;

/**
 * Created by HXH at 2019/3/7
 * RobustCallBack
 */
public class RobustCallBackSample implements RobustCallBack {


    /*@Override
    public void onPatchListFetched(boolean result, boolean isNet) {
        Log.i("RobustCallBack", "onPatchListFetched result: " + result);
    }*/

    @Override
    public void onPatchListFetched(boolean result, boolean isNet, List<Patch> patches) {
        Log.i("RobustCallBack", "onPatchListFetched result: " + result);
    }

    @Override
    public void onPatchFetched(boolean result, boolean isNet, Patch patch) {
        Log.i("RobustCallBack", "onPatchFetched result: " + result);
        Log.i("RobustCallBack", "onPatchFetched isNet: " + isNet);
        Log.i("RobustCallBack", "onPatchFetched patch: " + patch.getName());
    }

    @Override
    public void onPatchApplied(boolean result, Patch patch) {
        Log.i("RobustCallBack", "onPatchApplied result: " + result);
        Log.i("RobustCallBack", "onPatchApplied patch: " + patch.getName());

    }

    @Override
    public void logNotify(String log, String where) {
        Log.i("RobustCallBack", "logNotify log: " + log);
        Log.i("RobustCallBack", "logNotify where: " + where);
    }

    @Override
    public void exceptionNotify(Throwable throwable, String where) {
        Log.e("RobustCallBack", "exceptionNotify where: " + where, throwable);
    }
}
