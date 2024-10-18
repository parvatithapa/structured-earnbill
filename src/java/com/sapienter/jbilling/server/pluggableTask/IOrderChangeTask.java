package com.sapienter.jbilling.server.pluggableTask;

import java.math.BigDecimal;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

public interface IOrderChangeTask {

    String guessProductByAmount(BigDecimal Amount, Integer entityId) throws PluggableTaskException;
}
