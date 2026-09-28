package com.mynamesraph.mystcraft.block.portal

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

/**
 * Find all corner offsets of a free-form block frame
 *
 * @return A List containing the positions of each corners of the frame. If the frame is not valid, returns null
 */
fun solveFrameCorners(origin:BlockPos, validBlocks: Set<Block>, level: Level, maxBlockCount: Int): Result<List<BlockPos>> {
    val allValidCorners = mutableListOf<BlockPos>()
    val blockCounter = Counter(maxBlockCount)

    //println("solveFrameCorners: Solving...")
    runCatching {

        //TODO: handle properly instead of throwing
        for (direction in findAllValidNeighbours(origin,validBlocks,level).getOrThrow()) {
            //println("solveFrameCorners: Going ${direction.name}")

            val result = validateFrameInDirection(
                origin.relative(direction),
                validBlocks,
                level,
                direction,
                origin,
                mutableListOf(),
                blockCounter
            )

            //println("solveFrameCorners: $result")
            if (result.isSuccess) {
                var validCorners = result.getOrThrow()

                allValidCorners.addAll(result.getOrThrow()) // This should NEVER throw
            }
            else {
                //println("solveFrameCorners: ${result.exceptionOrNull()!!.message}")
            }
        }
    }

    if (allValidCorners.isNotEmpty()) return Result.success(allValidCorners.distinct())
    return Result.failure(Exception("No valid paths were found"))
}

private fun validateFrameInDirection(
    pos: BlockPos,
    validBlocks: Set<Block>,
    level: Level,
    direction: Direction,
    origin: BlockPos,
    corners: MutableList<BlockPos>,
    counter: Counter
): Result<List<BlockPos>> {

    // go in the direction and look for the first branch
    val branchR = findNextBranch(pos,validBlocks,level,direction,origin,counter)
    if (branchR.isSuccess) {
        val branchPos = branchR.getOrThrow().first
        val branchDirections = branchR.getOrThrow().second

        corners.add(branchPos) // add the block as a corner

        //TODO: figure out why only the first direction works
        for (branchDirection in branchDirections) {
            //println("ValidateFrameInDirection: validating branch at $branchPos going $direction")
            return validateFrameInDirection(branchPos.relative(branchDirection),validBlocks,level,branchDirection,origin,corners,counter)
        }
    }
    else {
        when (branchR.exceptionOrNull()!!.message) {
            "The origin was encountered" -> return Result.success(corners)
            "The line ends before branching" -> return Result.failure(Exception("Frame was not valid because no more branches were found"))
            else -> {} // keep looking for the next branch
        }
    }

    return Result.failure(IllegalStateException())
    /*println("validateFrameInDirection: finding end of line from $pos going $direction")
    val endOfLineR = findEndOfLine(pos,validBlocks,level,direction,origin,counter)

    if (endOfLineR.isFailure) {
        when (endOfLineR.exceptionOrNull()!!.message) {
            "The end of this line could not be reached as the origin block was encountered." -> {
                println("validateFrameInDirection: ${endOfLineR.exceptionOrNull()!!.message}")
                return Result.success(corners) // Reached the end of the loop, congrats
            }
            "Too many blocks!" -> {
                println("validateFrameInDirection:${endOfLineR.exceptionOrNull()!!.message}")
                return Result.failure(endOfLineR.exceptionOrNull()!!)
            }
            else -> {
                println("validateFrameInDirection: End of line encountered an unhandled exception!")
                return Result.failure(endOfLineR.exceptionOrNull()!!)
            }
        }
    }

    val endOfLine = endOfLineR.getOrThrow() // this should NEVER throw
    println("validateFrameInDirection: line ends at $endOfLine, ${level.getBlockState(endOfLine)}")
    println("validateFrameInDirection: Checking if $endOfLine is a corner")


    val neighbours = findAllValidNeighbours(endOfLine,validBlocks,level, notIncluded = setOf(direction.opposite))

    if (neighbours.isSuccess) {
        println("validateFrameInDirection: ${level.getBlockState(endOfLine)} at $endOfLine was a corner!")
        corners.add(endOfLine)
        println("validateFrameInDirection: corners = $corners")
        val frameValid = validateFrameInDirection(endOfLine,validBlocks,level,direction,origin,corners,counter)

        return if (frameValid.isSuccess) {
            Result.success(frameValid.getOrThrow()) // this should NEVER throw
        } else {
            Result.failure(frameValid.exceptionOrNull()!!)
        }
    }
    else {
        println("validateFrameInDirection: ${level.getBlockState(endOfLine)} at $endOfLine was a not corner!")
    }
    return Result.failure(Exception("No valid frames were found in the direction"))*/
}


/**
 * Finds the last valid block in a line
 * @return position of the block at the end of the line or null if the origin was reached
 */
private fun findEndOfLine(pos: BlockPos, validBlocks: Set<Block>, level: Level,direction: Direction, origin: BlockPos,counter: Counter): Result<BlockPos> {
    // TODO: Should fail if chunks are not loaded, see Level#hasChunkAt(BlockPos)

    var currentPos = pos.relative(direction)

    var bs = level.getBlockState(currentPos)
    println("endOfLine: $bs at $currentPos")


    while (validBlocks.contains(bs.block)) {

        counter.count++
        if (counter.maxxed()) return Result.failure(Exception("Too many blocks!"))

        if (currentPos == origin) {
            println("endOfLine: Congrats you looped back to the origin")
            return Result.failure(Exception("endOfLine: The end of this line could not be reached as the origin block was encountered."))
        }
        bs = level.getBlockState(currentPos.relative(direction))
        println("endOfLine: $bs at $currentPos")
        if (validBlocks.contains(bs.block)) {
            currentPos = currentPos.relative(direction)
        }
    }
    return Result.success(currentPos.relative(direction.opposite))
}

private fun findNextBranch(pos: BlockPos, validBlocks: Set<Block>, level: Level,direction: Direction,origin: BlockPos,counter: Counter): Result<Pair<BlockPos,List<Direction>>> {

    // Check if the block at the current position branches out
    var currentPos = pos
    var foundBranch = findAllValidNeighbours(currentPos,validBlocks,level, notIncluded = setOf(direction,direction.opposite))
    while (foundBranch.isFailure) {
        when (foundBranch.exceptionOrNull()!!.message) {
            "No valid neighbours" -> {}
            else -> {
                //println("findNextBranch: findAllValidNeighbours encountered unhandled exception -> ${foundBranch.exceptionOrNull()!!.message}")
                return Result.failure(foundBranch.exceptionOrNull()!!)
            }
        }

        if (currentPos == origin) {
            return Result.failure(Exception("The origin was encountered"))
        }

        // if there is still a block in the line,
        if (isNeighbourValid(currentPos,validBlocks,level,direction)) {
            //Check if the next block branches
            currentPos = currentPos.relative(direction)
            foundBranch = findAllValidNeighbours(currentPos,validBlocks,level, notIncluded = setOf(direction,direction.opposite))
        }
        else {
            return Result.failure(Exception("The line ends before branching"))
        }
    }

    // Last checked position is a branch
    //println("findNextBranch: found a branch starting at $currentPos with ${level.getBlockState(currentPos)}")
    return Result.success(Pair(currentPos,foundBranch.getOrThrow()))
}

private fun isNeighbourValid(pos: BlockPos, validBlocks: Set<Block>, level: Level, direction: Direction): Boolean {
    // TODO: Should fail if chunks are not loaded, see Level#hasChunkAt(BlockPos)
    if (validBlocks.contains(level.getBlockState(pos.relative(direction)).block)) {
        //println("checkNeighbour: $direction is valid")
        return true
    }
    //println("checkNeighbour: $direction is invalid")
    return false
}

/**
 * Check the block's neighbours for validBlocks
 *
 * @return A List of directions where validBlocks were found. Will be empty if none were found.
 */
private fun findAllValidNeighbours(pos: BlockPos, validBlocks: Set<Block>, level: Level, notIncluded:Set<Direction>? = null): Result<List<Direction>> {
    // TODO: Should fail if chunks are not loaded, see Level#hasChunkAt(BlockPos)
    val validNeighbours = mutableListOf<Direction>()
    for (direction in Direction.entries) {
        if (notIncluded != null) {
            if (notIncluded.contains(direction)) continue
        }


        if (isNeighbourValid(pos,validBlocks,level,direction)) {
            validNeighbours.add(direction)
        }
    }

    if (validNeighbours.isEmpty()) {
        //println("findAllValidNeighbours: no valid neighbours for $pos")
        return Result.failure(Exception("No valid neighbours"))
    }

    //println("findAllValidNeighbours: valid neighbours for $pos : $validNeighbours")
    return Result.success(validNeighbours)
}

/**
 * This seems slightly over-engineered, but I don't see how you could easily flood-fill a bent portal
 */
fun findAllPointsInsidePolygon(vertices: List<BlockPos>) : List<BlockPos> {
    val bounds = getPolygonBounds(vertices)
    val boundMin = bounds.first
    val boundMax = bounds.second

    val validPoints = mutableListOf<BlockPos>()

    for (x in boundMin.x..boundMax.x) {
        for (y in boundMin.y..boundMax.y) {
            for (z in boundMin.z..boundMax.z) {
                validPoints.add(BlockPos(x,y,z))
            }
        }
    }

    return validPoints
}


private fun getPolygonBounds(vertices: List<BlockPos>): Pair<BlockPos,BlockPos> {
    val minX = vertices.minOf { it.x }
    val minY = vertices.minOf { it.y }
    val minZ = vertices.minOf { it.z }
    val min = BlockPos(minX,minY,minZ)

    val maxX = vertices.maxOf { it.x }
    val maxY = vertices.maxOf { it.y }
    val maxZ = vertices.maxOf { it.z }
    val max = BlockPos(maxX,maxY,maxZ)

    return Pair(min,max)
}

private class Counter(val maxCount: Int) {
    var count = 0

    fun maxxed(): Boolean {
        return count > maxCount
    }
}