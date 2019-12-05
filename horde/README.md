Externalized access control
====

This module implements externalized access control, meaning that when
an incoming Gy requerst hits our system, it is sent to Prime who then at
some point, through some mechanism either permits or rejects use.

This module implements logic that permits external services to be involved
in this calculation.   It does this by implementing a gRPC client along the
lines outlined in the github repo:

    https://github.com/ExploratoryEngineering/gyorde

In addition to the base protocol client, which operates in a synchronous
(blocking) mode.

A configuration will be added that permits experimentation using this
protocol.  The configuration will initially look like this:

     horde:
	  server: horde1-server
	    host: foo.bar.baz
	    port: 1234
	    imsilist:
	      - 123456789012345
   	      - 123456789012344
	      - 123456789012343

The server/port parameters are the coordinates for the remote grpc server,
the imsis is a whitelist of imsis for which the external service will be
consulted.

The requests will contain information about:

    IMSI, Current IP, Type of IP address (IPV4 or IPV6)

The return value will be a simple boolean value interpreted as yes/no.

If this experiment proves successful, later versions will be able to
use more parameters than ip/imsi to make the determination for access,
and we will also introduce more encryption/authentication parameters
for the grpc connection

The name "horde" is used for the associated directory, since one of the components we will be interfacing
with is called "horde".
