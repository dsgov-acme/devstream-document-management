lockfile=/persistent/storage/clamdb/lock
# lock file expires after 30 mins if not deleted by the process
currentlockexpiry=$(date +%s --date="now + 30 minutes")
currenttime=$(date +%s)
previouslockexpiry=$(cat $lockfile 2>/dev/null )
if [[ $previouslockexpiry && $previouslockexpiry -gt $currenttime ]]
then
    echo "Another process is updating the virus database. Skipping this run."
else
    echo $currentlockexpiry > $lockfile
    trap "echo removing $lockfile ; rm $lockfile" EXIT
    set -e
    cvd update -V
fi
