package com.molean.staticmap;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.molean.staticmap.nms.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings({"deprecation"})
public class MapUtils {

    static class MyMapRenderer extends MapRenderer {
        public final int hashCode;
        public final byte[] colors;
        public final List<MapCursor> cursors;
        private final MapCursorCollection realCursors;
        private MyMapRenderer(byte[] colors, List<MapCursor> cursors) {
            this.colors = colors;
            this.hashCode = Arrays.hashCode(colors);
            this.cursors = cursors;
            this.realCursors = new MapCursorCollection();
            if (cursors != null && !cursors.isEmpty()) {
                for (MapCursor cursor : cursors) {
                    if (hiddenCursors.contains(cursor.getType())) continue;
                    this.realCursors.addCursor(cursor);
                }
            }
        }

        @Override
        public void render(@NotNull MapView mapView, @NotNull MapCanvas mapCanvas, @NotNull Player player) {
            mapCanvas.setCursors(this.realCursors);
            for (int i = 0; i < 128 * 128 && i < colors.length; i++) {
                int x = i % 128;
                int y = i / 128;
                byte color = colors[i];
                mapCanvas.setPixel(x, y, color);
            }
        }
    }

    protected static final Set<MapCursor.Type> hiddenCursors = new HashSet<>();

    public static void updateStaticMap(MapMeta mapMeta, byte[] bytes, List<MapCursor> cursors) {
        if (bytes == null) {
            return;
        }
        MapView mapView = mapMeta.hasMapView() ? mapMeta.getMapView() : null;
        if (mapView != null) try {
            MapRenderer mapRenderer = mapView.getRenderers().get(0);
            if (mapRenderer instanceof MyMapRenderer) {
                if (((MyMapRenderer) mapRenderer).hashCode == Arrays.hashCode(bytes)) {
                    return;
                }
            }
        } catch (Exception ignored) {}

        // 如果地图有 MapView 就用原来的，不要新建了
        if (mapView == null) {
            mapView = Bukkit.createMap(Bukkit.getWorlds().get(0));
        }
        while (!mapView.getRenderers().isEmpty()) {
            mapView.removeRenderer(mapView.getRenderers().get(0));
        }
        mapView.addRenderer(new MyMapRenderer(bytes, cursors));
        mapMeta.setMapView(mapView);
    }


    public static byte[] toBytes(List<MapCursor> cursors) {
        if (cursors.isEmpty()) return null;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeInt(cursors.size());
        for (MapCursor cursor : cursors) {
            out.writeByte(cursor.getX());
            out.writeByte(cursor.getY());
            out.writeByte(cursor.getDirection());
            out.writeByte(cursor.getRawType());
            out.writeBoolean(cursor.isVisible());
            if (cursor.getCaption() != null) {
                out.writeUTF(cursor.getCaption());
            } else {
                out.writeUTF("");
            }
        }
        return out.toByteArray();
    }

    public static List<MapCursor> fromBytes(byte[] cursors) {
        if (cursors == null) return null;
        ByteArrayDataInput in = ByteStreams.newDataInput(cursors);
        List<MapCursor> list = new ArrayList<>();
        int length = in.readInt();
        for (int i = 0; i < length; i++) {
            byte x = in.readByte();
            byte y = in.readByte();
            byte direction = in.readByte();
            byte type = in.readByte();
            boolean visible = in.readBoolean();
            String caption = in.readUTF();
            list.add(new MapCursor(x, y, direction, type, visible, caption.isEmpty() ? null : caption));
        }
        return list;
    }

    private static MapRenderer getRendererOrNull(MapMeta mapMeta) {
        MapView mapView = mapMeta.getMapView();

        if (mapView == null) {
            return null;
        }
        List<MapRenderer> renderers = mapView.getRenderers();
        if (renderers.isEmpty()) {
            return null;
        }
        return renderers.get(0);
    }

    public static byte[] getColors(MapMeta mapMeta) {
        MapRenderer renderer = getRendererOrNull(mapMeta);
        if (renderer == null) return null;
        return VersionManager.getColors(renderer);
    }

    public static List<MapCursor> getCursors(Player player, MapMeta mapMeta) {
        MapRenderer renderer = getRendererOrNull(mapMeta);
        if (renderer == null) return null;
        return VersionManager.getCursors(player, renderer);
    }

}
