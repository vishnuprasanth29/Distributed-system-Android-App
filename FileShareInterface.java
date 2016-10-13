package com.Project258;
/**
 * Created by Raj on 4/30/2016.
 */
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface (name = "com.Project258")
public interface FileShareInterface {
    /*
     * The BusSignal annotation signifies that this function should be used as
     * part of the AllJoyn interface.  The runtime is smart enough to figure
     * out that this is a used as a signal emitter and is only called to send
     * signals and not to receive signals.
     */
    @BusSignal
    public void Chat(String str) throws BusException;
}

