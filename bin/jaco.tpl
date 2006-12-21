#!/bin/sh
#
# JACO - JacORB cmdline
# @author Christoph Becker (PrismTech)
# @author Alexander Fetke (PrismTech)
# @author Alphonse Bendt (PrismTech)
# @version $Id: jaco.tpl,v 1.5 2006-12-21 10:08:51 alphonse.bendt Exp $
# @DONT_EDIT@

@RESOLVE_JACORB_HOME@
JACORB_HOME=${RESOLVED_JACORB_HOME}

@RESOLVE_JAVA_CMD@
JAVA_CMD=${RESOLVED_JAVA_CMD}

# verbosity output
#echo    "using JAVA_CMD   : ${JAVA_CMD}"
#echo    "Using JacORB from: ${JACORB_HOME}"
#echo -e "using CLASSPATH  :\n\t`echo $CLASSPATH | sed -e 's/:/\n\t/g'`"

exec "$JAVA_CMD"                                                    \
    @JACORB_BOOTCLASSPATH@                                          \
    -Djacorb.home="${JACORB_HOME}"                                  \
    -Dorg.omg.CORBA.ORBClass=org.jacorb.orb.ORB                     \
    -Dorg.omg.CORBA.ORBSingletonClass=org.jacorb.orb.ORBSingleton   \
    -classpath "${CLASSPATH}"                                       \
     "$@"
