package org.popcraft.chunkyborder.integration;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapAPIListener;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.ShapeMarker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.popcraft.chunky.shape.AbstractEllipse;
import org.popcraft.chunky.shape.AbstractPolygon;
import org.popcraft.chunky.shape.Circle;
import org.popcraft.chunky.shape.Oval;
import org.popcraft.chunky.shape.Shape;

import java.awt.Color;
import java.io.IOException;

public class BlueMapIntegration implements MapIntegration, BlueMapAPIListener {
    BlueMapAPI blueMapAPI;
    private final String SET_LABEL = "world border";

    @Override
    public void onEnable(BlueMapAPI blueMapApi) {
        this.blueMapAPI = blueMapApi;
    }

    @Override
    public void onDisable(BlueMapAPI blueMapApi) {
        this.blueMapAPI = null;
    }

    @Override
    public void addShapeMarker(World world, Shape shape) {
        if (blueMapAPI == null) {
            return;
        }
        final MarkerAPI markerAPI;
        try {
            markerAPI = blueMapAPI.getMarkerAPI();
            markerAPI.load();
        } catch (IOException e) {
            return;
        }
        final MarkerSet markerSet = markerAPI.createMarkerSet(SET_LABEL);
        de.bluecolored.bluemap.api.marker.Shape blueShape;
        if (shape instanceof AbstractPolygon) {
            AbstractPolygon polygon = (AbstractPolygon) shape;
            double[] pointsX = polygon.pointsX();
            double[] pointsZ = polygon.pointsZ();
            if (pointsX.length != pointsZ.length) {
                return;
            }
            Vector2d[] points = new Vector2d[pointsX.length];
            for (int i = 0; i < pointsX.length; ++i) {
                points[i] = new Vector2d(pointsX[i], pointsZ[i]);
            }
            blueShape = new de.bluecolored.bluemap.api.marker.Shape(points);
        } else if (shape instanceof AbstractEllipse) {
            AbstractEllipse ellipse = (AbstractEllipse) shape;
            double[] center = ellipse.getCenter();
            double[] radii = ellipse.getRadii();
            Vector2d centerPos = new Vector2d(center[0], center[1]);
            blueShape = createEllipse(centerPos, radii[0], radii[1], 100);
        } else {
            blueShape = new de.bluecolored.bluemap.api.marker.Shape();
        }
        blueMapAPI.getWorld(world.getUID()).ifPresent(blueWorld -> blueWorld.getMaps().forEach(map -> {
            ShapeMarker marker = markerSet.createShapeMarker(shapeLabel(world), map, blueShape, world.getSeaLevel());
            marker.setColors(Color.RED, new Color(0, true));
        }));
        try {
            markerAPI.save();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void removeShapeMarker(World world) {
        if (blueMapAPI == null) {
            return;
        }
        final MarkerAPI markerAPI;
        try {
            markerAPI = blueMapAPI.getMarkerAPI();
            markerAPI.load();
        } catch (IOException e) {
            return;
        }
        final MarkerSet markerSet = markerAPI.createMarkerSet(SET_LABEL);
        blueMapAPI.getWorld(world.getUID()).ifPresent(blueWorld -> blueWorld.getMaps().forEach(map -> {
            markerSet.removeMarker(shapeLabel(world));
        }));
        try {
            markerAPI.save();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void removeAllShapeMarkers() {
        if (blueMapAPI == null) {
            return;
        }
        final MarkerAPI markerAPI;
        try {
            markerAPI = blueMapAPI.getMarkerAPI();
            markerAPI.load();
        } catch (IOException e) {
            return;
        }
        final MarkerSet markerSet = markerAPI.createMarkerSet(SET_LABEL);
        Bukkit.getWorlds().forEach(world -> markerSet.removeMarker(shapeLabel(world)));
        try {
            markerAPI.save();
        } catch (IOException ignored) {
        }
    }

    private String shapeLabel(World world) {
        return String.format("%s.%s", "chunky.markerset", world.getName());
    }

    private de.bluecolored.bluemap.api.marker.Shape createEllipse(Vector2d centerPos, double radiusX, double radiusZ, int points) {
        if (points < 3) throw new IllegalArgumentException("A shape has to have at least 3 points!");

        Vector2d[] pointArray = new Vector2d[points];
        double segmentAngle = 2 * Math.PI / points;
        double angle = 0d;
        for (int i = 0; i < points; i++) {
            pointArray[i] = centerPos.add(Math.sin(angle) * radiusX, Math.cos(angle) * radiusZ);
            angle += segmentAngle;
        }

        return new de.bluecolored.bluemap.api.marker.Shape(pointArray);
    }
}
