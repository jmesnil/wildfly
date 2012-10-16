package org.jboss.as.appclient.subsystem;

import static org.jboss.as.appclient.logging.AppClientMessages.MESSAGES;

import java.io.PrintStream;

import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.version.Usage;

public class CommandLineUsage {

    private static Usage getUsage() {
        final Usage usage = new Usage();
        usage.addArguments(CommandLineConstants.APPCLIENT_CONFIG + "=<config>");
        usage.addInstruction(MESSAGES.argAppClientConfig());

        usage.addArguments(CommandLineConstants.SHORT_HELP, CommandLineConstants.HELP);
        usage.addInstruction(MESSAGES.argHelp());

        usage.addArguments(CommandLineConstants.HOST + "=<url>", CommandLineConstants.SHORT_HOST + "=<url>");
        usage.addInstruction(MESSAGES.argHost());

        usage.addArguments(CommandLineConstants.SHORT_PROPERTIES + "=<url>", CommandLineConstants.PROPERTIES + "=<url>");
        usage.addInstruction(MESSAGES.argProperties());

        usage.addArguments(CommandLineConstants.CONNECTION_PROPERTIES + "=<url>");
        usage.addInstruction(MESSAGES.argConnectionProperties());

        usage.addArguments(CommandLineConstants.SYS_PROP + "<name>[=value]");
        usage.addInstruction(MESSAGES.argSystemProperty());

        usage.addArguments(CommandLineConstants.SHORT_VERSION, CommandLineConstants.VERSION);
        usage.addInstruction(MESSAGES.argVersion());

        return usage;
    }

    public static void printUsage(final PrintStream out) {
        final Usage usage = getUsage();
        final String headline = usage.getDefaultUsageHeadline("appclient");
        out.print(usage.usage(headline));
    }
}
