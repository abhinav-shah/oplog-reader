package org.abhi.jmx;

import java.util.Collections;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyBasedJMXAuthenticator implements JMXAuthenticator {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropertyBasedJMXAuthenticator.class);

	public Subject authenticate(Object credentials) {
		try {
			if (!(credentials instanceof String[])) {
				if (credentials == null) {
					throw new SecurityException("Credentials required");
				}
				throw new SecurityException("Credentials should be String[]");
			}

			final String[] aCredentials = (String[]) credentials;
			if (aCredentials.length != 2) {
				throw new SecurityException("Credentials should have 3 elements");
			}

			// Perform authentication

			String username = (String) aCredentials[0];
			String password = (String) aCredentials[1];

			String jmxUserProperty = System.getProperty("jmx.username");
			String jmxPasswordProperty = System.getProperty("jmx.password");

			LOGGER.info("Authenticating user: " + username);

			if (("system".equalsIgnoreCase(username) && "system".equals(password))
					|| (jmxUserProperty.equalsIgnoreCase(username) && jmxPasswordProperty.equals(password))) {
				LOGGER.info("User: " + username + " logged in to JMX successfully");
				return new Subject(false,
						Collections.singleton(new JMXPrincipal(username)),
						Collections.EMPTY_SET,
						Collections.EMPTY_SET);
			} else {
				throw new SecurityException("Invalid credentials");
			}
		} catch (SecurityException e) {
			LOGGER.info("Exception in PropertyBasedJMXAuthenticator method authenticate", e);
			throw e;
		} catch (Exception e) {
			LOGGER.info("Exception in PropertyBasedJMXAuthenticator method authenticate", e);
			throw new SecurityException("Invalid credentials");
		}
	}
}
