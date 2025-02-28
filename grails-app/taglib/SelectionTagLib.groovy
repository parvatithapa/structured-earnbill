/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.server.user.UserDTOEx
import com.sapienter.jbilling.server.user.db.UserStatusDAS
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.user.db.SubscriberStatusDTO;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.db.MetaField;

import org.hibernate.Criteria

import static com.sapienter.jbilling.server.util.Constants.CUSTOMER_CANCELLATION_STATUS_DESCRIPTION
import static com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID

class SelectionTagLib {
	
	def accountType = { attrs, bodyy -> 
		println System.currentTimeMillis();
		String checking, savings;
		savings=checking= ""

		if (attrs.value == 1 ) {
			checking= "checked=checked" 
		} else if (attrs.value == 2 ){
			savings="checked=checked"
		}
		
		out << "Checking<input type='radio' name=\'" + attrs.name + "\' value=\'1\' " + checking + ">"
		out << "Savings<input type='radio' name=\'"  + attrs.name + "\' value=\'2\' "  + savings + ">"
		
	}

    /**
     * Sets the value if the Meta Field Value is present
     * @attr field REQUIRED the Meta Field
     * @attr fieldsArray REQUIRED the Meta Fields Array Object
     */
    def setFieldValue = { attrs,body->

        List<MetaFieldValueWS> fieldValueWSList = attrs?.fieldsArray
        MetaField field = attrs?.field

        // iterate on list, match the field name with name and set the value
        fieldValueWSList.each { MetaFieldValueWS fieldValueWS->

            if (fieldValueWS.fieldName == field.name) {
                // got it
                out << fieldValueWS.value
            }
        }
    }

    /**
     * Checks if the Meta Field Value is present
     * @attr field REQUIRED the Meta Field
     * @attr fieldsArray REQUIRED the Meta Fields Array Object
     */
    def ifValuePresent = { attrs,body->

        List<MetaFieldValueWS> fieldValueWSList = attrs?.fieldsArray
        MetaField field = attrs?.field

        if (fieldValueWSList*.any {it.fieldName == field.name} ) {
            out << "true"
        }
    }
	
	def selectRoles = { attrs, body ->
		
		Integer langId= attrs.languageId?.toInteger();
		String name= attrs.name;		
		String value = attrs.value?.toString()
		
		List list= new ArrayList();
		String[] sarr= null;
		def company_id = session['company_id']
		for (RoleDTO role: RoleDTO.createCriteria.list(){
			eq('company', new CompanyDTO(company_id))
			order('roleTypeId', 'asc')
		}) {
			String title= role.getTitle(langId);
			sarr=new String[2]
			sarr[0]= role.getRoleTypeId()
			sarr[1]= title
			list.add(sarr)
		}
		out << render(template:"/selectTag", model:[name:name, list:list, value:value])
	}

	def userStatus = { attrs, body ->
		
		Integer langId= attrs.languageId?.toInteger();
		String name= attrs.name;
		String value = attrs.value?.toString()
        Integer roleId = attrs.roleId?.toInteger() ?: null
        boolean disabledTag = attrs.disabled?.toBoolean() ?: false
        List except = attrs.except ?: []
		String readOnly= attrs.readonly
		String disabled= attrs.disabled
		boolean disableTag= (disabled == 'true' || disabled == 'disabled') ? true : false

		List list= new ArrayList();
		String[] sarr= null;
		def company_id = session['company_id']

        List<UserStatusDTO> statuses = new ArrayList<UserStatusDTO>()
		UserStatusDAS userStatusDAS = new UserStatusDAS()
        UserStatusDTO activeStatus = userStatusDAS.find(UserDTOEx.STATUS_ACTIVE)
        statuses << activeStatus
		
		UserStatusDTO cancellationStatus = userStatusDAS.findByDescription(CUSTOMER_CANCELLATION_STATUS_DESCRIPTION, LANGUAGE_ENGLISH_ID)
		statuses.add(cancellationStatus)
        if(roleId?.equals(CommonConstants.TYPE_CUSTOMER)) {
            statuses.addAll(UserStatusDTO.createCriteria().list() {
                createAlias('ageingEntityStep', 'ageingEntitySteps', Criteria.INNER_JOIN)
                and {
                    eq("ageingEntitySteps.company", new CompanyDTO(company_id))
                }
            })
            statuses = statuses.unique().sort { it?.ageingEntityStep?.days }
        }
        for (UserStatusDTO status: statuses) {
            String title= status.getDescription(langId) + ( status.ageingEntityStep?.suspend ? "(${g.message(code: "config.ageing.suspended")})" : '')
            sarr=new String[2]
            sarr[0]= status.getId()
            sarr[1]= title
            // add the status if its id is not in the exception List
            if(!except.contains(status.getId())){
                list.add(sarr)
            }
        }
        def model = [name: name, list: list, value: value]
        if(disabledTag){
            model << [disabled: disabledTag]
        }

		out << render(template: "/selectTag", model: model)
		
	}
	
	def subscriberStatus = { attrs, body ->
		
		Integer langId= attrs.languageId?.toInteger();
		String name= attrs.name;
		String value = attrs.value?.toString()
        String cssClass = attrs.cssClass?.toString()
        def disabled = attrs.disabled

		log.info "Value of tagName=" + name + " is " + value
		
		List list= new ArrayList();
		String[] sarr= null;
		for (SubscriberStatusDTO status: SubscriberStatusDTO.list()) {
			String title= status.getDescription(langId);
			sarr=new String[2]
			sarr[0]= status.getId()
			sarr[1]= title
			list.add(sarr)
		}
		
		out << render(template:"/selectTag", model:[name:name, list:list, value:value, cssClass: cssClass, disabled: disabled])
	}

	def periodUnit = { attrs, body ->
		
		Integer langId= attrs.languageId?.toInteger();
		String name= attrs.name;
		String value = attrs.value?.toString()

		log.info "Value of tagName=" + name + " is " + value
		
		List list= new ArrayList();
		String[] sarr= null;
		for (PeriodUnitDTO period: PeriodUnitDTO.list()) {
			String title= period.getDescription(langId);
			sarr=new String[2]
			sarr[0]= period.getId()
			sarr[1]= title
			list.add(sarr)
		}
		
		out << render(template:"/selectTag", model:[name:name, list:list, value:value])
	}
	
}
