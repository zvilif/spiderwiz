package org.spiderwiz.zutils;

/**
 * An interface for mail sending implementations.
 * <p>
 * Implementations of this interface must implement the {@link #configure(java.lang.String) configure()} method to configure the mail
 * system and the
 * {@link #send(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, boolean) send()}
 * method to send a message.
 * <p>
 * <em>Spiderwiz</em> comes out of the box with an {@link ZSMTPMail SMTP imlementation}. You can write your own plug-in implementation
 * that will be activated by the framework with the {@code class=} parameter of {@code [mail system]} property in the
 * <a href="../core/doc-files/config.html">application's configuration file</a>.
 * @see ZSMTPMail
 */
public interface ZMail {

    /**
     * Configures the mail system using the given configuration string.
     * <p>
     * When called from <em>Spiderwiz</em> framework the configuration string is the value of {@code [mail&nbsp;system]} property
     * in the <a href="../core/doc-files/config.html">application's configuration file</a>.
     * @param configString  the configuration string.
     */
    public void configure(String configString);
    
    /**
     * Sends a mail according to the given parameters.
     * @param from          the mail address of the sender.
     * @param to            the mail address(es) of the addressee(s).
     * @param cc            if not null, sends a carbon copy to the specified address(es).
     * @param subject       the mail subject or null if there is no subject.
     * @param body          the mail body or null if there is no body.
     * @param html          true if the message shall be sent in HTML format, false if it shall be sent as plain text.
     * @param highPriority  true if the message shall be flagged as high priority.
     * @throws Exception
     */
    public void send(String from, String to, String cc, String subject, String body, boolean html, boolean highPriority)
        throws Exception;
}
