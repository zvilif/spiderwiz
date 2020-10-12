package org.spiderwiz.websocket.server;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.spiderwiz.plugins.EndpointConsts;

/**
 * Filter requests on WEBSOCKET_URI to find out the remote IP address and store it in the session.
 * @author Zvi 
 */
@WebFilter(value=EndpointConsts.FILTER_URI)
public class WebsocketFilter implements javax.servlet.Filter{
    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
        HttpSession session = ((HttpServletRequest)sr).getSession();
        if (session != null)
            session.setAttribute(EndpointConsts.SESSION_ADDRESS, sr.getRemoteAddr());
        fc.doFilter(sr, sr1);
    }

    @Override
    public void destroy() {
    }

}
