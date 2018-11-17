/**
 * 
 */
package com.braintech.obdproxy;

/**
 * @author mauricio
 *
 */
public interface IResultReader {
    public String readResult(byte[] input);
    public String readFormattedResult(byte[] input);
}
