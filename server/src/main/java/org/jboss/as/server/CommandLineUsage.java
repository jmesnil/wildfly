package org.jboss.as.server;

import static org.jboss.as.server.ServerMessages.MESSAGES;

import java.io.PrintStream;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.version.Usage;

public class CommandLineUsage {

    private static Usage getUsage() {

        final Usage usage = new Usage();

        usage.addArguments(CommandLineConstants.ADMIN_ONLY);
        usage.addInstruction(MESSAGES.argAdminOnly());

        usage.addArguments(CommandLineConstants.PUBLIC_BIND_ADDRESS + " <value>", CommandLineConstants.PUBLIC_BIND_ADDRESS + "=<value>");
        usage.addInstruction(MESSAGES.argPublicBindAddress());

        usage.addArguments(CommandLineConstants.PUBLIC_BIND_ADDRESS + "<interface>=<value>");
        usage.addInstruction(MESSAGES.argInterfaceBindAddress());

        usage.addArguments(CommandLineConstants.SHORT_SERVER_CONFIG + " <config>", CommandLineConstants.SHORT_SERVER_CONFIG + "=<config>");
        usage.addInstruction(MESSAGES.argShortServerConfig());

        usage.addArguments(CommandLineConstants.DEBUG + " [<port>]");
        usage.addInstruction(MESSAGES.argDebugPort());

        usage.addArguments(CommandLineConstants.SYS_PROP + "<name>[=<value>]");
        usage.addInstruction(MESSAGES.argSystem());

        usage.addArguments(CommandLineConstants.SHORT_HELP, CommandLineConstants.HELP);
        usage.addInstruction(MESSAGES.argHelp());

        usage.addArguments(CommandLineConstants.READ_ONLY_SERVER_CONFIG + "=<config>");
        usage.addInstruction(MESSAGES.argReadOnlyServerConfig());

        usage.addArguments(CommandLineConstants.SHORT_PROPERTIES + " <url>", CommandLineConstants.SHORT_PROPERTIES + "=<url>", CommandLineConstants.PROPERTIES + "=<url>");
        usage.addInstruction(MESSAGES.argProperties());

        usage.addArguments(CommandLineConstants.SECURITY_PROP + "<name>[=<value>]");
        usage.addInstruction(MESSAGES.argSecurityProperty());

        usage.addArguments(CommandLineConstants.SERVER_CONFIG + "=<config>");
        usage.addInstruction(MESSAGES.argServerConfig());

        usage.addArguments(CommandLineConstants.DEFAULT_MULTICAST_ADDRESS + " <value>", CommandLineConstants.DEFAULT_MULTICAST_ADDRESS + "=<value>");
        usage.addInstruction(MESSAGES.argDefaultMulticastAddress());

        usage.addArguments(CommandLineConstants.SHORT_VERSION, CommandLineConstants.OLD_SHORT_VERSION, CommandLineConstants.VERSION);
        usage.addInstruction(MESSAGES.argVersion());

        return usage;
    }

    public static void printUsage(final PrintStream out) {
        final Usage usage = getUsage();
        final String headline = usage.getDefaultUsageHeadline("standalone");
        out.print(usage.usage(headline));
    }

}
