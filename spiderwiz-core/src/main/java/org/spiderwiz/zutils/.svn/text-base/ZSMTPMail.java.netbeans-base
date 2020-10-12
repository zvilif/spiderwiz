package org.spiderwiz.zutils;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

/**
 * Implementation of {@link ZMail} for SMTP mail.
 * <p>
 * This implementation is invoked by <em>Spiderwiz</em> framework when {@code [mail&nbsp;system]} property in the
 * <a href="../core/doc-files/config.html">application's configuration file</a> includes the {@code smtp} parameter.
 * <p>
 * SMTP mail configuration example:
 * <p style='padding-left:2em;'>
 * {@code [mail system]smtp;server=smtp.gmail.com;user=zvilif@spiderwiz.org;pwd=dumptrump;port=465;ssl}
 */
public class ZSMTPMail implements ZMail {
    /**
     * Defines SMTP security protocols.
     */
    protected enum Mode {
        /**
         * No security protocol is used.
         */
        PLAIN,

        /**
         * SSL protocol.
         */
        SSL,

        /**
         * TLS protocol.
         */
        TLS
    }
    
    private static final String HTML_FORMAT = "text/html; charset=utf-8";
    private static final String TEXT_FORMAT = "text/plain; charset=utf-8";
    private static final String SMTP_SERVER = "server";
    private static final String SMTP_PORT = "port";
    private static final String SMTP_SSL = "ssl";
    private static final String SMTP_TLS = "tls";
    private static final String SMTP_USERNAME = "user";
    private static final String SMTP_PASSWORD = "pwd";

    private String smtpServer = null;
    private String smtpPort;
    private Mode smtpMode;
    private String username;
    private String password;

    /**
     * Configures SMTP mail using the given configuration string.
     * <p>
     * The configuration string is a list of parameters concatenated by a semicolon (;). A parameter is either a keyword or
     * an assignment of the form <em>key</em>=<em>value</em>. SMTP configuration parameters are:
     * <p>
     * {@code server=} the SMTP server to use.
     * <p>
     * {@code user=} username to login with.
     * <p>
     * {@code pwd=} password to login with.
     * <p>
     * {@code port=} server port to use.
     * <p>
     * {@code ssl} - include this keyword if SSL protocol shall be used.
     * <p>
     * {@code tls} - include this keyword if TLS protocol shall be used.
     * <p>
     * Example:
     * <p style='padding-left:2em;'>
     * {@code server=smtp.gmail.com;user=zvilif@spiderwiz.org;pwd=dumptrump;port=465;ssl}
     * @param configString the configuration string.
     */
    @Override
    public synchronized void configure(String configString) {
        smtpServer = null;
        if (configString == null)
            return;
        ZDictionary configParams = ZDictionary.parseParameterList(configString);
        smtpServer = configParams.get(SMTP_SERVER);
        smtpPort = configParams.get(SMTP_PORT);
        smtpMode = configParams.containsKey(SMTP_SSL) ? Mode.SSL
                : configParams.containsKey(SMTP_TLS) ? Mode.TLS : Mode.PLAIN;
        username = configParams.get(SMTP_USERNAME);
        password = configParams.get(SMTP_PASSWORD);
    }

    /**
     * Sends a message via an SMTP server.
     * @param from          the mail address of the sender.
     * @param to            the mail address(es) of the addressee(s). Multiple addresses shall be concatenated by a comma(,) or
     *                      a semicolon(;).
     * @param cc            if not null, sends a carbon copy to the specified address(es). Multiple addresses shall be
     *                      concatenated by a comma(,) or a semicolon(;).
     * @param subject       the mail subject or null if there is no subject.
     * @param body          the mail body or null if there is no body.
     * @param html          true if the message shall be sent in HTML format, false if it shall be sent as plain text.
     * @param highPriority  true if the message shall be flagged as high priority.
     * @throws javax.mail.MessagingException
     * @throws java.io.UnsupportedEncodingException
     */
    @Override
    public synchronized void send(String from, String to, String cc, String subject, String body, boolean html,
            boolean highPriority) throws MessagingException, UnsupportedEncodingException {
        if (smtpServer == null)
            return;
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpServer);
        switch (smtpMode) {
            case SSL:
                props.put("mail.smtp.socketFactory.port", smtpPort);
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.auth", "true");
                break;
            case TLS:
                props.put("mail.smtp.starttls.enable", "true");
                break;
        }
        props.put("mail.smtp.port", smtpPort);

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        if (cc != null) {
            msg.addRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
        }
        msg.setSubject(MimeUtility.encodeText(subject, "utf-8", "B"));
        msg.setContent(body, html ? HTML_FORMAT : TEXT_FORMAT);
        if (highPriority) {
            msg.setHeader("X-Priority", "1");
        }
        Transport.send(msg);
    }

    /**
     * Gets the configured SMTP server.
     * @return the configured SMTP server
     */
    protected String getSmtpServer() {
        return smtpServer;
    }

    /**
     * Configures an SMTP server
     * @param smtpServer SMTP server name.
     */
    protected void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    /**
     * Gets the configured SMTP port number.
     * @return the configured SMTP port number
     */
    protected String getSmtpPort() {
        return smtpPort;
    }

    /**
     * Configures an SMTP port number.
     * @param smtpPort SMTP port number.
     */
    protected void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    /**
     * Gets the configured SMTP security mode.
     * @return {@link Mode#PLAIN PLAIN} for no security, {@link Mode#SSL SSL} or {@link Mode#TLS TLS}.
     */
    protected Mode getSmtpMode() {
        return smtpMode;
    }

    /**
     * Configures SMTP security mode.
     * @param smtpMode {@link Mode#PLAIN PLAIN} for no security, {@link Mode#SSL SSL} or {@link Mode#TLS TLS}.
     */
    protected void setSmtpMode(Mode smtpMode) {
        this.smtpMode = smtpMode;
    }

    /**
     * Gets the configured username to login to the SMTP server with.
     * @return the configured username to login to the SMTP server with.
     */
    protected String getUsername() {
        return username;
    }

    /**
     * Configures the username to login to the SMTP server with.
     * @param username the username to login to the SMTP server with.
     */
    protected void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the configured password to login to the SMTP server with.
     * @return the configured password to login to the SMTP server with.
     */
    protected String getPassword() {
        return password;
    }

    /**
     * Configures the password to login to the SMTP server with.
     * @param password the password to login to the SMTP server with.
     */
    protected void setPassword(String password) {
        this.password = password;
    }
}
