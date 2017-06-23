package net.minecraftforge.common.objectdict;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

public class ObjectDictionaryManager
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    public static void loadObjectDicts()
    {
        ObjectDictionary.REGISTRYTODICTS.clear();
        Loader.instance().getActiveModList().forEach((mod) -> loadObjectDictsFromMod(mod));
    }
    
    private static boolean loadObjectDictsFromMod(ModContainer mod)
    {
        Map<Class<?>, Map<ResourceLocation, ObjectDictionary.Builder>> builderMap = Maps.newHashMap();
        if ("minecraft".equals(mod.getModId()))
        {
            return true;
        }
        else
        {
            FileSystem fs = null;
            BufferedReader reader = null;
            try
            {
                JsonParser parser = new JsonParser(mod.getModId());
                File source = mod.getSource();
                Path root = null;
                if (source.isFile())
                {
                    try
                    {
                        fs = FileSystems.newFileSystem(source.toPath(), null);
                        root = fs.getPath("/assets/" + parser.getModId() + "/objectdicts/");
                    }
                    catch (IOException e)
                    {
                        FMLLog.log(Level.ERROR, e, "Error loading FileSystem from jar: " + e.toString());
                        return false;
                    }
                }
                else if (source.isDirectory())
                {
                    root = source.toPath().resolve("assets/" + parser.getModId() + "/objectdicts/");
                }
                if (root == null || !Files.exists(root))
                {
                    return false;
                }
                Iterator<Path> itr = null;
                try
                {
                    itr = Files.walk(root).iterator();
                }
                catch (IOException e)
                {
                    FMLLog.log(Level.ERROR, e, "Error iterating recipes for: " + parser.getModId());
                    return false;
                }
                Loader.instance().setActiveModContainer(mod);
                while (itr != null && itr.hasNext())
                {
                    Path f = itr.next();
                    if (!"json".equals(FilenameUtils.getExtension(f.toString())))
                    {
                        continue;
                    }
                    String name = FilenameUtils.removeExtension(root.relativize(f).toString()).replaceAll("\\\\", "/");
                    IOUtils.closeQuietly(reader);
                    try
                    {
                        reader = Files.newBufferedReader(f);
                        JsonObject json = JsonUtils.func_193839_a(GSON, reader, JsonObject.class);
                        parser.parseObjectDicts(name, json, builderMap);
                    }
                    catch (JsonParseException e)
                    {
                        FMLLog.log(Level.ERROR, e, String.format("Parsing error loading object dictionaries from '%s'", parser.getModId()));
                        return false;
                    }
                    catch (IOException e)
                    {
                        FMLLog.log(Level.ERROR, e, String.format("Couldn't read object dictionaries from '%s'", parser.getModId()));
                        return false;
                    }
                }
            }
            finally
            {
                IOUtils.closeQuietly(fs);
                IOUtils.closeQuietly(reader);
            }                
        }
        for (Entry<Class<?>, Map<ResourceLocation, ObjectDictionary.Builder>> entry : builderMap.entrySet())
        {
            Map<ResourceLocation, ObjectDictionary<?>> map = Maps.transformValues(entry.getValue(), (builder) -> builder.build());
            ObjectDictionary.REGISTRYTODICTS.put(entry.getKey(), map);
        }
        return true;
    }
    
    private static void putMinecraftObjectDicts()
    {
        
    }
}
