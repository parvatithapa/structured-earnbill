package com.sapienter.jbilling.server.provisioning.task;

import com.sapienter.jbilling.client.mcf.MCFServiceBL;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.provisioning.IRequestProvisioningCallback;
import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Created by pablo_galera on 06/02/17.
 */
public class MCFExternalProvisioningTask extends PluggableTask implements IExternalProvisioning {

    public static final String PARAM_ID_DEFAULT = "mcfservice";
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(MCFExternalProvisioningTask.class));
    private static final String METHOD_NAME = "METHOD_NAME";
    public static final ParameterDescription PARAMETER_MCF_REQUEST_URL =
            new ParameterDescription("MCF Request URL", true, ParameterDescription.Type.STR, false);

    // Initializer for pluggable params
    {
        descriptions.add(PARAMETER_MCF_REQUEST_URL);
    }

    @Override
    public String sendRequest(String id, String command, IRequestProvisioningCallback callback) throws TaskException {
        parseCommand(command).forEach( (k,v) -> LOG.info(k + " " + v));
        MCFServiceBL mcfServiceBL = new MCFServiceBL(parameters.get(PARAMETER_MCF_REQUEST_URL.getName()));
        String result = mcfServiceBL.sendADDACCandADDBILLCommands(parseCommand(command));
        return result;
    }

    @Override
    public String getId() {
        return PARAM_ID_DEFAULT;
    }

    private Map<String, String> parseCommand(String command)
            throws TaskException {

        LOG.info("parsing command string pattern: " + command);
        Map<String, String> params = new LinkedHashMap<String, String>();
        StringTokenizer st = new StringTokenizer(command, ":;");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            String[] entry = token.split(",");
            if (entry.length > 2)
                throw new TaskException(
                        "Error parsing command: Expected two Tokens but found too many tokens: "
                                + token);
            else if (entry.length == 1) {
                // found method name
                params.put(METHOD_NAME, entry[0]);
            } else {
                params.put(entry[0], entry[1]);
            }
        }
        return params;
    }
}
