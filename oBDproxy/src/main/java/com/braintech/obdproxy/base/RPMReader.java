/**
 * 
 */
package com.braintech.obdproxy.base;

import com.braintech.obdproxy.IResultReader;

/**
 * @author mapo
 *
 */
public class RPMReader extends OBDResponseReader implements IResultReader {

    private int rpm;

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
            rpm = (int) getValue(input) / 4;
            res = String.format("%d", rpm);
        }

        return res;
    }

}
