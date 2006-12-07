#!/bin/sh

# decodes and prints the components of a stringified IOR
# @version $Id: dior.tpl,v 1.1 2006-12-07 12:16:13 alphonse.bendt Exp $

@RESOLVE_JACORB_HOME@

JACO=${RESOLVED_JACORB_HOME}/jaco

$JACO org.jacorb.orb.util.PrintIOR "$@"
