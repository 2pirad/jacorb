#!/bin/sh

# @version $Id: version.tpl,v 1.1 2007-01-17 12:18:34 alphonse.bendt Exp $
# @DONT_EDIT@

@RESOLVE_JACO_CMD@

JACO_CMD=${RESOLVED_JACO_CMD}

$JACO_CMD org.jacorb.util.BuildVersion
