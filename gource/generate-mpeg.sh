#!/bin/bash

DESTINATION="ostelco-core.mp4"
DEPENDENCIES="gource  ffmpeg"
for DEP in $DEPENDENCIES ; do 
    if [[ -z "$(which $DEP)" ]] ; then
	echo "Could not find any \"$DEP\" executable in the current runtime envinronment, bailing out"
	exit 0
    fi
done

# Set up the dommands in the somewhat complicated pipeline
#  --start-position 0.09 --stop-position 0.1
GOURCE_CMD='gource -f -1280x720 --seconds-per-day 1 --auto-skip-seconds 1 --title "ostelco-core" -o - '
FFMPEG_CMD="ffmpeg -y -r 60 -f image2pipe -vcodec ppm -i - -vcodec libx264 -preset ultrafast -pix_fmt yuv420p -crf 1 -threads 0 -bf 0 $DESTINATION"

# Then run the pipeline
$GOURCE_CMD | $FFMPEG_CMD
