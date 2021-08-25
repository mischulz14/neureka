/*
MIT License

Copyright (c) 2019 Gleethos

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*/

package neureka.calculus;

import neureka.Tsr;
import neureka.autograd.GraphLock;
import neureka.autograd.GraphNode;
import neureka.backend.api.OperationContext;
import neureka.calculus.args.Arg;
import neureka.calculus.args.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

/**
 *  This class is part of a given {@link OperationContext} instance
 *  responsible for caching {@link Function} references based on
 *  their {@link String} representation generated by {@link Object#toString()}
 *  as well as caching of results for active functions.
 */
public final class Cache
{
    private static Cache _cache = new Cache();
    private Logger _log = LoggerFactory.getLogger( Cache.class );

    public static Cache instance()
    {
        Cache c = _cache;
        _cache = null;
        return c;
    }

    private final Map<String, Function> functionCache = Collections.synchronizedMap( new WeakHashMap<>() );

    public synchronized Map<String, Function> functions() {
        return this.functionCache;
    }

}
