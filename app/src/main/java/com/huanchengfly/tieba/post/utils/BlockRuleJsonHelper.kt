package com.huanchengfly.tieba.post.utils

import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Block.Companion.getKeywords
import com.huanchengfly.tieba.post.utils.BlockManager.BlockRuleDto

object BlockRuleJsonHelper {
    fun exportRulesJson(blockList: List<Block>): String {
        val dtos = blockList.map { block ->
            BlockRuleDto(
                category = block.category,
                type = block.type,
                keywords = if (block.type == Block.TYPE_KEYWORD) block.getKeywords() else null,
                username = block.username,
                uid = block.uid,
                pattern = block.pattern,
                minLevel = if (block.minLevel > 0) block.minLevel else null,
                ipRegion = block.ipRegion,
                forum = block.forum,
            )
        }
        return GsonUtil.getGson().toJson(dtos)
    }

    fun isDuplicateRule(existingList: List<Block>, newBlock: Block): Boolean {
        return existingList.any { existing ->
            existing.category == newBlock.category &&
            existing.type == newBlock.type &&
            existing.forum == newBlock.forum &&
            when (newBlock.type) {
                Block.TYPE_KEYWORD -> existing.keywords == newBlock.keywords
                Block.TYPE_USER -> (existing.uid != null && existing.uid == newBlock.uid) ||
                        (!existing.username.isNullOrBlank() && existing.username == newBlock.username)
                Block.TYPE_REGEX -> existing.pattern == newBlock.pattern
                Block.TYPE_USER_LEVEL -> existing.minLevel == newBlock.minLevel
                Block.TYPE_IP_REGION -> existing.ipRegion?.trim().equals(newBlock.ipRegion?.trim(), ignoreCase = true)
                else -> false
            }
        }
    }

    fun isDuplicateBetween(a: Block, b: Block): Boolean {
        if (a.category != b.category || a.type != b.type || a.forum != b.forum) return false
        return when (a.type) {
            Block.TYPE_KEYWORD -> a.keywords == b.keywords
            Block.TYPE_USER -> (a.uid != null && a.uid == b.uid) ||
                    (!a.username.isNullOrBlank() && a.username == b.username)
            Block.TYPE_REGEX -> a.pattern == b.pattern
            Block.TYPE_USER_LEVEL -> a.minLevel == b.minLevel
            Block.TYPE_IP_REGION -> a.ipRegion?.trim().equals(b.ipRegion?.trim(), ignoreCase = true)
            else -> false
        }
    }

    fun parseRulesFromJson(json: String, existingList: List<Block>): List<Block> {
        if (json.isBlank() || json.length > 500_000) return emptyList()
        val gson = GsonUtil.getGson()
        val type = object : TypeToken<List<BlockRuleDto>>() {}.type
        val importedList: List<BlockRuleDto> = runCatching {
            gson.fromJson<List<BlockRuleDto>>(json, type)
        }.getOrNull() ?: emptyList()

        if (importedList.isEmpty()) return emptyList()

        val validList = importedList.take(500)
        val blocksToSave = mutableListOf<Block>()
        for (dto in validList) {
            val category = dto.category.takeIf { it == Block.CATEGORY_BLACK_LIST || it == Block.CATEGORY_WHITE_LIST } ?: continue
            val ruleType = dto.type.takeIf { it in Block.TYPE_KEYWORD..Block.TYPE_IP_REGION } ?: continue

            if (ruleType == Block.TYPE_REGEX) {
                val p = dto.pattern?.trim()
                if (p.isNullOrBlank() || p.length > 200 || BlockFilterUtil.isDangerousRegexPattern(p) || runCatching { Regex(p) }.isFailure) {
                    continue
                }
            }

            val block = Block(
                category = category,
                type = ruleType,
                keywords = dto.keywords?.let { gson.toJson(it) },
                username = dto.username?.trim(),
                uid = dto.uid?.trim(),
                pattern = dto.pattern?.trim(),
                minLevel = dto.minLevel ?: 0,
                ipRegion = dto.ipRegion?.trim(),
                forum = dto.forum?.trim(),
            )

            if (isDuplicateRule(existingList, block) || blocksToSave.any { isDuplicateBetween(it, block) }) continue

            blocksToSave.add(block)
        }
        return blocksToSave
    }
}
