package com.sapienter.jbilling.server.user.db;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.Query;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class CancellationRequestDAS extends AbstractDAS<CancellationRequestDTO>{

	@SuppressWarnings("unchecked")
	public List<CancellationRequestDTO> getCancellationRequestsByUserId(Integer userId){
		Criteria criteria = getSession().createCriteria(CancellationRequestDTO.class,"cancellationrequest")
				.createAlias("cancellationrequest.customer","cs")
				.createAlias("cs.baseUser","bu")
				.add(Restrictions.eq("bu.id",userId));
		return criteria.list();
	}


	String hql = "FROM CancellationRequestDTO dto WHERE dto.customer.baseUser.company.id = :entity_id and dto.createTimestamp >= :startdate and dto.createTimestamp <= :enddate";

	@SuppressWarnings("unchecked")
	public List<CancellationRequestDTO> findCancelRequestsByEntityAndDateRange(Integer entityId, Date startDate, Date endDate){
		Query query = getSession().createQuery(hql);
		query.setParameter("entity_id", entityId);
		query.setParameter("startdate", startDate);
		query.setParameter("enddate", endDate);
		return  query.list();
	}
	
	public List<CancellationRequestDTO> findCancellationRequestsToBeProcessedByEntityAndDate(Integer entityId, Date cancellationDate) {
		Criteria criteria = getSession().createCriteria(CancellationRequestDTO.class,"cancellationrequest")
				.add(Restrictions.le("cancellationrequest.cancellationDate", cancellationDate))
				.add(Restrictions.eq("cancellationrequest.status", CancellationRequestStatus.APPLIED))
				.createAlias("cancellationrequest.customer","cs")
				.createAlias("cs.baseUser","bu")
				.createAlias("bu.company","company")
				.add(Restrictions.eq("company.id", entityId));
		
		@SuppressWarnings("unchecked")
		List<CancellationRequestDTO> result = criteria.list();
		return (null!= result && !result.isEmpty()) ? result : Collections.emptyList();
	}
}
