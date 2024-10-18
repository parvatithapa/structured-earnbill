package com.sapienter.jbilling.server;

import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Created by dario on 10/21/16.
 */
public class LoggingValidator {

    public static void validateEnhancedLog(String logFile, String priorityLevel, String callerClass, String apiMethod, LogConstants module,
                                           LogConstants status, LogConstants action, String message) {

        assertNotNull("Jbilling log file is empty!!!", logFile);
        String[] logLines = logFile.split("\\n");
        boolean validLog = false;
        for (String logLine : logLines) {
            boolean validLine = logLine.contains(priorityLevel) //valid priority
                    && logLine.contains(callerClass) //valid class
                    && logLine.contains("juser=\"1\", jcompany=\"1\", ip=\"127.0.0.1\"") // valid user, company and ip
                    && logFile.contains(apiMethod) //valid API method
                    && logLine.contains(module.toString()) // valid module
                    && logLine.contains(status.toString()) // valid status
                    && logLine.contains(action.toString()) //valid action
                    && logLine.contains(message); //valid message

            if (validLine) {
                validLog = true;
                break;
            }
        }
        String errorMessage = "priorityLevel=" + priorityLevel +
                ", callerClass=" + callerClass +
                ", juser=\"1\", jcompany=\"1\", ip=\"127.0.0.1\"" +
                ", apiMethod=" + apiMethod +
                ", module=" + module.toString() +
                ", status=" + status.toString() +
                ", action=" + action.toString() +
                ", message=" + message;

        assertTrue(errorMessage, validLog);
    }
}
