#!/bin/sh

# decodes and prints the components of a stringified IOR
# @version $Id: dior.tpl,v 1.2 2006-12-08 10:37:13 alphonse.bendt Exp $

@RESOLVE_JACORB_HOME@

JACO=${RESOLVED_JACORB_HOME}/bin/jaco

$JACO org.jacorb.orb.util.PrintIOR "$@"
