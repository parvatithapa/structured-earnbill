package com.sapienter.jbilling.server.security;

import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import grails.plugin.springsecurity.SpringSecurityService;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

@Aspect
@Order(2)
public class WSSecurityCheck {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Resource
	private SpringSecurityService springSecurityService;
	@Resource
	private SecurityHelperService securityHelperService;

	@PostConstruct
	void init() {
		Assert.notNull(springSecurityService, "springSecurityService is required property");
		Assert.notNull(securityHelperService, "securityHelperService is required property");
	}

	@Before("execution(* com.sapienter.jbilling.server.util.IWebServicesSessionBean.*(..))")
	public void securityCheck(JoinPoint joinPoint) {
		logger.debug("Validating web-service method='{}'", joinPoint.getSignature().getName());
		Object[] args = joinPoint.getArgs();
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		Method method = methodSignature.getMethod();
		Validator.Type type = Validator.Type.VIEW;
		Validator annotation = getAnnotation(method);
		if(null != annotation) {
			type = annotation.type();
		}
		//Avoid un-authenticated calls, except for methods with validation type = NONE.
		if (!springSecurityService.isLoggedIn() && !type.equals(Validator.Type.NONE)) {
			String message = "Web-service call has not been authenticated.";
			logger.warn(message);
			throw new SecurityException(message);
		}
		// validate call.
		securityHelperService.validateAccess(method, args, type);
	}

	/**
	 * Helper method to fetch Annotation from method level, and if not found then check in the interface method Annotation.
	 * @param method for which the JoinPoint is executed
	 * @return Validator Annotation if present, else returns null
	 */
	private static Validator getAnnotation(Method method) {
		Validator annotation =  method.getAnnotation(Validator.class);
		if(null == annotation) {
			// trying to fetch interface level annotation for the same method.
			logger.debug("Fetching annotation from Interface class since Implementation class does not have annotation for validator class ='{}'", method.getName());
			Method[] declaredInterfaceMethods = IWebServicesSessionBean.class.getDeclaredMethods();
			annotation = Arrays.stream(declaredInterfaceMethods).filter(interfaceMethod -> (interfaceMethod.getName() == method.getName() && interfaceMethod.getReturnType().equals(method.getReturnType()))).findFirst().get().getAnnotation(Validator.class);
		}
		logger.debug("Fetched annotation ='{}', for method ='{}'", annotation,  method.getName());
		return annotation;
	}

}
