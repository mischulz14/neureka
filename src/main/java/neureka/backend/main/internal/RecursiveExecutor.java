package neureka.backend.main.internal;

import neureka.Tsr;
import neureka.backend.api.ExecutionCall;
import neureka.devices.Device;

/**
 *  Resembles a grouped execution according to max arity of implementations.
 *  The {@link RecursiveExecutor} will process a chain of an arbitrary number of
 *  {@link ExecutionCall} arguments by being called recursively.
 *  This is useful for simple operators like '+' or '*' which
 *  might have any number of arguments...
 *  This can be implemented as a simple lambda.
 */
public interface RecursiveExecutor {

    Tsr<?> execute( ExecutionCall<? extends Device<?>> call, CallExecutor goDeeperWith );

}
