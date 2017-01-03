# TLDeflicker
Time Lapse Deflicker for day-to-night/night-to-day timelapses shot with GPhoto2Server script

CLI parameters:
<path to folder with jpg/jpeg images> <exposure EXIF directory name> <exposure EXIF tag name>

Example:
"/Users/me/Desktop/100D5100/" "Exif SubIFD" "Exposure Time"

Resulting images will be written into "deflickered" subdirectory of directory path to which is first parameter. 
Subdirectory "deflickered" will be created if it doesn't exist.
