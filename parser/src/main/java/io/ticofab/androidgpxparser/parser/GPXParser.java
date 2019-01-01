package io.ticofab.androidgpxparser.parser;

import android.util.Xml;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.ticofab.androidgpxparser.parser.domain.Bounds;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Link;
import io.ticofab.androidgpxparser.parser.domain.Metadata;
import io.ticofab.androidgpxparser.parser.domain.Point;
import io.ticofab.androidgpxparser.parser.domain.Route;
import io.ticofab.androidgpxparser.parser.domain.RoutePoint;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;
import io.ticofab.androidgpxparser.parser.domain.WayPoint;
import io.ticofab.androidgpxparser.parser.task.FetchAndParseGPXTask;
import io.ticofab.androidgpxparser.parser.task.GpxFetchedAndParsed;

public class GPXParser {

    static private final String TAG_GPX = "gpx";
    static private final String TAG_VERSION = "version";
    static private final String TAG_CREATOR = "creator";
    static private final String TAG_METADATA = "metadata";
    static private final String TAG_TRACK = "trk";
    static private final String TAG_SEGMENT = "trkseg";
    static private final String TAG_TRACK_POINT = "trkpt";
    static private final String TAG_LAT = "lat";
    static private final String TAG_LON = "lon";
    static private final String TAG_ELEVATION = "ele";
    static private final String TAG_TIME = "time";
    static private final String TAG_WAY_POINT = "wpt";
    static private final String TAG_ROUTE = "rte";
    static private final String TAG_ROUTE_POINT = "rtept";
    static private final String TAG_NAME = "name";
    static private final String TAG_DESC = "desc";
    static private final String TAG_CMT = "cmt";
    static private final String TAG_SRC = "src";
    static private final String TAG_LINK = "link";
    static private final String TAG_NUMBER = "number";
    static private final String TAG_TYPE = "type";
    static private final String TAG_TEXT = "text";
    static private final String TAG_AUTHOR = "author";
    static private final String TAG_COPYRIGHT = "copyright";
    static private final String TAG_KEYWORDS = "keywords";
    static private final String TAG_BOUNDS = "bounds";
    static private final String TAG_EXTENSIONS = "extensions";
    static private final String TAG_MIN_LAT = "minlat";
    static private final String TAG_MIN_LON = "minlon";
    static private final String TAG_MAX_LAT = "maxlat";
    static private final String TAG_MAX_LON = "maxlon";
    static private final String TAG_HREF = "href";

    static private final String ns = null;

    public void parse(String gpxUrl, GpxFetchedAndParsed listener) {
        new FetchAndParseGPXTask(gpxUrl, listener).execute();
    }

    public Gpx parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(in, null);
            parser.nextTag();
            return readGpx(parser);
        } finally {
            in.close();
        }
    }

    private Gpx readGpx(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<WayPoint> wayPoints = new ArrayList<>();
        List<Track> tracks = new ArrayList<>();
        List<Route> routes = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, TAG_GPX);

        Gpx.Builder builder = new Gpx.Builder();
        builder.setVersion(parser.getAttributeValue(null, TAG_VERSION));
        builder.setCreator(parser.getAttributeValue(null, TAG_CREATOR));

        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            switch (name) {
                case TAG_METADATA:
                    builder.setMetadata(readMetadata(parser));
                    break;
                case TAG_WAY_POINT:
                    wayPoints.add(readWayPoint(parser));
                    break;
                case TAG_ROUTE:
                    routes.add(readRoute(parser));
                    break;
                case TAG_TRACK:
                    tracks.add(readTrack(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_GPX);
        return builder
                .setWayPoints(wayPoints)
                .setRoutes(routes)
                .setTracks(tracks)
                .build();
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    private Track readTrack(XmlPullParser parser) throws XmlPullParserException, IOException {
        Track.Builder trackBuilder = new Track.Builder();

        List<TrackSegment> segments = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, TAG_TRACK);
        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_NAME:
                    trackBuilder.setTrackName(readName(parser));
                    break;
                case TAG_SEGMENT:
                    segments.add(readSegment(parser));
                    break;
                case TAG_DESC:
                    trackBuilder.setTrackDesc(readDesc(parser));
                    break;
                case TAG_CMT:
                    trackBuilder.setTrackCmt(readCmt(parser));
                    break;
                case TAG_SRC:
                    trackBuilder.setTrackSrc(readString(parser, TAG_SRC));
                    break;
                case TAG_LINK:
                    trackBuilder.setTrackLink(readLink(parser));
                    break;
                case TAG_NUMBER:
                    trackBuilder.setTrackNumber(readNumber(parser));
                    break;
                case TAG_TYPE:
                    trackBuilder.setTrackType(readString(parser, TAG_TYPE));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_TRACK);
        return trackBuilder
                .setTrackSegments(segments)
                .build();
    }

    private Link readLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_LINK);

        Link.Builder linkBuilder = new Link.Builder();
        linkBuilder.setLinkHref(parser.getAttributeValue(null, TAG_HREF));

        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_TEXT:
                    linkBuilder.setLinkText(readString(parser, TAG_TEXT));
                    break;
                case TAG_TYPE:
                    linkBuilder.setLinkType(readString(parser, TAG_TYPE));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_LINK);
        return linkBuilder.build();
    }

    private Bounds readBounds(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_BOUNDS);
        Bounds bounds = new Bounds.Builder()
                .setMinLat(Double.valueOf(parser.getAttributeValue(null, TAG_MIN_LAT)))
                .setMinLon(Double.valueOf(parser.getAttributeValue(null, TAG_MIN_LON)))
                .setMaxLat(Double.valueOf(parser.getAttributeValue(null, TAG_MAX_LAT)))
                .setMaxLon(Double.valueOf(parser.getAttributeValue(null, TAG_MAX_LON)))
                .build();

        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, ns, TAG_BOUNDS);

        return bounds;
    }

    // Processes summary tags in the feed.
    private TrackSegment readSegment(XmlPullParser parser) throws IOException, XmlPullParserException {
        List<TrackPoint> points = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, TAG_SEGMENT);
        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_TRACK_POINT:
                    points.add(readTrackPoint(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_SEGMENT);
        return new TrackSegment.Builder()
                .setTrackPoints(points)
                .build();
    }

    private Route readRoute(XmlPullParser parser) throws IOException, XmlPullParserException {
        List<RoutePoint> points = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, ns, TAG_ROUTE);
        Route.Builder routeBuilder = new Route.Builder();

        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_ROUTE_POINT:
                    points.add(readRoutePoint(parser));
                    break;
                case TAG_NAME:
                    routeBuilder.setRouteName(readName(parser));
                    break;
                case TAG_DESC:
                    routeBuilder.setRouteDesc(readDesc(parser));
                    break;
                case TAG_CMT:
                    routeBuilder.setRouteCmt(readCmt(parser));
                    break;
                case TAG_SRC:
                    routeBuilder.setRouteSrc(readString(parser, TAG_SRC));
                    break;
                case TAG_LINK:
                    routeBuilder.setRouteLink(readLink(parser));
                    break;
                case TAG_NUMBER:
                    routeBuilder.setRouteNumber(readNumber(parser));
                    break;
                case TAG_TYPE:
                    routeBuilder.setRouteType(readString(parser, TAG_TYPE));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_ROUTE);
        return routeBuilder
                .setRoutePoints(points)
                .build();
    }

    /**
     * Reads a single point, which can either be a {@link TrackPoint}, {@link RoutePoint} or {@link WayPoint}.
     *
     * @param builder The prepared builder, one of {@link TrackPoint.Builder}, {@link RoutePoint.Builder} or {@link WayPoint.Builder}.
     * @param parser  Parser
     * @param tagName Tag name, e.g. trkpt, rtept, wpt
     */
    private Point readPoint(Point.Builder builder, XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, tagName);

        builder.setLatitude(Double.valueOf(parser.getAttributeValue(null, TAG_LAT)));
        builder.setLongitude(Double.valueOf(parser.getAttributeValue(null, TAG_LON)));

        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_NAME:
                    builder.setName(readName(parser));
                    break;
                case TAG_DESC:
                    builder.setDesc(readDesc(parser));
                    break;
                case TAG_ELEVATION:
                    builder.setElevation(readElevation(parser));
                    break;
                case TAG_TIME:
                    builder.setTime(readTime(parser));
                    break;
                case TAG_TYPE:
                    builder.setType(readType(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, tagName);
        return builder.build();
    }

    private Metadata readMetadata(XmlPullParser parser) throws XmlPullParserException, IOException {
        Metadata.Builder metadataBuilder = new Metadata.Builder();

        parser.require(XmlPullParser.START_TAG, ns, TAG_METADATA);
        while (loopMustContinue(parser.next())) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case TAG_NAME:
                    metadataBuilder.setName(readName(parser));
                    break;
                case TAG_DESC:
                    metadataBuilder.setDesc(readDesc(parser));
                    break;
                case TAG_AUTHOR:
                    metadataBuilder.setAuthor(readString(parser, TAG_AUTHOR));
                    break;
                case TAG_COPYRIGHT:
                    metadataBuilder.setCopyright(readString(parser, TAG_COPYRIGHT));
                    break;
                case TAG_LINK:
                    metadataBuilder.setLink(readLink(parser));
                    break;
                case TAG_TIME:
                    metadataBuilder.setTime(readTime(parser));
                    break;
                case TAG_KEYWORDS:
                    metadataBuilder.setKeywords(readString(parser, TAG_KEYWORDS));
                    break;
                case TAG_BOUNDS:
                    metadataBuilder.setBounds(readBounds(parser));
                    break;
                case TAG_EXTENSIONS:
                    metadataBuilder.setExtensions(readString(parser, TAG_EXTENSIONS));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, TAG_METADATA);
        return metadataBuilder.build();
    }

    private WayPoint readWayPoint(XmlPullParser parser) throws XmlPullParserException, IOException {
        return (WayPoint) readPoint(new WayPoint.Builder(), parser, TAG_WAY_POINT);
    }

    private TrackPoint readTrackPoint(XmlPullParser parser) throws IOException, XmlPullParserException {
        return (TrackPoint) readPoint(new TrackPoint.Builder(), parser, TAG_TRACK_POINT);
    }

    private RoutePoint readRoutePoint(XmlPullParser parser) throws IOException, XmlPullParserException {
        return (RoutePoint) readPoint(new RoutePoint.Builder(), parser, TAG_ROUTE_POINT);
    }

    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        return readString(parser, TAG_NAME);
    }

    private String readDesc(XmlPullParser parser) throws IOException, XmlPullParserException {
        return readString(parser, TAG_DESC);
    }

    private String readType(XmlPullParser parser) throws IOException, XmlPullParserException {
        return readString(parser, TAG_TYPE);
    }

    private String readCmt(XmlPullParser parser) throws IOException, XmlPullParserException {
        return readString(parser, TAG_CMT);
    }

    private String readString(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tag);
        String value = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tag);
        return value;
    }

    private Double readElevation(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_ELEVATION);
        Double ele = Double.valueOf(readText(parser));
        parser.require(XmlPullParser.END_TAG, ns, TAG_ELEVATION);
        return ele;
    }

    private DateTime readTime(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_TIME);
        DateTime time = ISODateTimeFormat.dateTimeParser().parseDateTime(readText(parser));
        parser.require(XmlPullParser.END_TAG, ns, TAG_TIME);
        return time;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private Integer readNumber(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_NUMBER);
        Integer number = Integer.valueOf(readText(parser));
        parser.require(XmlPullParser.END_TAG, ns, TAG_NUMBER);
        return number;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private boolean loopMustContinue(int next) {
        return next != XmlPullParser.END_TAG && next != XmlPullParser.END_DOCUMENT;
    }
}
