package com.mynamesraph.mystcraft.worldgen

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.logging.LogUtils
import com.mojang.serialization.JsonOps
import com.mynamesraph.mystcraft.Mystcraft
import com.mynamesraph.mystcraft.data.saved.DimensionIdentificatorCounter
import com.mynamesraph.mystcraft.data.saved.NoMobDimensions
import net.commoble.infiniverse.api.InfiniverseAPI
import net.minecraft.SharedConstants
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.dimension.LevelStem
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.storage.LevelResource
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Imports feed the existing Infiniverse registry and counter, not a parallel Age registry. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object AgeImporter {
    private val logger = LogUtils.getLogger()
    private val jobs = ConcurrentHashMap<MinecraftServer, Job>()
    private val agePattern = Regex("age_([1-9][0-9]*)")
    private const val MAX_JSON_BYTES = 16L * 1024 * 1024
    private const val MAX_NBT_BYTES = 64L * 1024 * 1024

    private class Job {
        val cancelled = AtomicBoolean(false)
        val staging = AtomicReference<Path?>(null)
        @Volatile var worker: Thread? = null
    }

    private data class Metadata(
        val stem: LevelStem, val preferredId: Int?, val sourceSeed: Long?, val noMobs: Boolean
    )

    /** Called only by the permission-level-2 command. Large copies never block the server tick. */
    fun start(source: CommandSourceStack, folderName: String): Int {
        val server = source.server
        val worldRoot: Path
        val input: Path
        try {
            AgeImportFiles.validateName(folderName)
            worldRoot = server.getWorldPath(LevelResource.ROOT).toRealPath()
            input = AgeImportFiles.source(worldRoot, folderName)
        } catch (e: Exception) {
            source.sendFailure(Component.literal("Age import rejected: ${e.message}"))
            return 0
        }
        val job = Job()
        if (jobs.putIfAbsent(server, job) != null) {
            source.sendFailure(Component.literal("An Age import is already in progress. Wait for its completion message."))
            return 0
        }
        source.sendSuccess({ Component.literal("Validating and copying '$folderName'. The source folder will be left untouched.") }, false)
        val worker = Thread({
            try {
                val prepared = AgeImportFiles.prepare(input) { job.cancelled.get() }
                job.staging.set(prepared.staging)
                if (job.cancelled.get() || !server.isRunning) {
                    cleanup(job)
                    jobs.remove(server, job)
                } else {
                    server.execute {
                        if (job.cancelled.get() || !server.isRunning || jobs[server] !== job) {
                            cleanup(job)
                            jobs.remove(server, job)
                        } else activate(source, folderName, worldRoot, prepared, job)
                    }
                }
            } catch (e: Exception) {
                cleanup(job)
                jobs.remove(server, job)
                if (!job.cancelled.get()) {
                    logger.error("Mystcraft Age import failed for {}", folderName, e)
                    if (server.isRunning) server.execute {
                        source.sendFailure(Component.literal("Age import failed: ${e.message}. The source is unchanged."))
                    }
                }
            }
        }, "Mystcraft Age Import")
        worker.isDaemon = true
        job.worker = worker
        worker.start()
        return 1 // Accepted/queued, not a claim that the import has finished.
    }

    private fun activate(
        source: CommandSourceStack, folderName: String, worldRoot: Path,
        prepared: AgeImportFiles.Prepared, job: Job
    ) {
        val server = source.server
        var published: Path? = null
        try {
            // Registry-backed codec access and every registry/counter mutation occur on the server thread.
            val metadata = readMetadata(server, folderName, prepared.staging)
            val counter = DimensionIdentificatorCounter.get(server)
            val age = counter.nextAvailable(server, metadata.preferredId)
            val location = DimensionIdentificatorCounter.location(age)
            val key = ResourceKey.create(Registries.DIMENSION, location)

            // Ensure the imported definition is serializable *before* publishing the directory.
            val ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess())
            val definition = LevelStem.CODEC.encodeStart(ops, metadata.stem)
                .getOrThrow { IllegalArgumentException("The Age definition cannot be saved: $it") }.asJsonObject
            definition.addProperty("dimension_id", location.toString())
            definition.addProperty("seed", server.overworld().seed)
            definition.addProperty("data_version", SharedConstants.getCurrentVersion().dataVersion.version)
            definition.addProperty("no_mobs", metadata.noMobs)
            Files.writeString(
                prepared.staging.resolve("dimension.json"),
                GsonBuilder().setPrettyPrinting().create().toJson(definition) + "\n"
            )
            // A copied source level.dat is metadata only; it must never become this server's world data.
            for (name in listOf("level.dat", "level.dat_old", "session.lock")) {
                Files.deleteIfExists(prepared.staging.resolve(name))
            }

            published = AgeImportFiles.publish(worldRoot, Mystcraft.MOD_ID, age, prepared)
            job.staging.set(null) // Never clean up a published dimension, even on registration failure.
            counter.advancePast(age)
            if (metadata.noMobs) rememberNoMobs(server, location)

            // The same supported API used by DescriptiveBookItem. No reflection, unfreezing, or
            // manual writes to level.dat. Infiniverse persists this LevelStem with native Ages.
            InfiniverseAPI.get().getOrCreateLevel(server, key) { metadata.stem }
            server.saveEverything(false, true, true)

            source.sendSuccess({
                Component.literal("Imported '$folderName' as Age $age ($location): ${prepared.chunks} stored chunks. " +
                    "The Age is available now; no restart is required. The original import folder is unchanged.")
            }, true)
            if (metadata.sourceSeed == null || metadata.sourceSeed != server.overworld().seed) {
                source.sendSystemMessage(Component.literal(
                    "Import note: Infiniverse uses this server's seed for future generation. " +
                        (if (metadata.sourceSeed == null) "The source seed was not supplied. " else "The source seed differs. ") +
                        "Existing chunks are retained, but newly explored terrain may have seams."
                ))
            }
            source.sendSystemMessage(Component.literal(
                "The imported dimension is $location. Visit it with /execute in $location run tp ...; " +
                    "craft linking books there normally. No existing books were retargeted."
            ))
            logger.info("Imported Mystcraft Age {} from {} ({} files, {} bytes)", location, folderName, prepared.files, prepared.bytes)
        } catch (e: Exception) {
            logger.error("Could not activate imported Mystcraft Age from {}", folderName, e)
            if (published == null) {
                source.sendFailure(Component.literal("Age import rejected: ${e.message}. No Age was registered; the source is unchanged."))
            } else {
                // Infiniverse/other mods may have opened files or partly registered the level.
                // Deleting/rolling back its directory at this point would risk corrupting live data.
                source.sendFailure(Component.literal(
                    "The Age was copied to $published, but registration or saving failed: ${e.message}. " +
                        "The copied data and reserved ID were retained for recovery. Check the server log before retrying; " +
                        "the source folder is unchanged."
                ))
            }
        } finally {
            cleanup(job)
            jobs.remove(server, job)
        }
    }

    private fun readMetadata(server: MinecraftServer, folderName: String, folder: Path): Metadata {
        val jsonPath = folder.resolve("dimension.json")
        val document = if (Files.exists(jsonPath, NOFOLLOW_LINKS)) {
            require(Files.isRegularFile(jsonPath, NOFOLLOW_LINKS) && Files.size(jsonPath) <= MAX_JSON_BYTES) {
                "dimension.json is not a regular file or exceeds 16 MiB."
            }
            val text = Files.readString(jsonPath)
            checkJsonDepth(text)
            val parsed = JsonParser.parseString(text)
            require(parsed.isJsonObject) { "dimension.json must contain a JSON object." }
            parsed.asJsonObject
        } else JsonObject()

        val folderAge = agePattern.matchEntire(folderName)?.groupValues?.get(1)?.let {
            it.toIntOrNull()?.takeIf { number -> number in 1 until Int.MAX_VALUE }
                ?: throw IllegalArgumentException("Age number in the folder name is out of range.")
        }
        val statedId = optionalString(document, "dimension_id")?.let(::mystcraftAgeNumber)
        require(statedId == null || folderAge == null || statedId == folderAge) {
            "The folder name and dimension.json identify different Mystcraft Ages."
        }
        var preferred = statedId ?: folderAge
        val sourceId = optionalString(document, "source_dimension")?.let {
            ResourceLocation.tryParse(it) ?: throw IllegalArgumentException("Invalid source_dimension: $it")
        }
        if (sourceId?.namespace == Mystcraft.MOD_ID && agePattern.matches(sourceId.path)) {
            val sourceAge = mystcraftAgeNumber(sourceId.toString())
            require(preferred == null || preferred == sourceAge) { "Conflicting source and target Age identities." }
            preferred = sourceAge
        }

        val donor = readDonorData(folder.resolve("level.dat"))
        val currentVersion = SharedConstants.getCurrentVersion().dataVersion.version
        val jsonVersion = optionalLong(document, "data_version")
        require(jsonVersion == null || jsonVersion <= currentVersion) { "The dimension was saved by a newer Minecraft version." }
        if (donor != null && donor.contains("DataVersion", Tag.TAG_INT.toInt())) {
            require(donor.getInt("DataVersion") <= currentVersion) { "The donor level.dat is from a newer Minecraft version." }
        }

        val hasType = document.has("type")
        val hasGenerator = document.has("generator")
        require(hasType == hasGenerator) { "dimension.json must supply both 'type' and 'generator'." }
        val stem: LevelStem = if (hasType) {
            LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()), document)
                .getOrThrow { IllegalArgumentException("Invalid or unavailable dimension settings: $it") }
        } else {
            require(donor != null) { "Supply the original type/generator in dimension.json, or the donor world's level.dat." }
            val dimensions = donor.getCompound("WorldGenSettings").getCompound("dimensions")
            val candidates = dimensions.allKeys.filter {
                val id = ResourceLocation.tryParse(it)
                id != null && id.namespace == Mystcraft.MOD_ID && agePattern.matches(id.path)
            }
            val selected = sourceId?.toString()
                ?: preferred?.let { DimensionIdentificatorCounter.location(it).toString() }
                ?: when (folderName) {
                    "DIM-1" -> "minecraft:the_nether"
                    "DIM1" -> "minecraft:the_end"
                    else -> candidates.singleOrNull()
                }
                ?: throw IllegalArgumentException(
                    "The donor contains zero or multiple possible Ages. Name the folder age_<number>, " +
                        "or add dimension.json with a 'source_dimension' field to select the donor dimension."
                )
            require(dimensions.contains(selected, Tag.TAG_COMPOUND.toInt())) { "The donor level.dat has no dimension '$selected'." }
            val selectedId = ResourceLocation.parse(selected)
            if (selectedId.namespace == Mystcraft.MOD_ID && agePattern.matches(selectedId.path)) {
                val number = mystcraftAgeNumber(selected)
                require(preferred == null || preferred == number) { "The donor and requested Age identities conflict." }
                preferred = number
            }
            LevelStem.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, server.registryAccess()), dimensions.getCompound(selected))
                .getOrThrow { IllegalArgumentException("Cannot restore the donor dimension (missing mods/datapacks?): $it") }
        }

        // Preserve the recently fixed isolation rule: never tag imported Ages as the registered
        // overworld DimensionType. Copy the actual type's value, not a replacement/default type.
        val isolated = LevelStem(Holder.direct(stem.type().value()), stem.generator())
        val sourceSeed = optionalLong(document, "seed") ?: donor?.getCompound("WorldGenSettings")?.let {
            if (it.contains("seed", Tag.TAG_LONG.toInt())) it.getLong("seed") else null
        }
        val noMobs = if (document.has("no_mobs")) {
            val value = document.get("no_mobs")
            require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) { "no_mobs must be a boolean." }
            value.asBoolean
        } else false
        return Metadata(isolated, preferred, sourceSeed, noMobs)
    }

    private fun readDonorData(path: Path): CompoundTag? {
        if (!Files.exists(path, NOFOLLOW_LINKS)) return null
        require(Files.isRegularFile(path, NOFOLLOW_LINKS) && Files.size(path) <= MAX_NBT_BYTES) {
            "The donor level.dat is not a regular file or exceeds 64 MiB."
        }
        val root = NbtIo.readCompressed(path, NbtAccounter.create(MAX_NBT_BYTES))
        require(root.contains("Data", Tag.TAG_COMPOUND.toInt())) { "Invalid donor level.dat: missing Data compound." }
        return root.getCompound("Data")
    }

    private fun mystcraftAgeNumber(text: String): Int {
        val id = ResourceLocation.tryParse(text)
        require(id != null && id.namespace == Mystcraft.MOD_ID) { "dimension_id must use the ${Mystcraft.MOD_ID} namespace." }
        val number = agePattern.matchEntire(id.path)?.groupValues?.get(1)?.toIntOrNull()
        require(number != null && number in 1 until Int.MAX_VALUE) { "Invalid Mystcraft Age ID: $text" }
        return number
    }

    private fun optionalString(document: JsonObject, key: String): String? {
        if (!document.has(key)) return null
        val value = document.get(key)
        require(value.isJsonPrimitive && value.asJsonPrimitive.isString) { "$key must be a string." }
        return value.asString
    }

    private fun optionalLong(document: JsonObject, key: String): Long? {
        if (!document.has(key)) return null
        val value = document.get(key)
        require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber) { "$key must be an integer." }
        return value.asString.toLongOrNull() ?: throw IllegalArgumentException("Invalid integer for $key.")
    }

    private fun checkJsonDepth(text: String) {
        var depth = 0
        var quoted = false
        var escaped = false
        for (c in text) {
            if (quoted) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') quoted = false
            } else when (c) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 192) { "dimension.json is excessively nested; check for corrupt worldgen data." } }
                '}', ']' -> depth--
            }
        }
    }

    private fun rememberNoMobs(server: MinecraftServer, id: ResourceLocation) {
        val data = server.overworld().dataStorage.computeIfAbsent(
            SavedData.Factory({ NoMobDimensions() }, { tag, _ ->
                NoMobDimensions().also { saved ->
                    val list = tag.getList("dimensions", Tag.TAG_STRING.toInt())
                    for (i in 0 until list.size) ResourceLocation.tryParse(list.getString(i))?.let { saved.dimensions.add(it) }
                }
            }), NoMobDimensions.FILE_NAME
        )
        data.add(id)
    }

    @SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        val job = jobs.remove(event.server) ?: return
        job.cancelled.set(true)
        job.worker?.interrupt()
        cleanup(job)
    }

    private fun cleanup(job: Job) {
        val staging = job.staging.getAndSet(null) ?: return
        val thread = Thread({
            try { AgeImportFiles.deleteStaging(staging) }
            catch (e: Exception) { logger.warn("Could not remove import staging folder {}; it may be removed with the server stopped", staging, e) }
        }, "Mystcraft Import Cleanup")
        thread.isDaemon = true
        thread.start()
    }
}
