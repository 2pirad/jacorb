#!/bin/sh

# decodes and prints the components of a stringified IOR
# @version $Id: dior.tpl,v 1.3 2006-12-15 14:27:24 alphonse.bendt Exp $
# @DONT_EDIT@

@RESOLVE_JACO_CMD@

JACO_CMD=${RESOLVED_JACO_CMD}

$JACO_CMD org.jacorb.orb.util.PrintIOR "$@"
