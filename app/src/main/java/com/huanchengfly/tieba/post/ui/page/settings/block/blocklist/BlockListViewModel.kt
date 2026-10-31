package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.*
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.utils.BlockManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class BlockListViewModel :
    BaseViewModel<BlockListUiIntent, BlockListPartialChange, BlockListUiState, BlockListUiEvent>() {
    override fun createInitialState(): BlockListUiState = BlockListUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<BlockListUiIntent, BlockListPartialChange, BlockListUiState> =
        BlockSettingsPartialChangeProducer

    override fun dispatchEvent(partialChange: BlockListPartialChange): UiEvent? =
        when (partialChange) {
            is BlockListPartialChange.Load.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is BlockListPartialChange.Add.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is BlockListPartialChange.Delete.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is BlockListPartialChange.Import.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is BlockListPartialChange.Add.Success -> BlockListUiEvent.Success.Add
            is BlockListPartialChange.Delete.Success -> BlockListUiEvent.Success.Delete
            is BlockListPartialChange.Import.Success -> BlockListUiEvent.Success.Import(partialChange.count)
            is BlockListPartialChange.Clear.Success -> BlockListUiEvent.Success.Clear
            else -> null
        }

    private object BlockSettingsPartialChangeProducer :
        PartialChangeProducer<BlockListUiIntent, BlockListPartialChange, BlockListUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<BlockListUiIntent>): Flow<BlockListPartialChange> =
            merge(
                intentFlow.filterIsInstance<BlockListUiIntent.Load>()
                    .flatMapConcat { produceLoadPartialChange() },
                intentFlow.filterIsInstance<BlockListUiIntent.Add>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<BlockListUiIntent.Delete>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<BlockListUiIntent.ImportRules>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<BlockListUiIntent.Clear>()
                    .flatMapConcat { it.producePartialChange() },
            )

        private fun produceLoadPartialChange(): Flow<BlockListPartialChange.Load> =
            flowOf<BlockListPartialChange.Load>(
                BlockListPartialChange.Load.Success(
                    BlockManager.blackList,
                    BlockManager.whiteList
                )
            )
                .onStart { BlockListPartialChange.Load.Start }
                .catch { emit(BlockListPartialChange.Load.Failure(it)) }

        private fun BlockListUiIntent.Add.producePartialChange(): Flow<BlockListPartialChange.Add> =
            flow<BlockListPartialChange.Add> {
                val block = Block(
                    category = category,
                    type = type,
                    keywords = if (type == Block.TYPE_KEYWORD) keywords.toJson() else null,
                    username = username,
                    uid = uid,
                    pattern = pattern,
                    minLevel = minLevel,
                    ipRegion = ipRegion,
                )
                BlockManager.addBlock(block)
                emit(BlockListPartialChange.Add.Success(block))
            }.catch { emit(BlockListPartialChange.Add.Failure(it)) }

        private fun BlockListUiIntent.Delete.producePartialChange(): Flow<BlockListPartialChange.Delete> =
            flow<BlockListPartialChange.Delete> {
                BlockManager.removeBlock(id)
                emit(BlockListPartialChange.Delete.Success(id))
            }.catch { emit(BlockListPartialChange.Delete.Failure(it)) }

        private fun BlockListUiIntent.ImportRules.producePartialChange(): Flow<BlockListPartialChange.Import> =
            flow<BlockListPartialChange.Import> {
                val count = BlockManager.importRulesFromJson(json)
                emit(BlockListPartialChange.Import.Success(count, BlockManager.blackList, BlockManager.whiteList))
            }.catch { emit(BlockListPartialChange.Import.Failure(it)) }

        private fun BlockListUiIntent.Clear.producePartialChange(): Flow<BlockListPartialChange.Clear> =
            flow<BlockListPartialChange.Clear> {
                BlockManager.clearAll(category)
                emit(BlockListPartialChange.Clear.Success(BlockManager.blackList, BlockManager.whiteList))
            }.catch { emit(BlockListPartialChange.Clear.Failure(it)) }
    }
}

sealed interface BlockListUiIntent : UiIntent {
    object Load : BlockListUiIntent

    data class Add(
        val category: Int,
        val type: Int = Block.TYPE_KEYWORD,
        val keywords: List<String> = emptyList(),
        val username: String? = null,
        val uid: String? = null,
        val pattern: String? = null,
        val minLevel: Int = 0,
        val ipRegion: String? = null,
    ) : BlockListUiIntent

    data class Delete(
        val id: Long,
    ) : BlockListUiIntent

    data class ImportRules(
        val json: String,
    ) : BlockListUiIntent

    data class Clear(
        val category: Int? = null,
    ) : BlockListUiIntent
}

sealed interface BlockListPartialChange : PartialChange<BlockListUiState> {
    sealed class Load : BlockListPartialChange {
        override fun reduce(oldState: BlockListUiState): BlockListUiState =
            when (this) {
                Start -> oldState.copy(isLoading = true)
                is Success -> oldState.copy(
                    isLoading = false,
                    blackList = blackList,
                    whiteList = whiteList
                )

                is Failure -> oldState.copy(isLoading = false)
            }

        object Start : Load()

        data class Success(val blackList: List<Block>, val whiteList: List<Block>) : Load()

        data class Failure(val error: Throwable) : Load()
    }

    sealed class Add : BlockListPartialChange {
        override fun reduce(oldState: BlockListUiState): BlockListUiState =
            when (this) {
                is Success -> {
                    val newBlackList =
                        if (item.category == Block.CATEGORY_BLACK_LIST) {
                            oldState.blackList + item
                        } else {
                            oldState.blackList
                        }
                    val newWhiteList =
                        if (item.category == Block.CATEGORY_WHITE_LIST) {
                            oldState.whiteList + item
                        } else {
                            oldState.whiteList
                        }
                    oldState.copy(blackList = newBlackList, whiteList = newWhiteList)
                }

                is Failure -> oldState
            }

        data class Success(val item: Block) : Add()

        data class Failure(val error: Throwable) : Add()
    }

    sealed class Delete : BlockListPartialChange {
        override fun reduce(oldState: BlockListUiState): BlockListUiState =
            when (this) {
                is Success -> {
                    val newBlackList = oldState.blackList.filterNot { it.id == deletedId }
                    val newWhiteList = oldState.whiteList.filterNot { it.id == deletedId }
                    oldState.copy(blackList = newBlackList, whiteList = newWhiteList)
                }

                is Failure -> oldState
            }

        data class Success(val deletedId: Long) : Delete()

        data class Failure(val error: Throwable) : Delete()
    }

    sealed class Import : BlockListPartialChange {
        override fun reduce(oldState: BlockListUiState): BlockListUiState =
            when (this) {
                is Success -> oldState.copy(blackList = blackList, whiteList = whiteList)
                is Failure -> oldState
            }

        data class Success(val count: Int, val blackList: List<Block>, val whiteList: List<Block>) : Import()
        data class Failure(val error: Throwable) : Import()
    }

    sealed class Clear : BlockListPartialChange {
        override fun reduce(oldState: BlockListUiState): BlockListUiState =
            when (this) {
                is Success -> oldState.copy(blackList = blackList, whiteList = whiteList)
                is Failure -> oldState
            }

        data class Success(val blackList: List<Block>, val whiteList: List<Block>) : Clear()
        data class Failure(val error: Throwable) : Clear()
    }
}

data class BlockListUiState(
    val isLoading: Boolean = true,
    val blackList: List<Block> = emptyList(),
    val whiteList: List<Block> = emptyList(),
) : UiState

sealed interface BlockListUiEvent : UiEvent {
    sealed interface Success : BlockListUiEvent {
        object Add : Success
        object Delete : Success
        data class Import(val count: Int) : Success
        object Clear : Success
    }
}
