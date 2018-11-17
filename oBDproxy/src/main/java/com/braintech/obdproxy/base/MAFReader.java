/**
 * 
 */
package com.braintech.obdproxy.base;

import com.braintech.obdproxy.IResultReader;

/**
 * @author mapo
 *
 */
public class MAFReader extends OBDResponseReader implements IResultReader {

    private float maf = 0.0f;

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
            maf = getValue(input) / 100;
            res = String.format("%.0f", maf);
        }

        return res;
    }

}
