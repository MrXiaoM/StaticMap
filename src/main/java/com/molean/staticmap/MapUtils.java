package com.molean.staticmap;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.molean.staticmap.nms.VersionManager;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.molean.staticmap.StaticMapListener.IDS;

@SuppressWarnings({"deprecation"})
public class MapUtils {

    public static class MyMapRenderer extends MapRenderer {
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

    public static void updateStaticMap(ItemStack item, MapMeta mapMeta, String serverName, byte[] bytes, List<MapCursor> cursors) {
        // 如果数据异常，则忽略
        if (bytes == null) {
            item.setItemMeta(mapMeta);
            return;
        }
        MapView mapView = mapMeta.hasMapView() ? mapMeta.getMapView() : null;
        // 如果在当前子服已经应用过地图画，则忽略
        if (mapView != null) try {
            MapRenderer mapRenderer = mapView.getRenderers().get(0);
            if (mapRenderer instanceof MyMapRenderer) {
                if (((MyMapRenderer) mapRenderer).hashCode == Arrays.hashCode(bytes)) {
                    item.setItemMeta(mapMeta);
                    return;
                }
            }
        } catch (Exception ignored) {}

        // 获取地图在当前子服的 ID
        Integer id = NBT.get(item, nbt -> {
            ReadableNBT compound = nbt.getCompound(IDS);
            if (compound != null && compound.hasTag(serverName, NBTType.NBTTagInt)) {
                return compound.getInteger(serverName);
            }
            return null;
        });
        boolean saveNbt = false;
        // 如果没有储存这个子服的地图 ID，新建一个地图
        if (id == null) {
            mapView = Bukkit.createMap(Bukkit.getWorlds().get(0));
            id = mapView.getId();
            saveNbt = true;
        } else {
            // 如果储存了这个子服的 ID，或者地图没有 MapView
            if (mapView == null || mapView.getId() != id) {
                MapView map = Bukkit.getMap(id);
                if (map == null) { // 如果服务器里没有这个地图，重新创建一个，重新储存 ID
                    mapView = Bukkit.createMap(Bukkit.getWorlds().get(0));
                    id = mapView.getId();
                    saveNbt = true;
                } else {
                    mapView = map;
                }
            }
        }
        // 清空 MapRenderer，然后添加新的 MapRenderer
        while (!mapView.getRenderers().isEmpty()) {
            mapView.removeRenderer(mapView.getRenderers().get(0));
        }
        mapView.addRenderer(new MyMapRenderer(bytes, cursors));
        mapMeta.setMapView(mapView);
        item.setItemMeta(mapMeta);

        // 如果地图 ID 有变动，保存 NBT
        if (saveNbt) {
            int finalId = id;
            NBT.modify(item, nbt -> {
                ReadWriteNBT compound = nbt.getOrCreateCompound(IDS);
                compound.setInteger(serverName, finalId);
            });
        }
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

    public static MapRenderer getRendererOrNull(MapMeta mapMeta) {
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
        if (renderer == null || renderer instanceof MyMapRenderer) return null;
        return VersionManager.getColors(renderer);
    }

    public static List<MapCursor> getCursors(Player player, MapMeta mapMeta) {
        MapRenderer renderer = getRendererOrNull(mapMeta);
        if (renderer == null || renderer instanceof MyMapRenderer) return null;
        return VersionManager.getCursors(player, renderer);
    }

}
