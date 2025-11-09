package it.unipr.netsec.smqtt.gkd;

import java.io.IOException;


/** A Consumer that can throw a IOException.
 * @param <T> the type of the object that is consumed
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {
    void accept(T t) throws IOException;
}
