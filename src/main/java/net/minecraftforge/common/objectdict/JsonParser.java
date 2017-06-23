/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.minecraftforge.common.objectdict;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import net.minecraftforge.fml.common.registry.PersistentRegistryManager;
import scala.actors.threadpool.Arrays;

public class JsonParser
{
    private final String modId;
    

    public JsonParser(String modId)
    {
        this.modId = modId;
    }

    public String getModId()
    {
        return this.modId;
    }

    void parseObjectDicts(String dictModId, JsonObject json, Map<Class<?>, Map<ResourceLocation, ObjectDictionary.Builder>> typeMap)
    {
        for (Entry<String, JsonElement> elementEntry : json.entrySet())
        {
            String elementName = elementEntry.getKey();
            JsonElement element = elementEntry.getValue();
            
            if (!element.isJsonArray() && !element.isJsonObject())
            {
                throw new JsonSyntaxException(String.format("The element %s should be an array or object", elementName));
            }
            
            String[] path = elementName.split("/");
            ResourceLocation registryName = withModId(dictModId, path[0]);
            IForgeRegistry<?> registry = PersistentRegistryManager.findRegistryByName(registryName);
            
            if (registry == null)
            {
                throw new JsonSyntaxException(String.format("The registry %s does not exist", registryName));
            }
            
            Map<ResourceLocation, ObjectDictionary.Builder> builderMap = typeMap.computeIfAbsent(registry.getRegistrySuperType(), (superType) -> Maps.newHashMap());
            
            ResourceLocation dictKey = null;
            ObjectDictionary.Builder builder = null;
            
            if (path.length >= 2)
            {
                dictKey = withOptionalModId(dictModId, path[1]);
                builder = new ObjectDictionary.Builder();
            }
            
            if (element.isJsonArray())
            {
                if (path.length < 3)
                {
                    throw new JsonSyntaxException(String.format("The array %s should have a minimum of three forwardslashes", elementName));
                }
                
                JsonArray array = element.getAsJsonArray();
                
                if (array.size() > 0)
                {
                    ResourceLocation objectName = withModId(this.modId, Joiner.on("/").join(Arrays.copyOfRange(path, 2, path.length)));
                    Object object = registry.getValue(objectName);
                    
                    if (object == null)
                    {
                        throw new JsonSyntaxException(String.format("The object %s does not exist in registry %s", objectName, registryName));
                    }
                    
                    String[] tags = new String[array.size()];
                    
                    for (int i = 0; i < array.size(); i++)
                    {
                        JsonElement e = array.get(i);
                        
                        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString())
                        {
                            tags[i] = e.getAsString();
                        }
                        
                        else
                        {
                            throw new JsonSyntaxException(String.format("%s must be an array of strings", elementName));
                        }
                    }
                    
                    builder.add(object, tags);
                }
            }
            
            else if (element.isJsonObject())
            {
                
            }
            
            if (path.length >= 2)
            {
                if (builderMap.containsKey(dictKey))
                {
                    builderMap.get(dictKey).merge(builder);
                }
                
                else
                {
                    builderMap.put(dictKey, builder);
                }
            }
            /*if (entry.getValue().isJsonArray())
            {
                JsonArray array = entry.getValue().getAsJsonArray();
                if (array.size() > 0)
                {
                    boolean isEntryArray = this.isEntryArray(entry.getKey(), array);
                    String[] path = entry.getKey().split("/");
                    ResourceLocation registryName = null;
                    IForgeRegistry<?> registry = null;
                    try
                    {
                        registryName = this.getLocation(dictModId, path[0]);
                        registry = PersistentRegistryManager.findRegistryByName(registryName);
                        if (registry == null)
                            throw new JsonSyntaxException(String.format("Registry '%s' does not exist", registryName));
                    }
                    catch (JsonParseException e)
                    {
                        throw new JsonSyntaxException(String.format("Exception while trying to find registry for '%s'", entry.getKey()), e);
                    }
                    ObjectDictionary.Builder builder = new ObjectDictionary.Builder();
                    Deque<ResourceLocation> queue = Queues.newArrayDeque();
                    if (path.length > 1)
                    {
                        for (int i = 1; i < path.length; i++)
                        {
                            String s = path[i];
                            if (i == path.length - 1 && !isEntryArray)
                            {
                                ResourceLocation r = new ResourceLocation(s);
                                if (s.contains(":") && !r.getResourceDomain().equals(this.modId))
                                {
                                    String st = r.getResourcePath();
                                    throw new JsonSyntaxException(String.format("'%s' must either be '%s' or '%s'", s, st, String.format("%s:%s", this.modId, st)));
                                }
                            }
                            else
                            {
                                queue.addLast(s.contains(":") ? new ResourceLocation(s) : new ResourceLocation(dictModId, s));
                            }
                        }
                    }
                    else if (!isEntryArray)
                    {
                        throw new JsonSyntaxException(String.format("'%s' must be an array of json objects", entry.getKey()));
                    }
                    if (isEntryArray)
                    {
                        try
                        {
                            this.parseEntryArray(this.getBuilder(builder, queue), array, registry, registryName);
                        }
                        catch (JsonSyntaxException e)
                        {
                            throw new JsonSyntaxException(String.format("Exception while trying to parse json array '%s'", entry.getKey()));
                        }
                    }
                    else
                    {
                        try
                        {
                            this.parseTagArray(this.getBuilder(builder, queue), array, path[path.length - 1], registry, registryName);
                        }
                        catch (JsonSyntaxException e)
                        {
                            throw new JsonSyntaxException(String.format("Exception while trying to parse json array '%s'", entry.getKey()), e);
                        }
                    }
                    builderMap.computeIfAbsent(registry, (registry1) -> builder).merge(builder);
                }
            }*/
        }
    }
    
    boolean isEntryArray(String name, JsonArray array)
    {
        boolean isEntryArray = array.get(0).isJsonObject();
        if (Iterables.any(array, (element) -> isEntryArray ? !element.isJsonObject() : !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()))
        {
            throw new JsonSyntaxException(String.format("'%s' must be a uniform array of json objects or strings", name));
        }
        return isEntryArray;
    }
    
    void parseEntryArray(ObjectDictionary.Builder builder, JsonArray array, IForgeRegistry<?> registry, ResourceLocation registryName)
    {
        for (int i = 0; i < array.size(); i++)
        {
            JsonObject dictEntry = array.get(i).getAsJsonObject();
            JsonElement entryElement = dictEntry.get("entry");
            if (entryElement != null && entryElement.isJsonPrimitive() && entryElement.getAsJsonPrimitive().isString())
            {
                Object object = null;
                try
                {
                    ResourceLocation objectName = withModId(this.modId, entryElement.getAsString());
                    object = registry.getValue(objectName);
                    if (object == null)
                        throw new JsonSyntaxException(String.format("Object '%s' does not exist", objectName, registryName));
                }
                catch(JsonParseException e)
                {
                    throw new JsonSyntaxException(String.format("Exception while trying to find object for json element '%s' in registry '%s'", entryElement.getAsString(), registryName), e);
                }
                if (object != null && dictEntry.has("tags"))
                {
                    JsonElement tagsElement = dictEntry.get("tags");
                    if (tagsElement.isJsonArray())
                    {
                        JsonArray tags = tagsElement.getAsJsonArray();
                        if (tags.size() > 0)
                        {
                            for (int j = 0; j < tags.size(); j++)
                            {
                                JsonElement tagEntry = tags.get(j);
                                if (tagEntry.isJsonPrimitive() && tagEntry.getAsJsonPrimitive().isString())
                                {
                                    builder.add(object, tagEntry.getAsString());
                                }
                            }
                        }
                    }
                    else
                    {
                        throw new JsonSyntaxException(String.format("Json element 'tags' in json object %s in json array must be a json array", i));
                    }
                }
            }
            else
            {
                throw new JsonSyntaxException(String.format("Json object %s in array must contain an 'entry' string", i));
            }
        }
    }
    
    void parseTagArray(ObjectDictionary.Builder builder, JsonArray array, String objectPath, IForgeRegistry<?> registry, ResourceLocation registryName)
    {
        ResourceLocation objectName = withModId(this.modId, objectPath);
        Object object = registry.getValue(objectName);
        if (object == null)
            throw new JsonSyntaxException(String.format("Object '%s' does not exist", objectName, registryName));
        for (JsonElement element : array)
        {
            builder.add(object, element.getAsString());
        }
    }
    
    private static ResourceLocation withModId(String modId, String name)
    {
        ResourceLocation resource = new ResourceLocation(name);
        String resourceDomain = resource.getResourceDomain();
        if (modId.equals(resourceDomain)) 
        {
            return resource;
        }
        else
        {
            String resourcePath = resource.getResourcePath();
            if ("minecraft".equals(resourceDomain))
            {
                return new ResourceLocation(modId, resourcePath);
            }
            else
            {
                throw new JsonSyntaxException(String.format("Entry must either be '%s' or '%s'", resourcePath, String.format("%s:%s", modId, resourcePath)));
            }
        }
    }
    
    private static ResourceLocation withOptionalModId(String modId, String name)
    {
        if (name.contains(":"))
        {
            return new ResourceLocation(name);
        }
        else
        {
            return new ResourceLocation(modId, name);
        }
    }
}
