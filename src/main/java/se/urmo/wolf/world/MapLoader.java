package se.urmo.wolf.world;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MapLoader {
    public static MapData load(String resourcePath) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                MapLoader.class.getResourceAsStream("/" + resourcePath)) )) {

            List<String> lines = br.lines().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            int h = lines.size();
            int w = lines.get(0).length();
            int[][] grid = new int[h][w];

            double px = 1.5, py = 1.5;
            List<MapData.Spawn> spawns = new ArrayList<>();

            for (int y=0;y<h;y++) {
                String line = lines.get(y);
                for (int x=0;x<w;x++) {
                    char c = line.charAt(x);
                    switch (c) {
                        case '1': grid[y][x] = 1; break; // wall
                        case 'D': grid[y][x] = 2; break; // door
                        case 'P': px = x + 0.5; py = y + 0.5; grid[y][x]=0; break;
                        case 'G': spawns.add(new MapData.Spawn(MapData.Spawn.Type.GUARD, x+0.5, y+0.5)); grid[y][x]=0; break;
                        default: grid[y][x] = 0; break;
                    }
                }
            }

            GameMap map = new GameMap(w, h, grid);
            MapData data = new MapData(map, px, py);
            data.spawns.addAll(spawns);
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load map: " + resourcePath, e);
        }
    }
}