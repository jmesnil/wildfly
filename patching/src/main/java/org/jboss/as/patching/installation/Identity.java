package org.jboss.as.patching.installation;

/**
 *
 * An identity is a named set of distribution base + layered distribution(s) that is certified as a valid combination.
 *
 * @author Emanuel Muckenhuber
 */
public interface Identity extends PatchableTarget {

    /**
     * Get the identity name.
     *
     * @return the identity name
     */
    String getName();

    /**
     * Get the identity version.
     *
     * @return the identity version
     */
    String getVersion();

}
