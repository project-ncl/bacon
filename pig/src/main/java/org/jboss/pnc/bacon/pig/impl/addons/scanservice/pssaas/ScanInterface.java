package org.jboss.pnc.bacon.pig.impl.addons.scanservice.pssaas;

import org.jboss.pnc.bacon.pig.impl.addons.scanservice.ScanServiceDTO;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface ScanInterface {

    @POST
    @Path("/")
    @Consumes({ MediaType.APPLICATION_JSON })
    Response triggerScan(ScanServiceDTO builds);
}
