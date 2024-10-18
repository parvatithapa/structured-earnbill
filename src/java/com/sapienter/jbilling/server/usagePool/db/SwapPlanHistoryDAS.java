package com.sapienter.jbilling.server.usagePool.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;

import java.util.Date;
import java.util.List;

public class SwapPlanHistoryDAS extends AbstractDAS<SwapPlanHistoryDTO> {
	
	@SuppressWarnings("unchecked")
	public List<SwapPlanHistoryDTO> getSwapPlanHistroyByOrderId(Integer orderId) {
		 Criteria criteria = getSession().createCriteria(SwapPlanHistoryDTO.class)
				 .add(Restrictions.eq("orderId", orderId));
		 return criteria.list();
				 
	 }
	 
	 @SuppressWarnings("unchecked")
	public List<SwapPlanHistoryDTO> getSwapPlanHistroyByOrderAndSwapDate(Integer orderId, Date from, Date to) {
		 Criteria criteria = getSession().createCriteria(SwapPlanHistoryDTO.class)
	                .add(Restrictions.eq("orderId", orderId))
	                .add(Restrictions.ge("swapDate", from))
	                .add(Restrictions.lt("swapDate", to));
		 return criteria.list();
	                
	 }

	 public SwapPlanHistoryDTO getLatestSwapPlanHistoryByUserId(Integer userId){
		 String query =
				 "SELECT sph.id " +
				 "  FROM swap_plan_history sph " +
				 "  JOIN purchase_order po ON po.id = sph.order_id " +
				 "  JOIN base_user bu ON bu.id = po.user_id " +
				 " WHERE bu.id = :userId " +
				 "   AND swap_date = (SELECT MAX(swap_date) " +
				 "	                    FROM swap_plan_history)";
		 SQLQuery sqlQuery = getSession().createSQLQuery(query);
		 sqlQuery.setParameter("userId", userId);
		 Integer id  =(Integer) sqlQuery.uniqueResult();
		 return find(id);
	 }
}