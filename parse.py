from pykml import parser
import sys
import numpy as np
from datetime import timezone, datetime
import math
import regex as re
from zipfile import ZipFile
import uuid

def getPlacemarks(object):
    try:
        folders = object.Document.Folder
        if not isinstance(folders, list):
            folders = [folders]
        placemark = []
        for folder in folders:
            ps = folder.placemarks
            if (ps is None):
                continue
            elif(isinstance(ps, list)):
                placemark.append(ps)
            else:
                placemark.append(ps)
    except:
        try:
            placemark = []
            for i in object.Document.Placemark:
                placemark.append(i)
            if (placemark is None):
                placemark = []
            elif not isinstance(placemark, list):
                placemark = [placemark]
        except:
            return None
    return placemark

def tryParsePoint(placemark, metadata):
    try:
        point = placemark.Point
        latlng = point.coordinates.text
        name = tryParseName(placemark)
        description = tryParseDescription(placemark)
        timespan = tryParseTimeSpan(placemark, metadata)
        return [{
            'lat': float(latlng.split(",")[1]),
            'lng': float(latlng.split(",")[0]),
            'begin': timespan['begin'],
            'end': timespan['end'],
            'name': name,
            'description': description,
            }]
    except:
        return None

def ParseKml(text):
    output = []
    with open(text) as f:
        object = parser.parse(f, {}).getroot()
    placemarks = getPlacemarks(object)
    if placemarks == None:
        match = re.search(r'[0-9]{4}-[0-9]{2}-[0-9]{2}', object.Document.name.text)
        group = match.group()
        data = convKmlDateOnly(group)
        return [{'begin': data,
                    'end': data,
                    'description': 'No data available for this day'}]
    for mark in range(len(placemarks)):
        metadata = tryParseMetadataFromDescription(placemarks[mark])
        if (metadata):
            print("Found metadata in ${tryParseName(placemark)}:", metadata)
        retval = tryParsePoint(placemarks[mark], metadata) #funziona
        if (retval is not None):
            output.append(retval)
            continue
        retval = tryParseLineString(placemarks[mark], metadata)
        if (retval is not None):
            output.append(retval)
            continue
    return output

def tryParseMetadataFromDescription(placemark):
    desc = placemark.description
    if (desc is None):
        return {}
    desc = re.sub('/<br>/g', '\n', desc.text)
    return {}


def tryParseLineString(placemark, metadata):
    try:
        line_string = placemark.LineString
        coords_elements = line_string.coordinates.text
        if coords_elements == None:
            print("Invalid LineString data: ", placemark)
            return None
        coords = []
        coords_elements = re.compile("\s+").split(coords_elements)
        for coord_string in range(len(coords_elements)-1):
            lng = coords_elements[coord_string].split(",")[0]
            lat = coords_elements[coord_string].split(",")[1]
            coords.append({'lng':float(lng), 'lat':float(lat)})
        name = tryParseName(placemark)
        description = tryParseDescription(placemark)
        try:
            timespan = tryParseTimeSpan(placemark, metadata)
        except:
            return None
        return intrapolateCoords(coords, timespan, name, description)

    except:
        return None

def tryParseName(placemark):
    return placemark.name

def tryParseDescription(placemark):
    return placemark.description

def tryParseTimeSpan(placemark, metadata):
    timespan = placemark.TimeSpan
    if ('begin' in metadata and 'end' in metadata):
        return {'begin': metadata.begin, 'end': metadata.end}
    elif timespan is not None:
        time_begin = timespan.begin.text
        time_end = timespan.end.text
        return {'begin':convKmlDateToTimestamp(time_begin), 'end': convKmlDateToTimestamp(time_end)}
    else:
        print("Invalid timespan data: ", placemark)
        return None

def convKmlDateOnly(kml_date):
    year, month, day = kml_date.split("-")
    dt = datetime(int(year), int(month), int(day), tzinfo=timezone.utc)
    datum = dt.timestamp()
    return datum

def convKmlDateToTimestamp(kml_date):
    date, time = kml_date.split("T")
    year, month, day = date.split("-")
    hour, minute, second = time.split("Z")[0].split(":")
    dt = datetime(int(year), int(month), int(day), int(hour), int(minute), int(float(second)), tzinfo=timezone.utc)
    datum = dt.timestamp()
    return datum

def intrapolateCoords(coords, timespan, name, description):
    delta_t = timespan['end'] - timespan['begin']
    total_dist = 0
    segment_dist = [0]
    for i in range(1,len(coords)):
        d = getDistanceFromLatLonInMeters(coords[i - 1]['lat'], coords[i - 1]['lng'],coords[i]['lat'], coords[i]['lng'])
        segment_dist.append(d)
        total_dist = total_dist + d
    if total_dist == 0:
        return [{
        'lat': coords[0]['lat'],
        'lng': coords[0]['lng'],
        'begin': timespan.begin,
        'end': timespan.end,
        'name': name,
        'description': description}]

    retval = []
    current_t = timespan['begin']
    for i in range(1,len(coords)):
        segment_t = delta_t * segment_dist[i] / total_dist
        num_seg = math.ceil(segment_dist[i] / 100)
        px, py = coords[i - 1]['lat'], coords[i - 1]['lng']
        qx, qy = coords[i]['lat'], coords[i]['lng']
        for j in range(num_seg):
            r = (j + 0.5) / num_seg
            begin = current_t + segment_t * (j / num_seg)
            end = current_t + segment_t * ((j + 1) / num_seg)
            retval.append({
            'lat': px * (1 - r) + qx * r,
            'lng': py * (1 - r) + qy * r,
            'begin': begin,
            'end': end,
            'name': name,
            'description': description,})
        current_t = current_t + segment_t
    return retval
def getDistanceFromLatLonInMeters(lat1, lon1, lat2, lon2):
    R = 6371
    dLat = np.deg2rad(lat2-lat1)
    dLon = np.deg2rad(lon2-lon1)
    a = math.sin(dLat/2) * math.sin(dLat/2) + math.cos(np.deg2rad(lat1)) * math.cos(np.deg2rad(lat2)) * math.sin(dLon/2) * math.sin(dLon/2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    d = R * c
    return d * 1000

def extraction(file):
    file_name = []
    if file.split('.')[-1] == 'zip':
        with ZipFile(file, 'r') as zip:
            zip.extractall()
        for i in zip.infolist():
            file_name.append(i.filename)
        return file_name
    else:
        print('Compress in zip format all the kml files')

if __name__ == "__main__":
    file = sys.argv[1]
    user = uuid.uuid1() # uuid code per ogni utente
    final_data = []
    for data in extraction(file):
        final_data.extend(ParseKml(data))
    fin = {user.hex:final_data} # associo un uuid code ai percorsi estratti dall'utente
    print(fin[user.hex][0]) #esempio di output
