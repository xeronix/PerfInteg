::perforce user
set P4USER=

::perforce workspace
set P4CLIENT=

::comma separated list of users whose changes need to be integrated
set USERS=

set SOURCE_BRANCH=

set TARGET_BRANCH=

::provide a numbered empty changelist (default doesn't work)
set NEW_PENDING_CHANGELIST=

call java -jar %~dp0\P4Integrate.jar