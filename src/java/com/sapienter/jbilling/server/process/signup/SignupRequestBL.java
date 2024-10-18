package com.sapienter.jbilling.server.process.signup;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;

public class SignupRequestBL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String FIND_CATEGORY_ID_SQL =
            "SELECT id FROM pluggable_task_type_category WHERE interface_name = 'com.sapienter.jbilling.server.process.signup.ISignupProcessTask' ";

    private SignupPlaceHolder signupPlaceHolder;

    public SignupRequestBL(SignupPlaceHolder signupPlaceHolder) {
        this.signupPlaceHolder = signupPlaceHolder;
    }

    /**
     * Delegate Request to configured {@link ISignupProcessTask} for entity
     * @return
     */
    public SignupResponseWS processSignUpRequest() {
        logger.debug("Processing Sign Up Request {}", signupPlaceHolder.getSignUpRequest());
        SignupResponseWS response = signupPlaceHolder.getSignUpResponse();
        try {
            PluggableTaskManager<ISignupProcessTask> taskManager = new PluggableTaskManager<>(signupPlaceHolder.getEntityId(), getCategoryId());
            ISignupProcessTask task = taskManager.getNextClass();
            while(null!= task) {
                task.validateSignupRequest(signupPlaceHolder);
                if(response.hasError()) {
                    return response;
                }
                logger.debug("Task {} is processing singup request for entity {}", task.getClass().getCanonicalName(), signupPlaceHolder.getEntityId());
                task.processSignupRequest(signupPlaceHolder);
                task = taskManager.getNextClass(); // fetch next task.
            }
        } catch (Exception e) {
            logger.error("SignupProcessTask failed!", e);
            response.addErrorResponse("SIGNUP-PROCESS-TASK-EXCECUTION-FAILED");
        }
        if(response.hasError()) {
            response.resetResponse();
        }
        logger.debug("Processed Sign Up Response {}", response);
        return response;
    }

    /**
     * Method returns ISignupProcessTask category id
     * @return
     */
    private Integer getCategoryId() {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        return jdbcTemplate.queryForObject(FIND_CATEGORY_ID_SQL, Integer.class);
    }

}
