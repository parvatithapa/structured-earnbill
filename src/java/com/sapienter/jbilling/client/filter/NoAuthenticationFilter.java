package com.sapienter.jbilling.client.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.web.filter.GenericFilterBean;

/**
 * Created by rvaibhav on 13/12/17.
 */
public class NoAuthenticationFilter extends GenericFilterBean {
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		chain.doFilter(request, response);
	}
}
