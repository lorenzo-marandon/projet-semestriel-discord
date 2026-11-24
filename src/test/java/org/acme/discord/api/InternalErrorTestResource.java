package org.acme.discord.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * Test-only resource used to exercise the complete unexpected-error HTTP chain.
 */
@Path("/api/test/internal-error")
@Produces(MediaType.APPLICATION_JSON)
public class InternalErrorTestResource {

    @GET
    @Operation(hidden = true)
    public void fail() {
        throw new IllegalStateException("SQL password=integration-test-secret");
    }
}
