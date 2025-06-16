package logic;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * TileLoader is responsible for loading tile patterns from a JSON file.
 * The JSON file should be located in the resources directory of the project.
 * It uses the Gson library to parse the JSON data into a list of tile patterns.
 */
public class TileLoader {
    private static final String TILES_RESOURCE_PATH = "tiles.json";

    public static List<String> loadTilePatterns() {
        Gson gson = new Gson();
        try (InputStream inputStream = TileLoader.class.getResourceAsStream(TILES_RESOURCE_PATH)) {
            if (inputStream == null) {
                System.err.println("TileLoader Error: Cannot find resource file at " + TILES_RESOURCE_PATH);
                return Collections.emptyList();
            }
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                TilesDefinition definition = gson.fromJson(reader, TilesDefinition.class);
                if (definition != null && definition.getTiles() != null) {
                    return definition.getTiles();
                }
                else {
                    System.err.println("TileLoader Error: Failed to parse tiles from JSON or" +
                            " JSON structure is incorrect in " + TILES_RESOURCE_PATH);
                }
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            System.err.println("TileLoader Error: JSON syntax error in " + TILES_RESOURCE_PATH);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("TileLoader Error: Could not load or read tile patterns from " + TILES_RESOURCE_PATH);
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
