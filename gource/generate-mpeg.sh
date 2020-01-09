#!/bin/bash

DESTINATION="ostelco-core.mp4"
DEPENDENCIES="gource  ffmpeg"
for DEP in $DEPENDENCIES ; do 
    if [[ -z "$(which $DEP)" ]] ; then
	echo "Could not find any \"$DEP\" executable in the current runtime envinronment, bailing out"
	exit 0
    fi
done


GOURCE_CMD='gource --start-position 0.09 --stop-position 0.1 -f -1280x720 --seconds-per-day 1 --auto-skip-seconds 1 --title "ostelco-core" -o - '
FFMPEG_WITH_AUDIO_CMD="ffmpeg -y -b 3000K -r 28 -f image2pipe -vcodec ppm -i - -i audio.mp3 -vcodec libx264 -preset slow -crf 28 -threads 0 $DESTINATION_FILE"
FFMPEG_WITHOUT_AUDIO_CMD="ffmpeg -y -b 3000K -r 28 -f image2pipe -vcodec ppm -i - -vcodec libx264 -preset slow -crf 28 -threads 0 $DESTINATION_FILE"
FFMPEG_CMD="ffmpeg -y -r 60 -f image2pipe -vcodec ppm -i - -vcodec libx264 -preset ultrafast -pix_fmt yuv420p -crf 1 -threads 0 -bf 0 $DESTINATION"

$GOURCE_CMD | $FFMPEG_CMD
