package org.dspace.authority.orcid;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz at OR13 dev challenge
 */
public class LookupException extends Exception {

    public LookupException(String message) {
        super(message);
    }

    public LookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
