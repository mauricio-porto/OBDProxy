### Summary

1. [Overview](#overview)
2. [OBD-II PIDs](#obd-ii-pids)
3. [UML Diagram](#uml-diagram)
4. [OBDCommand enumeration](#obdcommand)
5. [How to use](#how-to-use)

# Overview

OBDProxy is part of an OBD diagnostic application that queries a list (see *commandList*) of diagnostic parameters (see OBD-II PIDs below), such as engine temperature, oil pressure, etc. and showing them (or sending them remotely) at small intervals (typically every 2 seconds).

In the implemented version, the list of parameters is of fixed length, consisting of 7 types of information about the operation of the engine, but this list could be modified at any time during the execution of the application.

We have an *OBDConnector* class that sends such a list of queries to the OBD scanner via Bluetooth and receive and treat the responses.

# OBD-II PIDs

[OBD-II PIDs](http://en.wikipedia.org/wiki/OBD-II_PIDs) are the codes used to query the data of a vehicle, used in diagnostic tools.

These codes are classified into service types, as the above Wikipedia link shows. Here we will only deal with service 01 - Show current data.

The client program sends a read request by passing the service identifier and the parameter identifier (PID) it wishes.

The OBD scanner then responds with an encoded value that can have a variable number of bytes.

The response of each parameter (PID) is coded in order to optimize speed and memory usage, often requiring a formula to translate in meaningful values.
For example, the PID for the inlet air temperature (PID *0F*) returns only one byte and must have its value subtracted from 40, which will indicate the temperature in Celsius degrees.

As a result, we need to have a specific code to process and translate each of the parameters (PID) we want to use.

My solution was to concentrate the common behavior on an abstract class called *OBDResponseReader* and extend that class to each desired PID by implementing methods that translate the result read to the required meaningful values ​​and units.

The classes specialized in translating each parameter must implement 2 methods, called *readResult* and *readFormattedResult*, which return the raw read values ​​(in bytes) to the converted values ​​with their respective units. For example, RPM for motor rotation and °C for temperatures.

These 2 methods of translating the results are declared in the *IResultReader* interface that specialized classes must implement.

See the example for the *TemperatureReader* class:

```
/**
 * @author mauricio
 *
 */
public class TemperatureReader extends OBDResponseReader implements IResultReader {

    private float temperature = 0.0f;

    @Override
    public String readResult(byte[] input) {
        String res = new String(input);
        return res;
    }

    @Override
    public String readFormattedResult(byte[] input) {
        String res = new String(input);

        if (!"NODATA".equals(res)) {
            temperature = getValue(input) - 40;  // It ranges from -40 to 215 °C
            
            res = String.format("%.0f", temperature);
        }

        return res;
    }

}
```

# UML Diagram

The class diagram below shows the major components of the model designed to handle OBD objects in the OBDProxy application.
![UML Diagram](https://github.com/mauricio-porto/OBDProxy/blob/master/pictures/OBDProxy-UML.png "UML Diagram")

## OBDCommand

The central component is an enumeration that brings together all OBD commands. These commands are used to initialize the OBD scanner and to read the diagnostic data that the scanner collects.

The advantage of using an enumeration is primarily efficiency, as each new element of an enumeration is much lighter than a new class.

The constructor of this enumeration receives 4 arguments:

  - The name of the command;
  - The OBD code of the command;
  - A reference to a result translator, that is, a class that implements the interface *IResultReader* properly;
  - A mnemonic to be used as an identifier.
  
Here's an example:

```
  AMBIENT_AIR_TEMPERATURE("Ambient Air Temperature", "01 46", new TemperatureReader(), "ambTemp")
```

To add a new OBD parameter to be read, all the programmer needs to do is implement a result reader for this parameter and declare it's constructor as in the example above.

As an exercise, let's add the Throttle position parameter to the model.

Initially, we need to create the *ThrottlePositionReader* class that extends the *OBDResponseReader* and implements *IResultReader*.
Using your preferred IDE, the automatically generated class should resemble this:

```
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
       //TODO method stub
       return null;
    }

    @Override
    public String readFormattedResult(byte[] input) {
      //TODO method stub
      return null;
    }
}
```

Let's fill out the methods and do the conversion of the returned value.

According to the Wikipedia page on [OBD-II PIDs](https://en.wikipedia.org/wiki/OBD-II_PIDs), the throttle position is given in percent and the conversion of the read byte A is made by A * 100/255.

Thus the completed methods will be:

```
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
```

Finally, we need to add the constructor of this new parameter in the *OBDCommand* enumeration, like this:

```
THROTTLE_POSITION("Throttle posititon", "01 11", new ThrottlePositionReader(), "throtPos")
```

And voila, it's ready, just use it.

Easy to extend the model with new OBD parameters, right?

# How to use

As previously stated, the class that communicates with the OBD scanner is the *OBDConnector*.

To send the list of OBD commands to the scanner and pass the responses to the notification service, I created an instance of *Runnable* as follows:

```
private Runnable mQueueCommands = new Runnable() {
        public void run() {
            StringBuilder sb = new StringBuilder();
            sb.append('"').append("data").append('"').append(':').append('{');
            for (int i = 0; i < commandList.length; i++) {
                OBDCommand command = commandList[i];
                sb.append('"').append(command.getMnemo()).append('"').append(':').append('[');
                sb.append('"').append(command.getName()).append('"').append(',');
                String readParam = getOBDData(command);
                if (readParam != null) {
                    sb.append(readParam).append(']');
                } else {
                    sb.append(0).append(']');
                    Log.e(TAG, "\t\t " + command.getName() + " got null!!!");
                }
                if (i < commandList.length - 1) {
                    sb.append(',');
                }
            }
            sb.append('}');
            service.notifyDataReceived(sb.toString());
            // run again in 2s
            mHandler.postDelayed(mQueueCommands, 2000);
        }
    };
```

My list of *OBDCommand* named *commandList*:

```
OBDCommand[] commandList = {
            OBDCommand.ENGINE_RPM,
            OBDCommand.ENGINE_LOAD,
            OBDCommand.SPEED,
            OBDCommand.AMBIENT_AIR_TEMPERATURE,
            OBDCommand.COOLANT_TEMPERATURE,
            OBDCommand.INTAKE_AIR_TEMPERATURE,
            OBDCommand.MAF};

```

Note that for performance reasons, I used an _array_ for the *OBDCommand* used in this prototype, with a fixed length. But as said at the beginning, I could use a dynamic list, such as *ArrayList* for example.

Going back to inspecting the *Runnable* _mQueueCommands_, we note the use of the *getOBDData(command)* method to send a read request to the OBD scanner and return the response.

Here is its implementation:

```
private String getOBDData(OBDCommand param) {
        if (param == null) {
            return null;
        }

        byte[] data = sendToDevice(param.getOBDcode());
        if (data != null && data.length > 0) {
            IResultReader reader = param.getReader();
            if (reader != null) {
                return reader.readFormattedResult(data);
            }
        }
        return null;
    }
```


In this method, we send (thru _sendToDevice_) to the Bluetooth scanner the read code (_OBDCode_) associated with the parameter to be read and we receive the response in a _byte array_ that is checked for the null response. If it is not null, such a response is forwarded to the _readFormattedResult(data)_ method, which will then translate the response according to the expected format as described in the section [OBD-II PIDS](#obd-ii-pids).

Finally, in the execution loop of the *Runnable mQueueCommands*, the response obtained is converted and sent to the notification service by _service.notifyDataReceived()_ and a new run is rescheduled to 2 seconds.
