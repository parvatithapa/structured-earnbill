package com.sapienter.jbilling.server.usagePool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.sapienter.jbilling.server.usagePool.db.SwapPlanHistoryDAS;
import com.sapienter.jbilling.server.usagePool.db.SwapPlanHistoryDTO;

public class SwapPlanHistoryBL {
	
	private SwapPlanHistoryDAS das;
	
	public SwapPlanHistoryBL() {
		das = new SwapPlanHistoryDAS();
	}
	
	public SwapPlanHistoryWS[] getSwapPlanHistroyByOrderId(Integer orderId) {
		List<SwapPlanHistoryWS> result = new ArrayList<SwapPlanHistoryWS>();
		for(SwapPlanHistoryDTO history : das.getSwapPlanHistroyByOrderId(orderId)) {
			result.add(new SwapPlanHistoryWS(history.getId(), history.getOldPlanId(), history.getNewPlanID(), history.getId(),
        			history.getSwapDate(), history.getOldPlanOverageQuantity().setScale(2, BigDecimal.ROUND_HALF_UP).toString(), 
        			history.getOldPlanOverageQuantity().setScale(2, BigDecimal.ROUND_HALF_UP).toString(), 
        			history.getOldPlanUsedfreeQuantity().setScale(2, BigDecimal.ROUND_HALF_UP).toString()));
		}
		
		return result.toArray(new SwapPlanHistoryWS[result.size()]);
	}
	
	public SwapPlanHistoryWS[] getSwapPlanHistroyByOrderAndSwapDate(Integer orderId, Date from, Date to) {
		List<SwapPlanHistoryWS> result = new ArrayList<SwapPlanHistoryWS>();
		for(SwapPlanHistoryDTO history : das.getSwapPlanHistroyByOrderAndSwapDate(orderId, from, to)) {
			result.add(new SwapPlanHistoryWS(history.getId(), history.getOldPlanId(), history.getNewPlanID(), history.getId(),
        			history.getSwapDate(), history.getOldPlanOverageQuantity().toString(), 
        			history.getOldPlanOverageQuantity().toString(), history.getOldPlanUsedfreeQuantity().toString()));
		}
		
		return result.toArray(new SwapPlanHistoryWS[result.size()]);
	}
	
}
