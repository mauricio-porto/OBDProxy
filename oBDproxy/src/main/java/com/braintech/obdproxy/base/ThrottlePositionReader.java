package com.braintech.obdproxy.base;

import com.braintech.obdproxy.IResultReader;

/**
 * @author mauricio
 *
 */
public class ThrottlePositionReader extends OBDResponseReader implements IResultReader {

    private int position;

    @Override
    public String readResult(byte[] input) {
        String res = new String(input);
        return res;
    }

    @Override
    public String readFormattedResult(byte[] input) {
        String res = new String(input);

        if (!"NODATA".equals(res)) {
            position = (int) getValue(input) * 100 / 255;

            res = String.format("%d", position);
        }
        return res;
    }
}
