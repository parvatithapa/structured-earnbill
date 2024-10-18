package com.sapienter.jbilling.server.pluggableTask;


import java.util.List;
import java.util.UUID;


public interface UndoMediationFilterTask  {
	 List<Integer> getOrderIdsEligibleForUndoMediation(UUID mediationProcessId) throws TaskException;
}
