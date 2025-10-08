package se.urmo.wolf.world;

import java.util.ArrayList;
import java.util.List;

public class MapData {
    public final GameMap map;
    public final double playerStartX, playerStartY;
    public final List<Spawn> spawns = new ArrayList<>();

    public MapData(GameMap map, double px, double py) {
        this.map = map; this.playerStartX = px; this.playerStartY = py;
    }

    public static class Spawn {
        public enum Type { GUARD }
        public final Type type;
        public final double x, y;
        public Spawn(Type type, double x, double y) { this.type=type; this.x=x; this.y=y; }
    }
}