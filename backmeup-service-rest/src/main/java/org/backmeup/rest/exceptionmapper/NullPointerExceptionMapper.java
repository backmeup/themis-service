package org.backmeup.rest.exceptionmapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NullPointerExceptionMapper extends
		CommonExceptionMapper<NullPointerException> implements
		ExceptionMapper<NullPointerException> {

	public NullPointerExceptionMapper() {
		super(Status.BAD_REQUEST);
	}

}