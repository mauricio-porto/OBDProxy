/**
 * 
 */
package com.braintech.obdproxy.base;

import com.braintech.obdproxy.IResultReader;

/**
 * @author mapo
 *
 */
public class SpeedReader extends OBDResponseReader implements IResultReader {

    private int speed;

    /* (non-Javadoc)
     * @see IResultReader#readResult(byte[])
     */
    @Override
    public String readResult(byte[] input) {
        String res = new String(input);
        return res;
    }

    /* (non-Javadoc)
     * @see IResultReader#readFormattedResult(byte[])
     */
    @Override
    public String readFormattedResult(byte[] input) {
        String res = new String(input);
        
        if (!"NODATA".equals(res)) {
            speed = (int) getValue(input);
            res = String.format("%d", speed);
        }

        return res;
    }

}
