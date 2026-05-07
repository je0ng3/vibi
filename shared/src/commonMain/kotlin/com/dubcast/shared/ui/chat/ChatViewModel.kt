package com.dubcast.shared.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dubcast.shared.data.remote.dto.ChatMessageDto
import com.dubcast.shared.data.remote.dto.ChatRequestDto
import com.dubcast.shared.data.remote.dto.ChatResponseDto
import com.dubcast.shared.data.remote.dto.ProjectContextDto
import com.dubcast.shared.data.remote.dto.ProposalDto
import com.dubcast.shared.data.remote.dto.ToolCallDto
import com.dubcast.shared.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 채팅 패널 in-memory 세션. v1 은 Room 영구 보관 X — 시트 닫고 다시 열면 messages 초기화.
 *
 * pending proposal 이 있을 때만 [ChatPanel] 이 ProposalCard 를 렌더. 사용자 [적용] 시
 * 호출자(ChatPanel)가 dispatcher 를 직접 호출 — VM 은 timelineVm 을 직접 소유 안 함.
 */
class ChatViewModel(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun send(text: String, projectContext: ProjectContextDto, locale: String = "ko") {
        if (text.isBlank()) return
        val userTurn = ChatMessageDto(role = "user", content = text)
        _state.value = _state.value.copy(
            messages = _state.value.messages + userTurn,
            isSending = true,
            error = null,
        )
        viewModelScope.launch {
            // BFF/Gemini turn 은 user/model 만 의미. UI 전용 "system" (dispatcher 결과 라벨) 은
            // 컨텍스트 노이즈라 BFF 전송 리스트에서 제외. role 정합성은 appendLocalGuide 에서 "model"
            // 로 push 해 user/model alternation 을 깨지 않게 유지.
            val turnsForBff = _state.value.messages.filter { it.role == "user" || it.role == "model" }
            val req = ChatRequestDto(
                messages = turnsForBff,
                projectContext = projectContext,
                locale = locale,
            )
            chatRepository.chat(req).fold(
                onSuccess = { resp -> applyResponse(resp) },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isSending = false,
                        error = e.message ?: "채팅 호출 실패",
                    )
                },
            )
        }
    }

    private fun applyResponse(resp: ChatResponseDto) {
        when (resp.kind) {
            "text" -> {
                val msg = ChatMessageDto(role = "model", content = resp.text.orEmpty())
                _state.value = _state.value.copy(
                    messages = _state.value.messages + msg,
                    isSending = false,
                    pending = null,
                )
            }
            "proposal" -> {
                val proposal = resp.proposal ?: return run {
                    _state.value = _state.value.copy(
                        isSending = false,
                        error = "proposal 비어있음",
                    )
                }
                // proposal rationale 도 messages 에 model turn 으로 보존 → 다음 send 시 컨텍스트.
                val msg = ChatMessageDto(role = "model", content = proposal.rationale)
                _state.value = _state.value.copy(
                    messages = _state.value.messages + msg,
                    isSending = false,
                    pending = proposal,
                )
            }
            else -> {
                _state.value = _state.value.copy(
                    isSending = false,
                    error = "알 수 없는 응답 kind: ${resp.kind}",
                )
            }
        }
    }

    /** [ChatPanel] 이 dispatcher 호출 후 결과 라벨을 system 메시지로 push. */
    fun onApplied(steps: List<ToolCallDto>, summary: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages + ChatMessageDto(role = "system", content = summary),
            pending = null,
        )
    }

    fun cancelProposal() {
        _state.value = _state.value.copy(
            messages = _state.value.messages + ChatMessageDto(role = "system", content = "취소되었습니다."),
            pending = null,
        )
    }

    /**
     * 정해진 답이 있는 가이드성 질문 — Gemini 호출 없이 user/assistant 메시지를 로컬로 append.
     * "어떤 편집을 할 수 있는지" 같은 capability 안내가 대표 케이스.
     */
    fun appendLocalGuide(userPrompt: String, assistantReply: String) {
        // role 은 Gemini 규약 (user/model) 으로 통일 — "assistant" 는 OpenAI 스타일이라 BFF coerce 시
        // user 로 깎여 turn alternation 이 깨졌었다.
        _state.value = _state.value.copy(
            messages = _state.value.messages +
                ChatMessageDto(role = "user", content = userPrompt) +
                ChatMessageDto(role = "model", content = assistantReply),
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

data class ChatUiState(
    val messages: List<ChatMessageDto> = emptyList(),
    val pending: ProposalDto? = null,
    val isSending: Boolean = false,
    val error: String? = null,
)
