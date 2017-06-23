package net.minecraftforge.common.objectdict;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;

public class ObjectDictionary<I extends IForgeRegistryEntry<I>>
{
    static final BiMap<Class<?>, Map<ResourceLocation, ObjectDictionary<?>>> REGISTRYTODICTS = HashBiMap.<Class<?>, Map<ResourceLocation, ObjectDictionary<?>>>create();
    static final ObjectDictionary<?> EMPTY = new Builder().build();
    private final Map<I, Set<String>> objectToTags;
    private final Map<String, Set<I>> tagToObjects;
    
    ObjectDictionary(Map<I, Set<String>> objectToTags, Map<String, Set<I>> tagToObjects)
    {
        this.objectToTags = objectToTags;
        this.tagToObjects = tagToObjects;
    }

    @Nullable
    public I firstWithTags(String... tagArray)
    {
        Set<String> tagSet = Sets.newHashSet(tagArray);
        return firstWith((tagSet1) -> tagSet1.containsAll(tagSet));
    }
    
    public Set<I> withTags(String... tagArray)
    {
        if (tagArray.length == 1)
            return Sets.newHashSet(this.tagToObjects.get(tagArray[0]));
        Set<String> tagSet = Sets.newHashSet(tagArray);
        return with((tagSet1) -> tagSet1.containsAll(tagSet));
    }
    
    @Nullable
    public I firstWith(Predicate<Set<String>> predicate)
    {
        for (Entry<I, Set<String>> entry : this.objectToTags.entrySet())
            if (predicate.apply(entry.getValue()))
                return entry.getKey();
        return null;
    }
    
    public Set<I> with(Predicate<Set<String>> predicate)
    {
        Set<I> objectSet = Sets.newHashSet();
        for (Entry<I, Set<String>> entry : this.objectToTags.entrySet())
            if (predicate.apply(entry.getValue()))
                objectSet.add(entry.getKey());
        return objectSet;
    }
    
    public Set<I> getObjects()
    {
        return Sets.newHashSet(this.objectToTags.keySet());
    }
    
    public boolean hasObject(I object)
    {
        return this.objectToTags.containsKey(object);
    }
    
    public Set<String> getTags()
    {
        return Sets.newHashSet(this.tagToObjects.keySet());
    }
    
    public boolean hasTag(String tag)
    {
        return this.tagToObjects.containsKey(tag);
    }
    
    public Set<String> getTagsFor(I i)
    {
        return Sets.newHashSet(this.objectToTags.getOrDefault(i, Sets.newHashSet()));
    }
    
    public Set<I> getObjectsFor(String tag)
    {
        return Sets.newHashSet(this.tagToObjects.getOrDefault(tag, Sets.newHashSet()));
    }

    @SuppressWarnings("unchecked")
    public static <I extends IForgeRegistryEntry<I>> ObjectDictionary<I> getObjectDictionary(Class<I> c, ResourceLocation key)
    {
        return (ObjectDictionary<I>)REGISTRYTODICTS.getOrDefault(c, Collections.emptyMap()).getOrDefault(key, EMPTY);
    }
    
    static class Builder
    {
        final Map<Object, Set<String>> objectToTags = Maps.newHashMap();
        final Map<String, Set<Object>> tagToObjects = Maps.newHashMap();
        
        void add(Object object, String... tags)
        {
            for (String tag : tags)
            {
                this.objectToTags.computeIfAbsent(object, (object1) -> Sets.newHashSet()).add(tag);
                this.tagToObjects.computeIfAbsent(tag, (tag1) -> Sets.newHashSet()).add(object);
            }
        }
        
        void merge(Builder builder)
        {
            if (builder == this)
            {
                return;
            }
            for (Entry<Object, Set<String>> entry : builder.objectToTags.entrySet())
            {
                this.objectToTags.computeIfAbsent(entry.getKey(), (tagSet) -> Sets.newHashSet()).addAll(entry.getValue());
            }
            for (Entry<String, Set<Object>> entry : builder.tagToObjects.entrySet())
            {
                this.tagToObjects.computeIfAbsent(entry.getKey(), (objectSet) -> Sets.newHashSet()).addAll(entry.getValue());
            }
        }
        
        @SuppressWarnings("unchecked")
        <I extends IForgeRegistryEntry<I>> ObjectDictionary<I> build()
        {
            Map<I, Set<String>> objectToTags = Maps.<I, Set<String>>newHashMap();
            for (Entry<Object, Set<String>> entry : this.objectToTags.entrySet())
            {
                objectToTags.put((I)entry.getKey(), entry.getValue());
            }
            Map<String, Set<I>> tagToObjects = Maps.<String, Set<Object>, Set<I>>transformValues(this.tagToObjects, (objectsSet) -> Sets.<I>newHashSet(Iterables.<Object, I>transform(objectsSet, (object) -> (I)object)));
            return new ObjectDictionary<I>(objectToTags, tagToObjects);
        }
    }
}
