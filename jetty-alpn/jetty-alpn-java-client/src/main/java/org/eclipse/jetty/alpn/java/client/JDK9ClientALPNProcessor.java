//
//  ========================================================================
//  Copyright (c) 1995-2017 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.alpn.java.client;

import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.eclipse.jetty.alpn.client.ALPNClientConnection;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.ssl.ALPNProcessor;
import org.eclipse.jetty.io.ssl.SslConnection.DecryptedEndPoint;
import org.eclipse.jetty.io.ssl.SslHandshakeListener;
import org.eclipse.jetty.util.JavaVersion;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class JDK9ClientALPNProcessor implements ALPNProcessor.Client
{
    private static final Logger LOG = Log.getLogger(JDK9ClientALPNProcessor.class);

    @Override
    public void init(boolean debug)
    {
        if (JavaVersion.VERSION.getPlatform() < 9)
            throw new IllegalStateException(this + " not applicable for java "+JavaVersion.VERSION);
        if (debug)
            LOG.setDebugEnabled(true);
    }

    @Override
    public boolean appliesTo(SSLEngine sslEngine)
    {
        // TODO check the class name... make this more future proof!
        return sslEngine.getClass().getName().startsWith("sun.security.ssl.");
    }

    @Override
    public void configure(SSLEngine sslEngine, Connection connection)
    {
        ALPNClientConnection alpn = (ALPNClientConnection)connection;
        SSLParameters sslParameters = sslEngine.getSSLParameters();
        List<String> protocols = alpn.getProtocols();
        sslParameters.setApplicationProtocols(protocols.toArray(new String[protocols.size()]));
        sslEngine.setSSLParameters(sslParameters);

        ((DecryptedEndPoint)connection.getEndPoint()).getSslConnection()
                .addHandshakeListener(new ALPNListener(alpn));
    }


    private final class ALPNListener implements SslHandshakeListener
    {
        private final ALPNClientConnection alpnConnection;

        private ALPNListener(ALPNClientConnection connection)
        {
            alpnConnection = connection;
        }

        @Override
        public void handshakeSucceeded(Event event)
        {
            alpnConnection.selected(alpnConnection.getSSLEngine().getApplicationProtocol());
        }
    }

}
