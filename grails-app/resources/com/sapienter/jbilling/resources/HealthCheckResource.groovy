package com.sapienter.jbilling.resources

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

import static javax.ws.rs.core.MediaType.APPLICATION_JSON

@Path("/api/healthCheck")
class HealthCheckResource {

	@GET
	@Produces(APPLICATION_JSON)
	public Response healthCheck() {
		return Response.ok().entity("{\"status\":\"UP\"}").build()
	}
}
